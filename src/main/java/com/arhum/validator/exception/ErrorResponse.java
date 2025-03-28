package com.arhum.validator.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private Integer code;
    private String message;
    private Map<String, String> errors;

    public ErrorResponse(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
