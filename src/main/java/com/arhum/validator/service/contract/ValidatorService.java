package com.arhum.validator.service.contract;

import com.arhum.validator.exception.BaseException;
import com.arhum.validator.model.request.AddressAddRequest;
import com.arhum.validator.model.response.*;

import java.io.IOException;

public interface ValidatorService {

    CommonResponse doPong();

    CommonResponse addIpToFirewall(AddressAddRequest request) throws BaseException;

    CommonResponse isIpPresent(String ip) throws BaseException;

    CommonResponse purgeFirewall() throws BaseException;

    InstanceDetailResponse getMachineDetails() throws BaseException;

    FirewallRuleResponse getFirewallDetails() throws BaseException;

    MOTDResponse getServerInfo(String address) throws IOException;

    ModListResponse getModList() throws BaseException;

    CommonResponse download(String object) throws BaseException;
}