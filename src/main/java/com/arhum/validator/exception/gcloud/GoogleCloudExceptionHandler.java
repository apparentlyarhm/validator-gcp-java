package com.arhum.validator.exception.gcloud;

import com.google.api.gax.rpc.ApiException;
import com.google.api.gax.rpc.StatusCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GoogleCloudExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GoogleCloudExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, Object>> handleGoogleApiException(ApiException ex) {
        Map<String, Object> errorResponse = new HashMap<>();

        errorResponse.put("status", ex.getStatusCode().getCode().toString());
        errorResponse.put("message", ex.getMessage());

        log.info("GCP ERROR: {}", ex.getCause() != null ? ex.getCause().toString() : "N/A");
        HttpStatus httpStatus = mapGoogleStatusToHttp(ex.getStatusCode());
        return ResponseEntity.status(httpStatus).body(errorResponse);
    }

    private HttpStatus mapGoogleStatusToHttp(StatusCode statusCode) {
        return switch (statusCode.getCode()) {

            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case PERMISSION_DENIED -> HttpStatus.FORBIDDEN;
            case UNAVAILABLE -> HttpStatus.SERVICE_UNAVAILABLE;
            case DEADLINE_EXCEEDED -> HttpStatus.GATEWAY_TIMEOUT;
            case INVALID_ARGUMENT -> HttpStatus.BAD_REQUEST;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}
