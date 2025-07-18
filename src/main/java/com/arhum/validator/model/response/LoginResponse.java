package com.arhum.validator.model.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginResponse {

    private String id;
    private String email;
    private String token;
}
