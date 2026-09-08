package com.domainify.exception;

/**
 * Payment gateway call failed with optional provider code/message for audit storage.
 */
public class PaymentGatewayException extends ApiException {

    private final Integer gatewayCode;
    private final String gatewayMessage;

    public PaymentGatewayException(ErrorCode code, Integer gatewayCode, String gatewayMessage) {
        super(code);
        this.gatewayCode = gatewayCode;
        this.gatewayMessage = gatewayMessage;
    }

    public Integer getGatewayCode() {
        return gatewayCode;
    }

    public String getGatewayMessage() {
        return gatewayMessage;
    }
}
