package com.arhum.validator.service.impl;

import com.arhum.validator.config.RconClient;
import com.arhum.validator.exception.*;
import com.arhum.validator.model.enums.IpStatus;
import com.arhum.validator.model.enums.RconCommands;
import com.arhum.validator.model.rcon.RconRequest;
import com.arhum.validator.model.request.AddressAddRequest;
import com.arhum.validator.model.response.*;
import com.arhum.validator.service.contract.ValidatorService;
import com.arhum.validator.util.GeneralUtils;
import com.google.cloud.compute.v1.*;
import com.google.cloud.storage.*;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

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
import java.util.concurrent.TimeUnit;

    import static com.arhum.validator.util.RconUtils.executeCommand;
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

    @Value("${google.storage.bucket}")
    private String bucketName;

    @Value("${google.storage.filename}")
    private String fileName;

    @Value("${rcon.port}")
    private String rconPort;

    @Value("${rcon.pass}")
    private String rconPass;

    @Autowired
    private FirewallsClient firewallsClient;

    @Autowired
    private MachineTypesClient machineTypesClient;

    @Autowired
    private InstancesClient instancesClient;

    @Autowired
    private Storage storage;

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
    public CommonResponse executeRcon(String address, RconRequest request) throws IOException {
        String res;
        try (RconClient client = new RconClient(address, Integer.parseInt(rconPort), rconPass)){
            RconCommands commandEnum = request.getCommand();

            if (!commandEnum.getIsEnabled()) {
                throw new UnsupportedOperationException("Command '" + commandEnum.name() + "' is not enabled.");
            }

            String finalCommand = commandEnum.format(request.getArguments().toArray());
            res = executeCommand(finalCommand, client);

            return new CommonResponse(res);
        }
        // IOException in case of errors will be thrown by internal methods
    }

    @Override
    public ModListResponse getModList() throws BaseException {
        List<String> modFiles;
        String updatedAt;

        // in a proper scenario, this must be a bucketProvider that will have the try catch there, this code here will therefore look cleaner.
        try {
            // we look for modlist.txt and try to parse it.
            Blob blob = storage.get(BlobId.of(bucketName, fileName));

            if (blob != null) {
                byte[] content = blob.getContent();
                String fileContent = new String(content, StandardCharsets.UTF_8);

                modFiles = Arrays.stream(fileContent.split("\n"))
                        .map(path -> {
                            String fileName = path.substring(path.lastIndexOf("/") + 1);
                            return fileName.replaceAll("\\.jar$", "");
                        })
                        .toList();
                updatedAt = Instant.ofEpochMilli(blob.getUpdateTime()).atZone(ZoneId.of("Asia/Kolkata")).toLocalDateTime().toString(); // we can rely on this

            } else {
                // if the text file doesnt exist, highly likely that the mods are not available to download, at least partially.
                // we can return an empty response for better UX
                logger.info("not found");
                modFiles = new ArrayList<>();
                updatedAt = Instant.now().atZone(ZoneId.of("Asia/Kolkata")).toLocalDateTime().toString();
            }

        } catch (StorageException e) {
            logger.info("GCS error :: {}", e.getMessage());
            throw new InternalServerException("Something went wrong", 500);
        }

        ModListResponse response = new ModListResponse();
        response.setMods(modFiles);
        response.setUpdatedAt(updatedAt);

        return response;
    }

    @Override
    public CommonResponse download(String object) throws BaseException {
        long expiryInMinutes = 5;

        String blobPath = "files/" + object + ".jar"; // this is in accordance to what the frontend sees, so have to add the prefix and suffix.
        BlobId blobId = BlobId.of(bucketName, blobPath);

        try {
            Blob blob = storage.get(blobId);
            if (blob == null || !blob.exists()) {
                logger.warn("File not found in GCS: {}", blobPath);
                throw new NotFoundException("File not found: " + object, 40000);
            }

            BlobInfo blobInfo = BlobInfo.newBuilder(blobId).build();

            URL signedUrl = storage.signUrl(
                    blobInfo,
                    expiryInMinutes,
                    TimeUnit.MINUTES,
                    Storage.SignUrlOption.withV4Signature(),
                    Storage.SignUrlOption.httpMethod(HttpMethod.GET)
            );

            return new CommonResponse(signedUrl.toString());

        } catch (StorageException e) {
            logger.info("GCS error while downloading :: {}", e.getMessage());
            throw new InternalServerException("Something went wrong", 500);
        }
    }

}
