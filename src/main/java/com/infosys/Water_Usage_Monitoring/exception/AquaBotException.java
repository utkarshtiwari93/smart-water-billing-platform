package com.infosys.Water_Usage_Monitoring.exception;

public class AquaBotException extends RuntimeException {

    private final AquaBotErrorCode errorCode;

    public AquaBotException(AquaBotErrorCode errorCode) {
        super(errorCode.safeMessage());
        this.errorCode = errorCode;
    }

    public AquaBotException(AquaBotErrorCode errorCode, Throwable cause) {
        super(errorCode.safeMessage(), cause);
        this.errorCode = errorCode;
    }

    public AquaBotErrorCode getErrorCode() {
        return errorCode;
    }
}
