package com.arhum.validator.service.contract;

import com.arhum.validator.exception.BaseException;
import com.arhum.validator.model.request.AddressAddRequest;
import com.arhum.validator.model.request.GetServerInfoRequest;
import com.arhum.validator.model.response.CommonResponse;
import com.arhum.validator.model.response.FirewallRuleResponse;
import com.arhum.validator.model.response.InstanceDetailResponse;
import com.arhum.validator.model.response.MOTDResponse;

import java.io.IOException;
import java.util.Map;

public interface ValidatorService {

    CommonResponse doPong();

    CommonResponse addIpToFirewall(AddressAddRequest request) throws BaseException;

    CommonResponse isIpPresent(String ip) throws BaseException;

    CommonResponse purgeFirewall() throws BaseException;

    InstanceDetailResponse getMachineDetails() throws BaseException;

    FirewallRuleResponse getFirewallDetails() throws BaseException;

    MOTDResponse getServerInfo(String address) throws IOException;
}