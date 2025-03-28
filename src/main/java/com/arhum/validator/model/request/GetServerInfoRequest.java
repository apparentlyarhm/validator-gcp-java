package com.arhum.validator.model.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GetServerInfoRequest {

    @NotBlank
    private String address;
}
