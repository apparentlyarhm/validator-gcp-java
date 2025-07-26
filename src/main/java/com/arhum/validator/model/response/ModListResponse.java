package com.arhum.validator.model.response;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ModListResponse {

    private String updatedAt;
    private List<String> mods;
}
