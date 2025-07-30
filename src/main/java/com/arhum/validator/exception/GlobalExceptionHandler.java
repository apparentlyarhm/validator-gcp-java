package com.arhum.validator.exception;

import com.google.api.gax.rpc.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.io.IOException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(IOException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleIOException(IOException ex) {
        log.error("IOException occurred :: {}", ex.getMessage());
        return new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Internal Server Error");
    }

    @ExceptionHandler(InternalServerException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleInternalError(InternalServerException ex) {
        log.error("Some exception occurred :: {}", ex.getMessage());
        return new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Internal Server Error");
    }

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleGcpNotFound(NotFoundException ex) {
        log.error("GCP not found occurred :: {}", ex.getMessage());
        return new ErrorResponse(HttpStatus.NOT_FOUND.value(), "The queried resource was not found");
    }

    // our not found exception
    @ExceptionHandler(com.arhum.validator.exception.NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFound(com.arhum.validator.exception.NotFoundException ex) {
        log.error("not found occurred :: {}", ex.getMessage());
        return new ErrorResponse(HttpStatus.NOT_FOUND.value(), "The queried resource was not found");
    }

    @ExceptionHandler(AlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse alreadyExists(AlreadyExistsException ex) {
        log.error("resource exists :: {}", ex.getMessage());
        return new ErrorResponse(HttpStatus.CONFLICT.value(), ex.getMessage());
    }

    @ExceptionHandler(BadRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleBadRequest(BadRequestException ex) {
        log.error("bad request occurred :: {}", ex.getMessage());
        return new ErrorResponse(HttpStatus.BAD_REQUEST.value(), ex.getMessage());
    }

    @ExceptionHandler(UnsupportedOperationException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleUnsupportedOperationException(UnsupportedOperationException ex) {
        log.error("UnsupportedOperationException occurred :: {}", ex.getMessage());
        return new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Internal Server Error");
    }

}
