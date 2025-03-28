package com.arhum.validator.exception;

public class BadRequestException extends BaseException{

    public BadRequestException(String message, int code) {
        super(message, code);
    }
}
