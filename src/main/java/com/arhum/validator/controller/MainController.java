package com.arhum.validator.controller;

import com.arhum.validator.exception.BaseException;
import com.arhum.validator.model.response.CommonResponse;
import com.arhum.validator.model.response.FirewallRuleResponse;
import com.arhum.validator.model.response.InstanceDetailResponse;
import com.arhum.validator.service.contract.ValidatorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping(value = "api/v2", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Main Controller", description = "Each API will go here")
public class MainController {

    @Autowired
    private ValidatorService validatorService;

    @GetMapping(value = "/ping", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Test endpoint")
    public CommonResponse pong() {
        return validatorService.doPong();
    }

    @GetMapping(value = "/machine", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get VM details")
    public InstanceDetailResponse getResponse() throws BaseException, IOException {
        return validatorService.getMachineDetails();
    }

    @GetMapping(value = "/firewall", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get Firewall details")
    public FirewallRuleResponse getFirewall() throws IOException {
        return validatorService.getFirewallDetails();
    }
}
