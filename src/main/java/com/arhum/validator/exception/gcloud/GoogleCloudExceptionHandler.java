package com.arhum.validator.exception.gcloud;

import com.arhum.validator.exception.ErrorResponse;
import com.google.api.gax.rpc.ApiException;
import com.google.api.gax.rpc.StatusCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GoogleCloudExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GoogleCloudExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<Map<String, Object>> handleGoogleApiException(ApiException ex) {
        HttpStatus httpStatus = mapGoogleStatusToHttp(ex.getStatusCode());
        log.info("GCP ERROR: {} and status {}", ex.getCause() != null ? ex.getCause().toString() : "N/A", httpStatus); // have to keep this to know whats going on

        Map<String, Object> response = Map.of(
                "status", mapGoogleStatusToHttp(ex.getStatusCode()).value(),
                "message", "Internal Server Error: GCP related issue."
        );

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response); // have to be very generic to the client
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
