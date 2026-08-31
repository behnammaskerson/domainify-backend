package com.domainify.dto;

import java.util.List;

public class TotpEnableResponse {
    private UserDto user;
    private List<String> backupCodes;

    public TotpEnableResponse() {
    }

    public TotpEnableResponse(UserDto user, List<String> backupCodes) {
        this.user = user;
        this.backupCodes = backupCodes;
    }

    public UserDto getUser() {
        return user;
    }

    public void setUser(UserDto user) {
        this.user = user;
    }

    public List<String> getBackupCodes() {
        return backupCodes;
    }

    public void setBackupCodes(List<String> backupCodes) {
        this.backupCodes = backupCodes;
    }
}
