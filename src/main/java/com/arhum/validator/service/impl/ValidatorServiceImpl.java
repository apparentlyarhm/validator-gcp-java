package com.arhum.validator.service.impl;

import com.arhum.validator.exception.BaseException;
import com.arhum.validator.model.request.AddressAddRequest;
import com.arhum.validator.model.response.CommonResponse;
import com.arhum.validator.model.response.FirewallRuleResponse;
import com.arhum.validator.model.response.InstanceDetailResponse;
import com.arhum.validator.service.contract.ValidatorService;
import com.google.cloud.compute.v1.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class ValidatorServiceImpl implements ValidatorService {
    private static final Logger logger = LoggerFactory.getLogger(ValidatorServiceImpl.class);

    @Value("${google.project-id}")
    private String projectId;

    @Value("${google.compute.zone}")
    private String zone;

    @Value("${google.compute.instance-name}")
    private String instanceName;

    @Value("${google.compute.firewall-name}")
    private String firewallName;

    @Override
    public CommonResponse doPong() {
        return new CommonResponse("pong!");
    }

    @Override
    public CommonResponse addIpToFirewall(AddressAddRequest request) {
        return null;
    }

    @Override
    public InstanceDetailResponse getMachineDetails() throws BaseException, IOException {
        try (InstancesClient instancesClient = InstancesClient.create();
             MachineTypesClient machineTypesClient = MachineTypesClient.create()) {

            Instance instance = instancesClient.get(projectId, zone, instanceName);

            String publicIp = null;
            for (NetworkInterface networkInterface : instance.getNetworkInterfacesList()) {
                if (!networkInterface.getAccessConfigsList().isEmpty()) {
                    publicIp = networkInterface.getAccessConfigs(0).getNatIP();
                    break;
                }
            }

            String machineTypeUrl = instance.getMachineType();
            String machineTypeName = machineTypeUrl.substring(machineTypeUrl.lastIndexOf("/") + 1);
            MachineType machineType = machineTypesClient.get(projectId, zone, machineTypeName);

            InstanceDetailResponse response = new InstanceDetailResponse();

            response.setInstanceName(instance.getName());
            response.setMachineType(machineType.getName());
            response.setInstanceId(String.valueOf(instance.getId()));
            response.setCpuPlatform(instance.getCpuPlatform());
            response.setStatus(instance.getStatus());
            response.setCreationTimestamp(instance.getCreationTimestamp());
            response.setPublicIp(publicIp);
            response.setCpuCores(machineType.getGuestCpus());
            response.setMemoryMb(machineType.getMemoryMb());
            response.setMaxPersistentDisksGb(machineType.getMaximumPersistentDisksSizeGb());

            return response;
        }
    }

    @Override
    public FirewallRuleResponse getFirewallDetails() throws IOException {
        try (FirewallsClient firewallsClient = FirewallsClient.create()) {
            Firewall firewall = firewallsClient.get(projectId, firewallName);

            String status = firewall.hasDisabled() && firewall.getDisabled() ? "DISABLED" : "ENABLED";
            String direction = firewall.getDirection();
            int allowedIpCount = firewall.getAllowedList()
                    .stream()
                    .mapToInt(Allowed::getPortsCount)
                    .sum();

            return new FirewallRuleResponse(firewallName, status, direction, allowedIpCount);
        }
    }
}
