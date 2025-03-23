package com.arhum.validator.model.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.ALWAYS)
public class CommonResponse {

    private String message;

    public CommonResponse(String message) {
        this.message = message;
    }

    public CommonResponse() {
    }
}
