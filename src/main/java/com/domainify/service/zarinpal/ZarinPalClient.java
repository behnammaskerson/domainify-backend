package com.domainify.service.zarinpal;

import com.domainify.exception.ApiException;
import com.domainify.exception.ErrorCode;
import com.domainify.exception.PaymentGatewayException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.http.HttpClient;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;

@Component
public class ZarinPalClient {

    private static final Logger log = LoggerFactory.getLogger(ZarinPalClient.class);

    public static final String PROD_HOST = "https://payment.zarinpal.com";
    public static final String SANDBOX_HOST = "https://sandbox.zarinpal.com";

    private final RestClient secureClient;
    private final RestClient insecureClient;
    private final ObjectMapper objectMapper;
    private final boolean allowInsecureSsl;

    public ZarinPalClient(
            ObjectMapper objectMapper,
            @Value("${app.payment.ssl.insecure:false}") boolean allowInsecureSsl) {
        this.objectMapper = objectMapper;
        this.allowInsecureSsl = allowInsecureSsl;
        this.secureClient = RestClient.builder()
                .requestFactory(new JdkClientHttpRequestFactory(HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(15))
                        .build()))
                .build();
        this.insecureClient = allowInsecureSsl ? buildInsecureClient() : this.secureClient;
        if (allowInsecureSsl) {
            log.warn("Payment gateway insecure SSL is enabled (sandbox only). Do not use with production.");
        }
    }

    public String baseHost(boolean sandbox) {
        return sandbox ? SANDBOX_HOST : PROD_HOST;
    }

    public String startPayUrl(boolean sandbox, String authority) {
        return baseHost(sandbox) + "/pg/StartPay/" + authority;
    }

    public RequestResult requestPayment(
            boolean sandbox,
            String merchantId,
            long amountIrt,
            String description,
            String callbackUrl,
            String email,
            String mobile,
            String orderId) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("merchant_id", merchantId);
        body.put("amount", amountIrt);
        body.put("currency", "IRT");
        body.put("description", description);
        body.put("callback_url", callbackUrl);

        ObjectNode metadata = objectMapper.createObjectNode();
        if (StringUtils.hasText(email)) {
            metadata.put("email", email.trim());
        }
        if (StringUtils.hasText(mobile)) {
            metadata.put("mobile", mobile.trim());
        }
        if (StringUtils.hasText(orderId)) {
            metadata.put("order_id", orderId.trim());
        }
        if (!metadata.isEmpty()) {
            body.set("metadata", metadata);
        }

        String url = baseHost(sandbox) + "/pg/v4/payment/request.json";
        JsonNode response = postJson(url, body, sandbox);
        JsonNode data = response.path("data");
        int code = extractCode(response, data);
        if (code != 100) {
            String message = firstErrorMessage(response, data);
            log.warn("Payment gateway request failed code={} message={} sandbox={}", code, message, sandbox);
            throw new PaymentGatewayException(ErrorCode.PAYMENT_GATEWAY_ERROR, code, message);
        }
        String authority = data.path("authority").asText(null);
        if (!StringUtils.hasText(authority)) {
            log.warn("Payment gateway request succeeded without authority");
            throw new PaymentGatewayException(
                    ErrorCode.PAYMENT_GATEWAY_ERROR, code, "Missing authority in gateway response");
        }
        return new RequestResult(authority, code, data.path("fee").asLong(0L));
    }

    public VerifyResult verifyPayment(
            boolean sandbox,
            String merchantId,
            long amountIrt,
            String authority) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("merchant_id", merchantId);
        body.put("amount", amountIrt);
        body.put("authority", authority);

        String url = baseHost(sandbox) + "/pg/v4/payment/verify.json";
        JsonNode response = postJson(url, body, sandbox);
        JsonNode data = response.path("data");
        int code = extractCode(response, data);
        if (code != 100 && code != 101) {
            String message = firstErrorMessage(response, data);
            log.warn("Payment gateway verify failed code={} message={} authority={}", code, message, authority);
            throw new PaymentGatewayException(ErrorCode.PAYMENT_VERIFY_FAILED, code, message);
        }
        Long refId = data.hasNonNull("ref_id") ? data.path("ref_id").asLong() : null;
        Long fee = data.has("fee") && !data.path("fee").isNull() ? data.path("fee").asLong() : null;
        String cardPan = data.path("card_pan").asText(null);
        return new VerifyResult(code, refId, fee, cardPan, code == 101);
    }

    private JsonNode postJson(String url, ObjectNode body, boolean sandbox) {
        RestClient client = clientFor(sandbox);
        try {
            String payload = objectMapper.writeValueAsString(body);
            String raw = client.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .exchange((request, response) -> {
                        String responseBody = response.bodyTo(String.class);
                        if (!response.getStatusCode().is2xxSuccessful()) {
                            log.warn(
                                    "Payment gateway HTTP {} body={}",
                                    response.getStatusCode().value(),
                                    truncate(responseBody));
                            if (!StringUtils.hasText(responseBody)) {
                                throw new PaymentGatewayException(
                                        ErrorCode.PAYMENT_GATEWAY_ERROR,
                                        response.getStatusCode().value(),
                                        "HTTP " + response.getStatusCode().value());
                            }
                        }
                        return responseBody;
                    });
            if (!StringUtils.hasText(raw)) {
                throw new PaymentGatewayException(
                        ErrorCode.PAYMENT_GATEWAY_ERROR, null, "Empty gateway response");
            }
            return objectMapper.readTree(raw);
        } catch (PaymentGatewayException ex) {
            throw ex;
        } catch (ApiException ex) {
            throw ex;
        } catch (RestClientResponseException ex) {
            log.warn(
                    "Payment gateway HTTP error status={} body={}",
                    ex.getStatusCode().value(),
                    truncate(ex.getResponseBodyAsString()));
            throw new PaymentGatewayException(
                    ErrorCode.PAYMENT_GATEWAY_ERROR,
                    ex.getStatusCode().value(),
                    truncate(ex.getResponseBodyAsString()));
        } catch (Exception ex) {
            log.warn("Payment gateway call failed: {}", ex.toString());
            throw new PaymentGatewayException(
                    ErrorCode.PAYMENT_GATEWAY_ERROR, null, truncate(ex.getMessage()));
        }
    }

    private RestClient clientFor(boolean sandbox) {
        if (sandbox && allowInsecureSsl) {
            return insecureClient;
        }
        return secureClient;
    }

    private static RestClient buildInsecureClient() {
        try {
            TrustManager[] trustAll = new TrustManager[]{
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(X509Certificate[] chain, String authType) {
                        }

                        @Override
                        public void checkServerTrusted(X509Certificate[] chain, String authType) {
                        }

                        @Override
                        public X509Certificate[] getAcceptedIssuers() {
                            return new X509Certificate[0];
                        }
                    }
            };
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAll, new SecureRandom());
            HttpClient httpClient = HttpClient.newBuilder()
                    .sslContext(sslContext)
                    .connectTimeout(Duration.ofSeconds(15))
                    .build();
            return RestClient.builder()
                    .requestFactory(new JdkClientHttpRequestFactory(httpClient))
                    .build();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to build insecure payment RestClient", ex);
        }
    }

    private int extractCode(JsonNode response, JsonNode data) {
        if (data != null && data.has("code") && !data.path("code").isNull()) {
            return data.path("code").asInt(0);
        }
        JsonNode errors = response.path("errors");
        if (errors.isObject() && errors.has("code")) {
            return errors.path("code").asInt(0);
        }
        if (errors.isArray() && !errors.isEmpty() && errors.get(0).has("code")) {
            return errors.get(0).path("code").asInt(0);
        }
        return 0;
    }

    private String firstErrorMessage(JsonNode response, JsonNode data) {
        JsonNode errors = response.path("errors");
        if (errors.isObject()) {
            if (errors.has("message")) {
                return errors.path("message").asText();
            }
            if (errors.has("code")) {
                return "code " + errors.path("code").asText();
            }
        }
        if (errors.isArray() && !errors.isEmpty()) {
            JsonNode first = errors.get(0);
            if (first.isTextual()) {
                return first.asText();
            }
            if (first.has("message")) {
                return first.path("message").asText();
            }
        }
        if (data != null && data.has("message")) {
            return data.path("message").asText();
        }
        return "unknown";
    }

    private String truncate(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.length() <= 500 ? value : value.substring(0, 500) + "...";
    }

    public record RequestResult(String authority, int code, long fee) {
    }

    public record VerifyResult(int code, Long refId, Long fee, String cardPan, boolean alreadyVerifiedAtGateway) {
    }
}
