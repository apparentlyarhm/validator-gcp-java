package com.arhum.validator.exception;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InternalServerException extends BaseException {

    public InternalServerException(String message, int code) {
        super(message, code);
    }
}
