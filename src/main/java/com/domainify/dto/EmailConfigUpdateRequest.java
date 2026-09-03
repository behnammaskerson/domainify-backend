package com.domainify.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public class EmailConfigUpdateRequest {

    private boolean enabled;

    @Size(max = 255, message = "{validation.email.host.size}")
    private String host;

    @Min(value = 1, message = "{validation.email.port.range}")
    @Max(value = 65535, message = "{validation.email.port.range}")
    private int port = 587;

    @Size(max = 255, message = "{validation.email.username.size}")
    private String username;

    @Size(max = 512, message = "{validation.email.password.size}")
    private String password;

    @Email(message = "{validation.email.fromEmail.invalid}")
    @Size(max = 255, message = "{validation.email.fromEmail.size}")
    private String fromEmail;

    @Size(max = 255, message = "{validation.email.fromName.size}")
    private String fromName;

    private boolean useTls = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFromEmail() {
        return fromEmail;
    }

    public void setFromEmail(String fromEmail) {
        this.fromEmail = fromEmail;
    }

    public String getFromName() {
        return fromName;
    }

    public void setFromName(String fromName) {
        this.fromName = fromName;
    }

    public boolean isUseTls() {
        return useTls;
    }

    public void setUseTls(boolean useTls) {
        this.useTls = useTls;
    }
}
