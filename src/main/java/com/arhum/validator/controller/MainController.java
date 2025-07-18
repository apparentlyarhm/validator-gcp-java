package com.arhum.validator.controller;

import com.arhum.validator.exception.BaseException;
import com.arhum.validator.model.request.AddressAddRequest;
import com.arhum.validator.model.request.GetServerInfoRequest;
import com.arhum.validator.model.response.CommonResponse;
import com.arhum.validator.model.response.FirewallRuleResponse;
import com.arhum.validator.model.response.InstanceDetailResponse;
import com.arhum.validator.model.response.MOTDResponse;
import com.arhum.validator.service.contract.ValidatorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

@CrossOrigin
@RestController
@RequestMapping(value = "api/v2", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Main Controller", description = "Each API will go here")
public class MainController {

    // TODO: refactor exceptions here

    @Autowired
    private ValidatorService validatorService;

    @GetMapping(value = "/ping", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Test endpoint")
    public CommonResponse pong() {
        return validatorService.doPong();
    }

    @GetMapping(value = "/machine", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get VM details")
    public InstanceDetailResponse getMachineDetailsResponse() throws BaseException {
        return validatorService.getMachineDetails();
    }

    @GetMapping(value = "/firewall", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get Firewall details")
    public FirewallRuleResponse getFirewallDetails() throws BaseException {
        return validatorService.getFirewallDetails();
    }

    @PatchMapping(value = "/firewall/add-ip", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get Firewall details")
    public CommonResponse addUserIp(@RequestBody @Valid AddressAddRequest request) throws BaseException {
        return validatorService.addIpToFirewall(request);
    }

    @GetMapping(value = "/firewall/check-ip", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Check if an IP is allowed in firewall")
    public CommonResponse checkIpInFirewall(@RequestParam String ip) throws BaseException {
        return validatorService.isIpPresent(ip);
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @PatchMapping(value = "/firewall/purge", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Purges the current list of whitelisted URLs- ONLY ADMINS")
    public CommonResponse purge() throws BaseException {
        return validatorService.purgeFirewall();
    }

    @GetMapping(value = "/server-info", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get MOTD of the minecraft server")
    public MOTDResponse getServerInfo(@RequestParam String address) throws IOException {
        return validatorService.getServerInfo(address);
    }
}
