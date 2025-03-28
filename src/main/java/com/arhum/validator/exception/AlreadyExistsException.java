package com.arhum.validator.exception;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AlreadyExistsException extends BaseException {

    public AlreadyExistsException(String message, int code) {
        super(message, code);
    }
}
