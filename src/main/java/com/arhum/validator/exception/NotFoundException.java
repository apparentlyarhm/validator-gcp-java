package com.arhum.validator.exception;

public class NotFoundException extends BaseException {
    public NotFoundException(String message, int code) {
        super(message, code);
    }
}
