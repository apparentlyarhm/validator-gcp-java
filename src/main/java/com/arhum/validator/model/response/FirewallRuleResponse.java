package com.arhum.validator.model.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FirewallRuleResponse {

    private String name;
    private String status;
    private String direction;
    private int allowedIpCount;

    public FirewallRuleResponse(String name, String status, String direction, int allowedIpCount) {
        this.name = name;
        this.status = status;
        this.direction = direction;
        this.allowedIpCount = allowedIpCount;
    }
}
