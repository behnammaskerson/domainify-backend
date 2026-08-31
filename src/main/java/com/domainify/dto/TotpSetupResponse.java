package com.domainify.dto;

public class TotpSetupResponse {
    private String secret;
    private String otpauthUri;
    private String qrCodeDataUri;

    public TotpSetupResponse() {
    }

    public TotpSetupResponse(String secret, String otpauthUri, String qrCodeDataUri) {
        this.secret = secret;
        this.otpauthUri = otpauthUri;
        this.qrCodeDataUri = qrCodeDataUri;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public String getOtpauthUri() {
        return otpauthUri;
    }

    public void setOtpauthUri(String otpauthUri) {
        this.otpauthUri = otpauthUri;
    }

    public String getQrCodeDataUri() {
        return qrCodeDataUri;
    }

    public void setQrCodeDataUri(String qrCodeDataUri) {
        this.qrCodeDataUri = qrCodeDataUri;
    }
}
