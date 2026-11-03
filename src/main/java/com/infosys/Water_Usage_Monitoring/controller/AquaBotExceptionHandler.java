package com.infosys.Water_Usage_Monitoring.controller;

import com.infosys.Water_Usage_Monitoring.dto.AquaBotErrorResponse;
import com.infosys.Water_Usage_Monitoring.exception.AquaBotException;
import com.infosys.Water_Usage_Monitoring.exception.AquaBotErrorCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = AquaBotController.class)
public class AquaBotExceptionHandler {

    @ExceptionHandler(AquaBotException.class)
    public ResponseEntity<AquaBotErrorResponse> handleAquaBotException(AquaBotException exception) {
        AquaBotErrorCode code = exception.getErrorCode();
        return ResponseEntity.status(code.httpStatus())
                .body(new AquaBotErrorResponse(code.httpStatus().value(), code.name(), code.safeMessage()));
    }
}
