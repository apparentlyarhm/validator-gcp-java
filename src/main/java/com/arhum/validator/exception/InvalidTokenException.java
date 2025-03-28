package com.arhum.validator.exception;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InvalidTokenException extends BaseException {

    public InvalidTokenException(String message, int code) {
        super(message, code);
    }
}
