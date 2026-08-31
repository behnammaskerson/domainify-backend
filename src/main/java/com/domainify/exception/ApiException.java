package com.domainify.exception;

public class ApiException extends RuntimeException {
    private final ErrorCode code;
    private final Object[] args;

    public ApiException(ErrorCode code) {
        super(code.name());
        this.code = code;
        this.args = null;
    }

    public ApiException(ErrorCode code, Object... args) {
        super(code.name());
        this.code = code;
        this.args = args;
    }

    public ErrorCode getCode() {
        return code;
    }

    public Object[] getArgs() {
        return args;
    }
}
