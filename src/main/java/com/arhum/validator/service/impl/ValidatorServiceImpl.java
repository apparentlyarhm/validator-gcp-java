package com.arhum.validator.service.impl;

import com.arhum.validator.exception.AlreadyExistsException;
import com.arhum.validator.exception.BadRequestException;
import com.arhum.validator.exception.BaseException;
import com.arhum.validator.exception.InternalServerException;
import com.arhum.validator.model.enums.IpStatus;
import com.arhum.validator.model.request.AddressAddRequest;
import com.arhum.validator.model.response.*;
import com.arhum.validator.provider.contract.GcsProvider;
import com.arhum.validator.service.contract.ValidatorService;
import com.arhum.validator.util.GeneralUtils;
import com.google.cloud.compute.v1.*;
import com.google.cloud.storage.Blob;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.arhum.validator.util.SocketUtils.*;

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

    @Value("${minecraft-server.port}")
    private String port;

    @Value("${google.storage.filename}")
    private String fileName;

    @Autowired
    private FirewallsClient firewallsClient;

    @Autowired
    private MachineTypesClient machineTypesClient;

    @Autowired
    private InstancesClient instancesClient;

    @Autowired
    private GcsProvider gcsProvider;

    @Override
    public CommonResponse doPong() {
        return new CommonResponse("pong!");
    }

    /*
    Instead of directly modifying the Java source code, @SneakyThrows operates during the compilation phase.
    Lombok injects bytecode instructions that effectively trick the Java compiler into believing that the
    method does not throw the specified checked exception. At runtime, the exception is still thrown as a
    checked exception, but the compiler's check is bypassed.

    In this case the patchAsync().get() can throw ExecutionException || InterruptedException
     */
    @SneakyThrows
    @Override
    public CommonResponse addIpToFirewall(AddressAddRequest request) throws BaseException {
        String ip = request.getAddress();
        String target = ip + "/32";  // a singular IPv4 will always have /32 suffix.

        GeneralUtils.validateIPv4Address(ip);

        Firewall firewall = firewallsClient.get(projectId, firewallName);
        List<String> newSourceIpList = new ArrayList<>(firewall.getSourceRangesList());

        if (newSourceIpList.contains(target)) {
            throw new AlreadyExistsException("This IP already exists in the firewall rule!", 2222);
        }
        // Adding logic
        if (newSourceIpList.size() > 50) {
            // We will purge all current addresses.
            newSourceIpList.clear();
        }

        newSourceIpList.add(target);

        Firewall newFirewallState = firewall.toBuilder()
                .clearSourceRanges()
                .addAllSourceRanges(newSourceIpList)
                .build();

        PatchFirewallRequest patchRequest = PatchFirewallRequest.newBuilder()
                .setFirewall(firewallName)
                .setProject(projectId)
                .setFirewallResource(newFirewallState)
                .build();

        // Execute patch
        Operation operation = firewallsClient.patchAsync(patchRequest).get();

        // Optional: check operation status
        if (operation.hasError()) {
            throw new InternalServerException("Failed to patch firewall: " + operation.getError(), 500);
        }
        return new CommonResponse("Done");
    }

    // Sneaky throws is used for the same reason above
    @SneakyThrows
    @Override
    public CommonResponse purgeFirewall() throws BaseException {
        Firewall firewall = firewallsClient.get(projectId, firewallName);
        List<String> newSourceIpList = new ArrayList<>(firewall.getSourceRangesList());

        if (newSourceIpList.isEmpty()) {
            throw new BadRequestException("Allowed list is already empty!", 400);
        }
        newSourceIpList.clear();
        newSourceIpList.add("1.1.1.1/32"); // A dummy address because emptying it wasn't working

        Firewall newFirewallState = firewall.toBuilder()
                .clearSourceRanges()
                .addAllSourceRanges(newSourceIpList)
                .build();

        PatchFirewallRequest patchRequest = PatchFirewallRequest.newBuilder()
                .setFirewall(firewallName)
                .setProject(projectId)
                .setFirewallResource(newFirewallState)
                .build();

        // Execute patch
        Operation operation = firewallsClient.patchAsync(patchRequest).get();

        // Optional: check operation status
        if (operation.hasError()) {
            throw new InternalServerException("Failed to patch firewall: " + operation.getError(), 500);
        }
        return new CommonResponse("Done");
    }

    @Override
    public CommonResponse isIpPresent(String ip) throws BaseException {
        String target = ip + "/32";  // a singular IPv4 will always have /32 suffix.

        GeneralUtils.validateIPv4Address(ip);

        Firewall firewall = firewallsClient.get(projectId, firewallName);
        List<String> sourceIps = firewall.getSourceRangesList();

        CommonResponse response = new CommonResponse();
        if (sourceIps.contains(target) || sourceIps.contains("0.0.0.0/0")) {
            // either IP is present or any address is allowed over the firewall, the frontend does not need to know
            response.setMessage((String.valueOf(IpStatus.PRESENT)));
        } else {
            response.setMessage(String.valueOf(IpStatus.NOT_PRESENT));
        }

        return response;
    }

    @Override
    public InstanceDetailResponse getMachineDetails() throws BaseException {
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
        response.setInstanceZone(machineType.getZone());
        response.setMachineType(machineType.getName());
        response.setInstanceId(String.valueOf(instance.getId()));
        response.setCpuPlatform(instance.getCpuPlatform());
        response.setStatus(instance.getStatus());
        response.setCreationTimestamp(instance.getCreationTimestamp());
        response.setPublicIp(publicIp);
        response.setCpuCores(machineType.getGuestCpus());
        response.setMemoryMb(machineType.getMemoryMb());
        response.setDiskGb(instance.getDisks(0).getDiskSizeGb()); // Change if more disks are added, highly doubt it tho for this use case

        return response;

    }

    @Override
    public FirewallRuleResponse getFirewallDetails() throws BaseException {
        Firewall firewall = firewallsClient.get(projectId, firewallName);

        String status = firewall.hasDisabled() && firewall.getDisabled() ? "DISABLED" : "ENABLED";
        String direction = firewall.getDirection();

        int allowedIpCount = firewall.getSourceRangesCount();

        return new FirewallRuleResponse(firewallName, status, direction, allowedIpCount);

    }

    @Override
    public MOTDResponse getServerInfo(String address) throws IOException {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(2000);
            InetSocketAddress target = new InetSocketAddress(address, Integer.parseInt(port));

            byte[] handshakeRequest = createHandshakePacket();
            sendPacket(socket, target, handshakeRequest);

            byte[] responseBuffer = new byte[1024];
            DatagramPacket responsePacket = receivePacket(socket, responseBuffer);

            int sessionId = extractSessionId(responsePacket.getData());

            byte[] fullQueryRequest = createFullQueryPacket(sessionId);
            sendPacket(socket, target, fullQueryRequest);

            DatagramPacket fullResponsePacket = receivePacket(socket, responseBuffer);

            return new MOTDResponse(parseFullQueryResponse(fullResponsePacket.getData()));
        }
    }

    @Override
    public ModListResponse getModList() throws BaseException {
        List<String> modFiles;
        String updatedAt;

        Blob blob = gcsProvider.getBlob(fileName);
        String fileContent = new String(blob.getContent(), StandardCharsets.UTF_8);

        modFiles = Arrays.stream(fileContent.split("\n"))
                .filter(path -> !path.trim().isEmpty())
                .map(path -> path.substring(path.lastIndexOf("/") + 1).replaceAll("\\.jar$", ""))
                .toList();

        updatedAt = Instant.ofEpochMilli(blob.getUpdateTime())
                .atZone(ZoneId.of("Asia/Kolkata"))
                .toString();

        ModListResponse response = new ModListResponse();
        response.setMods(modFiles);
        response.setUpdatedAt(updatedAt);

        return response;
    }

    @Override
    public CommonResponse download(String object) throws BaseException {
        String blobPath = "files/" + object + ".jar"; // this is in accordance to what the frontend sees, so have to add the prefix and suffix.
        URL signedUrl = gcsProvider.generateV4SignedUrl(blobPath);

        return new CommonResponse(signedUrl.toString());
    }

    @Override
    public CommonResponse uploadFinalZip(MultipartFile file) throws BaseException {
        String blobName = "final/final.zip";
        gcsProvider.uploadFile(blobName, file, "application/zip");

        return new CommonResponse("File uploaded successfully.");
    }
}
