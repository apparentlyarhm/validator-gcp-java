package com.arhum.validator.exception;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BaseException extends Exception {

    private String message;
    private int code;

    public BaseException(String message, int code) {
        this.message = message;
        this.code = code;
    }
}
