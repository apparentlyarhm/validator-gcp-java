package com.arhum.validator.service.contract;

import com.arhum.validator.exception.BaseException;
import com.arhum.validator.model.response.CommonResponse;
import com.arhum.validator.model.response.FirewallRuleResponse;
import com.arhum.validator.model.response.InstanceDetailResponse;

import java.io.IOException;

public interface ValidatorService {

    public CommonResponse doPong();

    public CommonResponse addIpToFirewall(String address);

    public InstanceDetailResponse getMachineDetails() throws BaseException, IOException;

    public FirewallRuleResponse getFirewallDetails() throws IOException;
}
