package com.arhum.validator.model.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InstanceDetailResponse {

    private String instanceName;
    private String machineType;
    private String instanceId;
    private String status;
    private String creationTimestamp;
    private String publicIp;
    private String cpuPlatform;
    private int cpuCores;
    private int memoryMb;
    private long maxPersistentDisksGb;
    private Map<String, String> metadata;
}
