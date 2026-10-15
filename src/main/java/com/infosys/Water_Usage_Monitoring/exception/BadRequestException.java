package com.infosys.Water_Usage_Monitoring.exception;

public class BadRequestException extends RuntimeException {
    public BadRequestException(String msg) { super(msg); }
}