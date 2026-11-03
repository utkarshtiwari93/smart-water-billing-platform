package com.infosys.Water_Usage_Monitoring.exception;

import org.springframework.http.HttpStatus;

public enum AquaBotErrorCode {
    PROVIDER_NOT_CONFIGURED(HttpStatus.SERVICE_UNAVAILABLE,
            "AquaBot has no AI provider configured on the backend."),
    PROVIDER_CONFIGURATION_MISSING(HttpStatus.SERVICE_UNAVAILABLE,
            "AquaBot provider configuration is incomplete. Contact the administrator."),
    PROVIDER_AUTHENTICATION_FAILED(HttpStatus.SERVICE_UNAVAILABLE,
            "AquaBot could not authenticate with its AI provider. Contact the administrator."),
    PROVIDER_MODEL_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE,
            "The configured AquaBot model is unavailable. Contact the administrator."),
    PROVIDER_RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS,
            "AquaBot is receiving too many requests. Please try again shortly."),
    PROVIDER_UPSTREAM_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE,
            "The AquaBot AI provider is temporarily unavailable. Please try again later."),
    PROVIDER_CONNECTION_FAILED(HttpStatus.SERVICE_UNAVAILABLE,
            "AquaBot could not connect to its AI provider. Please try again later."),
    PROVIDER_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT,
            "AquaBot took too long to respond. Please try again."),
    PROVIDER_INVALID_RESPONSE(HttpStatus.BAD_GATEWAY,
            "AquaBot received an invalid response from its AI provider. Please try again."),
    PROVIDER_REQUEST_REJECTED(HttpStatus.BAD_GATEWAY,
            "The AI provider could not process this request. Please try again."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR,
            "AquaBot encountered an internal error. Please try again later.");

    private final HttpStatus httpStatus;
    private final String safeMessage;

    AquaBotErrorCode(HttpStatus httpStatus, String safeMessage) {
        this.httpStatus = httpStatus;
        this.safeMessage = safeMessage;
    }

    public HttpStatus httpStatus() {
        return httpStatus;
    }

    public String safeMessage() {
        return safeMessage;
    }
}
