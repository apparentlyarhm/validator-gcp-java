package com.arhum.validator.exception;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BaseException extends Exception {

    private String message;

    public BaseException(String message) {
        this.message = message;
    }
}
