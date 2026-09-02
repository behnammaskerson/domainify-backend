package com.domainify.service;

import com.domainify.dto.SmsArchiveSendResultDto;
import com.domainify.dto.SmsBulkSendDataDto;
import com.domainify.dto.SmsBulkSendProviderBody;
import com.domainify.dto.SmsBulkSendRequest;
import com.domainify.dto.SmsBulkSendResultDto;
import com.domainify.dto.SmsCreditResultDto;
import com.domainify.dto.SmsDailyPackItemDto;
import com.domainify.dto.SmsDailyPackResultDto;
import com.domainify.dto.SmsDeliveryStatusDataDto;
import com.domainify.dto.SmsIrArchiveSendEnvelope;
import com.domainify.dto.SmsIrBulkSendEnvelope;
import com.domainify.dto.SmsIrDailyPackEnvelope;
import com.domainify.dto.SmsIrEnvelope;
import com.domainify.dto.SmsIrLinesEnvelope;
import com.domainify.dto.SmsIrLiveSendEnvelope;
import com.domainify.dto.SmsIrPackReportEnvelope;
import com.domainify.dto.SmsIrReceiveEnvelope;
import com.domainify.dto.SmsLinesResultDto;
import com.domainify.dto.SmsLiveSendResultDto;
import com.domainify.dto.SmsPackReportResultDto;
import com.domainify.dto.SmsReceiveLatestResultDto;
import com.domainify.dto.SmsReceivePagedResultDto;
import com.domainify.dto.SmsReceivedMessageDto;
import com.domainify.exception.ApiException;
import com.domainify.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SmsService {

    private static final int PROVIDER_SUCCESS = 1;
    private static final int MAX_BULK_MOBILES = 100;

    private final SmsConfigService smsConfigService;
    private final ScheduledSmsService scheduledSmsService;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public SmsService(
            SmsConfigService smsConfigService,
            ScheduledSmsService scheduledSmsService,
            ObjectMapper objectMapper) {
        this.smsConfigService = smsConfigService;
        this.scheduledSmsService = scheduledSmsService;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.create();
    }

    public SmsCreditResultDto fetchCredit() {
        String apiKey = requireApiKey();
        String creditUrl = smsConfigService.getServerUrl() + "v1/credit";

        try {
            return restClient.get()
                    .uri(creditUrl)
                    .header("X-API-KEY", apiKey)
                    .exchange((request, response) -> mapCreditResponse(response));
        } catch (Exception ex) {
            return SmsCreditResultDto.failure(null, null);
        }
    }

    public SmsLinesResultDto fetchLines() {
        String apiKey = requireApiKey();
        String linesUrl = smsConfigService.getServerUrl() + "v1/line";

        try {
            return restClient.get()
                    .uri(linesUrl)
                    .header("X-API-KEY", apiKey)
                    .exchange((request, response) -> mapLinesResponse(response));
        } catch (Exception ex) {
            return SmsLinesResultDto.failure(null, null);
        }
    }

    public SmsBulkSendResultDto sendBulk(SmsBulkSendRequest request) {
        String apiKey = requireApiKey();
        List<String> mobiles = normalizeMobiles(request.getMobiles());
        if (mobiles.isEmpty()) {
            throw new ApiException(ErrorCode.SMS_MOBILES_REQUIRED);
        }
        if (mobiles.size() > MAX_BULK_MOBILES) {
            throw new ApiException(ErrorCode.SMS_MOBILES_LIMIT);
        }

        String messageText = request.getMessageText() != null ? request.getMessageText().trim() : "";
        if (!StringUtils.hasText(messageText)) {
            throw new ApiException(ErrorCode.SMS_MESSAGE_TEXT_REQUIRED);
        }

        String lineNumber = resolveLineNumber(request.getLineNumber());
        validateSendDateTime(request.getSendDateTime());

        SmsBulkSendProviderBody body = new SmsBulkSendProviderBody();
        body.setLineNumber(Long.parseLong(lineNumber));
        body.setMessageText(messageText);
        body.setMobiles(mobiles);
        body.setSendDateTime(request.getSendDateTime());

        String url = smsConfigService.getServerUrl() + "v1/send/bulk";

        try {
            SmsBulkSendResultDto result = restClient.post()
                    .uri(url)
                    .header("X-API-KEY", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .exchange((req, response) -> mapBulkSendResponse(response));
            if (result.isSuccess() && request.getSendDateTime() != null && result.getData() != null) {
                scheduledSmsService.recordScheduledSend(request, result.getData(), request.getSendDateTime());
            }
            return result;
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            return SmsBulkSendResultDto.failure(null, null);
        }
    }

    public SmsLiveSendResultDto fetchLiveSends(Integer pageSize, Integer pageNumber) {
        int size = pageSize == null || pageSize <= 0 ? 100 : Math.min(pageSize, 100);
        int page = pageNumber == null || pageNumber <= 0 ? 1 : pageNumber;

        String apiKey = requireApiKey();
        String baseUrl = smsConfigService.getServerUrl() + "v1/send/live";

        try {
            return restClient.get()
                    .uri(baseUrl + "?PageSize={pageSize}&PageNumber={pageNumber}", size, page)
                    .header("X-API-KEY", apiKey)
                    .exchange((request, response) -> mapLiveSendResponse(response, size, page));
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            return SmsLiveSendResultDto.failure(null, null);
        }
    }

    public SmsArchiveSendResultDto fetchArchiveSends(
            Long fromDate,
            Long toDate,
            Integer pageSize,
            Integer pageNumber) {
        int size = pageSize == null || pageSize <= 0 ? 100 : Math.min(pageSize, 100);
        int page = pageNumber == null || pageNumber <= 0 ? 1 : pageNumber;

        String apiKey = requireApiKey();
        String baseUrl = smsConfigService.getServerUrl() + "v1/send/archive";
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(baseUrl)
                .queryParam("PageSize", size)
                .queryParam("PageNumber", page);
        if (fromDate != null && fromDate > 0) {
            uriBuilder.queryParam("FromDate", fromDate);
        }
        if (toDate != null && toDate > 0) {
            uriBuilder.queryParam("ToDate", toDate);
        }

        try {
            return restClient.get()
                    .uri(uriBuilder.toUriString())
                    .header("X-API-KEY", apiKey)
                    .exchange((request, response) -> mapArchiveSendResponse(response, size, page));
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            return SmsArchiveSendResultDto.failure(null, null);
        }
    }

    public SmsDailyPackResultDto fetchDailyPacks(Integer pageSize, Integer pageNumber) {
        int size = pageSize == null || pageSize <= 0 ? 100 : Math.min(pageSize, 100);
        int page = pageNumber == null || pageNumber <= 0 ? 1 : pageNumber;

        String apiKey = requireApiKey();
        String baseUrl = smsConfigService.getServerUrl() + "v1/send/pack";

        try {
            return restClient.get()
                    .uri(baseUrl + "?PageSize={pageSize}&PageNumber={pageNumber}", size, page)
                    .header("X-API-KEY", apiKey)
                    .exchange((request, response) -> mapDailyPackResponse(response, size, page));
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            return SmsDailyPackResultDto.failure(null, null);
        }
    }

    public SmsPackReportResultDto fetchPackReport(String packId) {
        if (!StringUtils.hasText(packId)) {
            throw new ApiException(ErrorCode.SMS_PACK_ID_REQUIRED);
        }

        String trimmedPackId = packId.trim();
        String apiKey = requireApiKey();
        String url = smsConfigService.getServerUrl() + "v1/send/pack/" + trimmedPackId;

        try {
            return restClient.get()
                    .uri(url)
                    .header("X-API-KEY", apiKey)
                    .exchange((request, response) -> mapPackReportResponse(response, trimmedPackId));
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            return SmsPackReportResultDto.failure(null, null);
        }
    }

    public SmsReceiveLatestResultDto fetchLatestReceived(Integer count) {
        int requestedCount = count == null || count <= 0 ? 100 : Math.min(count, 100);
        String apiKey = requireApiKey();
        String baseUrl = smsConfigService.getServerUrl() + "v1/receive/latest";

        try {
            return restClient.get()
                    .uri(baseUrl + "?Count={count}", requestedCount)
                    .header("X-API-KEY", apiKey)
                    .exchange((request, response) -> mapReceiveLatestResponse(response, requestedCount));
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            return SmsReceiveLatestResultDto.failure(null, null);
        }
    }

    public SmsReceivePagedResultDto fetchLiveReceived(
            Integer pageSize,
            Integer pageNumber,
            Boolean sortByNewest,
            String mobile) {
        int size = pageSize == null || pageSize <= 0 ? 100 : Math.min(pageSize, 100);
        int page = pageNumber == null || pageNumber <= 0 ? 1 : pageNumber;

        String apiKey = requireApiKey();
        String baseUrl = smsConfigService.getServerUrl() + "v1/receive/live";
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(baseUrl)
                .queryParam("PageSize", size)
                .queryParam("PageNumber", page);
        if (sortByNewest != null) {
            uriBuilder.queryParam("sortByNewest", sortByNewest);
        }
        if (StringUtils.hasText(mobile)) {
            uriBuilder.queryParam("mobile", mobile.trim());
        }

        try {
            return restClient.get()
                    .uri(uriBuilder.toUriString())
                    .header("X-API-KEY", apiKey)
                    .exchange((request, response) -> mapReceivePagedResponse(response, size, page));
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            return SmsReceivePagedResultDto.failure(null, null);
        }
    }

    public SmsReceivePagedResultDto fetchArchiveReceived(
            Long fromDate,
            Long toDate,
            Integer pageSize,
            Integer pageNumber,
            String mobile) {
        int size = pageSize == null || pageSize <= 0 ? 100 : Math.min(pageSize, 100);
        int page = pageNumber == null || pageNumber <= 0 ? 1 : pageNumber;

        String apiKey = requireApiKey();
        String baseUrl = smsConfigService.getServerUrl() + "v1/receive/archive";
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(baseUrl)
                .queryParam("PageSize", size)
                .queryParam("PageNumber", page);
        if (fromDate != null && fromDate > 0) {
            uriBuilder.queryParam("FromDate", fromDate);
        }
        if (toDate != null && toDate > 0) {
            uriBuilder.queryParam("ToDate", toDate);
        }
        if (StringUtils.hasText(mobile)) {
            uriBuilder.queryParam("mobile", mobile.trim());
        }

        try {
            return restClient.get()
                    .uri(uriBuilder.toUriString())
                    .header("X-API-KEY", apiKey)
                    .exchange((request, response) -> mapReceivePagedResponse(response, size, page));
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            return SmsReceivePagedResultDto.failure(null, null);
        }
    }

    private String requireApiKey() {
        String apiKey = smsConfigService.getApiKey();
        if (!StringUtils.hasText(apiKey)) {
            throw new ApiException(ErrorCode.SMS_API_KEY_REQUIRED);
        }
        return apiKey;
    }

    private SmsCreditResultDto mapCreditResponse(ClientHttpResponse response) throws IOException {
        int httpStatus = response.getStatusCode().value();
        String rawBody = readBody(response);
        SmsIrEnvelope envelope = parseEnvelope(rawBody, SmsIrEnvelope.class);

        if (httpStatus == 200 && envelope != null && envelope.getStatus() != null
                && envelope.getStatus() == PROVIDER_SUCCESS) {
            BigDecimal credit = envelope.getData() != null ? envelope.getData() : BigDecimal.ZERO;
            return SmsCreditResultDto.success(credit);
        }

        Integer providerStatus = envelope != null ? envelope.getStatus() : null;
        return SmsCreditResultDto.failure(httpStatus, providerStatus);
    }

    private SmsLinesResultDto mapLinesResponse(ClientHttpResponse response) throws IOException {
        int httpStatus = response.getStatusCode().value();
        String rawBody = readBody(response);
        SmsIrLinesEnvelope envelope = parseEnvelope(rawBody, SmsIrLinesEnvelope.class);

        if (httpStatus == 200 && envelope != null && envelope.getStatus() != null
                && envelope.getStatus() == PROVIDER_SUCCESS) {
            List<String> lines = envelope.getData().stream()
                    .map(number -> String.valueOf(number.longValue()))
                    .collect(Collectors.toList());
            return SmsLinesResultDto.success(lines);
        }

        Integer providerStatus = envelope != null ? envelope.getStatus() : null;
        return SmsLinesResultDto.failure(httpStatus, providerStatus);
    }

    private SmsBulkSendResultDto mapBulkSendResponse(ClientHttpResponse response) throws IOException {
        int httpStatus = response.getStatusCode().value();
        String rawBody = readBody(response);
        SmsIrBulkSendEnvelope envelope = parseEnvelope(rawBody, SmsIrBulkSendEnvelope.class);

        if (httpStatus == 200 && envelope != null && envelope.getStatus() != null
                && envelope.getStatus() == PROVIDER_SUCCESS) {
            SmsBulkSendDataDto data = envelope.getData() != null ? envelope.getData() : new SmsBulkSendDataDto();
            return SmsBulkSendResultDto.success(data);
        }

        Integer providerStatus = envelope != null ? envelope.getStatus() : null;
        return SmsBulkSendResultDto.failure(httpStatus, providerStatus);
    }

    private SmsLiveSendResultDto mapLiveSendResponse(
            ClientHttpResponse response,
            int pageSize,
            int pageNumber) throws IOException {
        int httpStatus = response.getStatusCode().value();
        String rawBody = readBody(response);
        SmsIrLiveSendEnvelope envelope = parseEnvelope(rawBody, SmsIrLiveSendEnvelope.class);

        if (httpStatus == 200 && envelope != null && envelope.getStatus() != null
                && envelope.getStatus() == PROVIDER_SUCCESS) {
            List<SmsDeliveryStatusDataDto> data = envelope.getData() != null
                    ? envelope.getData()
                    : List.of();
            boolean hasMore = data.size() >= pageSize;
            return SmsLiveSendResultDto.success(data, pageNumber, pageSize, hasMore);
        }

        Integer providerStatus = envelope != null ? envelope.getStatus() : null;
        return SmsLiveSendResultDto.failure(httpStatus, providerStatus);
    }

    private SmsArchiveSendResultDto mapArchiveSendResponse(
            ClientHttpResponse response,
            int pageSize,
            int pageNumber) throws IOException {
        int httpStatus = response.getStatusCode().value();
        String rawBody = readBody(response);
        SmsIrArchiveSendEnvelope envelope = parseEnvelope(rawBody, SmsIrArchiveSendEnvelope.class);

        if (httpStatus == 200 && envelope != null && envelope.getStatus() != null
                && envelope.getStatus() == PROVIDER_SUCCESS) {
            List<SmsDeliveryStatusDataDto> data = envelope.getData() != null
                    ? envelope.getData()
                    : List.of();
            boolean hasMore = data.size() >= pageSize;
            return SmsArchiveSendResultDto.success(data, pageNumber, pageSize, hasMore);
        }

        Integer providerStatus = envelope != null ? envelope.getStatus() : null;
        return SmsArchiveSendResultDto.failure(httpStatus, providerStatus);
    }

    private SmsDailyPackResultDto mapDailyPackResponse(
            ClientHttpResponse response,
            int pageSize,
            int pageNumber) throws IOException {
        int httpStatus = response.getStatusCode().value();
        String rawBody = readBody(response);
        SmsIrDailyPackEnvelope envelope = parseEnvelope(rawBody, SmsIrDailyPackEnvelope.class);

        if (httpStatus == 200 && envelope != null && envelope.getStatus() != null
                && envelope.getStatus() == PROVIDER_SUCCESS) {
            List<SmsDailyPackItemDto> data = envelope.getData() != null
                    ? envelope.getData()
                    : List.of();
            boolean hasMore = data.size() >= pageSize;
            return SmsDailyPackResultDto.success(data, pageNumber, pageSize, hasMore);
        }

        Integer providerStatus = envelope != null ? envelope.getStatus() : null;
        return SmsDailyPackResultDto.failure(httpStatus, providerStatus);
    }

    private SmsPackReportResultDto mapPackReportResponse(
            ClientHttpResponse response,
            String packId) throws IOException {
        int httpStatus = response.getStatusCode().value();
        String rawBody = readBody(response);
        SmsIrPackReportEnvelope envelope = parseEnvelope(rawBody, SmsIrPackReportEnvelope.class);

        if (httpStatus == 200 && envelope != null && envelope.getStatus() != null
                && envelope.getStatus() == PROVIDER_SUCCESS) {
            List<SmsDeliveryStatusDataDto> data = envelope.getData() != null
                    ? envelope.getData()
                    : List.of();
            return SmsPackReportResultDto.success(packId, data);
        }

        Integer providerStatus = envelope != null ? envelope.getStatus() : null;
        return SmsPackReportResultDto.failure(httpStatus, providerStatus);
    }

    private SmsReceiveLatestResultDto mapReceiveLatestResponse(
            ClientHttpResponse response,
            int count) throws IOException {
        int httpStatus = response.getStatusCode().value();
        String rawBody = readBody(response);
        SmsIrReceiveEnvelope envelope = parseEnvelope(rawBody, SmsIrReceiveEnvelope.class);

        if (httpStatus == 200 && envelope != null && envelope.getStatus() != null
                && envelope.getStatus() == PROVIDER_SUCCESS) {
            List<SmsReceivedMessageDto> data = envelope.getData() != null
                    ? envelope.getData()
                    : List.of();
            return SmsReceiveLatestResultDto.success(data, count);
        }

        Integer providerStatus = envelope != null ? envelope.getStatus() : null;
        return SmsReceiveLatestResultDto.failure(httpStatus, providerStatus);
    }

    private SmsReceivePagedResultDto mapReceivePagedResponse(
            ClientHttpResponse response,
            int pageSize,
            int pageNumber) throws IOException {
        int httpStatus = response.getStatusCode().value();
        String rawBody = readBody(response);
        SmsIrReceiveEnvelope envelope = parseEnvelope(rawBody, SmsIrReceiveEnvelope.class);

        if (httpStatus == 200 && envelope != null && envelope.getStatus() != null
                && envelope.getStatus() == PROVIDER_SUCCESS) {
            List<SmsReceivedMessageDto> data = envelope.getData() != null
                    ? envelope.getData()
                    : List.of();
            boolean hasMore = data.size() >= pageSize;
            return SmsReceivePagedResultDto.success(data, pageNumber, pageSize, hasMore);
        }

        Integer providerStatus = envelope != null ? envelope.getStatus() : null;
        return SmsReceivePagedResultDto.failure(httpStatus, providerStatus);
    }

    private List<String> normalizeMobiles(List<String> rawMobiles) {
        if (rawMobiles == null) {
            return List.of();
        }
        List<String> normalized = new ArrayList<>();
        for (String mobile : rawMobiles) {
            if (!StringUtils.hasText(mobile)) {
                continue;
            }
            String trimmed = mobile.trim();
            if (!trimmed.isEmpty()) {
                normalized.add(trimmed);
            }
        }
        return normalized;
    }

    private String resolveLineNumber(String requestedLine) {
        if (StringUtils.hasText(requestedLine)) {
            return requestedLine.trim();
        }
        String defaultLine = smsConfigService.getDefaultLine();
        if (!StringUtils.hasText(defaultLine)) {
            throw new ApiException(ErrorCode.SMS_DEFAULT_LINE_INVALID);
        }
        return defaultLine.trim();
    }

    private void validateSendDateTime(Long sendDateTime) {
        if (sendDateTime == null) {
            return;
        }
        Instant scheduled = Instant.ofEpochSecond(sendDateTime);
        Instant now = Instant.now();
        Instant min = now.plus(1, ChronoUnit.HOURS);
        Instant max = now.plus(365, ChronoUnit.DAYS);
        if (scheduled.isBefore(min) || scheduled.isAfter(max)) {
            throw new ApiException(ErrorCode.SMS_SEND_DATETIME_INVALID);
        }
    }

    private String readBody(ClientHttpResponse response) throws IOException {
        return new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
    }

    private <T> T parseEnvelope(String rawBody, Class<T> type) {
        if (!StringUtils.hasText(rawBody)) {
            return null;
        }
        try {
            return objectMapper.readValue(rawBody, type);
        } catch (Exception ex) {
            return null;
        }
    }
}
