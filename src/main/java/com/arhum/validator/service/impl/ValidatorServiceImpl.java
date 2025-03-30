package com.arhum.validator.service.impl;

import com.arhum.validator.exception.AlreadyExistsException;
import com.arhum.validator.exception.BaseException;
import com.arhum.validator.model.request.AddressAddRequest;
import com.arhum.validator.model.request.GetServerInfoRequest;
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
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

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

    @Override
    public CommonResponse doPong() {
        return new CommonResponse("pong!");
    }

    @Override
    public CommonResponse addIpToFirewall(AddressAddRequest request) throws IOException, BaseException {
        String ip = request.getAddress();

        try (FirewallsClient firewallsClient = FirewallsClient.create()){
            Firewall firewall = firewallsClient.get(projectId, firewallName);

            if (!firewall.getDisabled()) {
                List<Allowed> allowedList = firewall.getAllowedList();

                for (Allowed allowed : allowedList) {
                    if (allowed.getPortsList().contains(ip)) {
                        throw new AlreadyExistsException("IP address " + ip + " is already in the firewall rule", 10000);
                    }
                }

            }

        }
        return new CommonResponse("test");
    }

    @Override
    public InstanceDetailResponse getMachineDetails() throws IOException {
        try (InstancesClient instancesClient = InstancesClient.create(); MachineTypesClient machineTypesClient = MachineTypesClient.create()) {
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
            response.setInstanceZone(instance.getZone());
            response.setMachineType(machineType.getName());
            response.setInstanceId(String.valueOf(instance.getId()));
            response.setCpuPlatform(instance.getCpuPlatform());
            response.setStatus(instance.getStatus());
            response.setCreationTimestamp(instance.getCreationTimestamp());
            response.setPublicIp(publicIp);
            response.setCpuCores(machineType.getGuestCpus());
            response.setMemoryMb(machineType.getMemoryMb());
            response.setDiskGb(instance.getDisks(0).getDiskSizeGb()); // Change if more disks are added

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

    @Override
    public Map<String, Object> getServerInfo(String address) throws IOException {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(2000);

            logger.info("Socket created with a timeout of 2000ms");

            InetSocketAddress target = new InetSocketAddress(address, Integer.parseInt(port));
            logger.info("Target server: {}@{}", address, port);

            byte[] handshakeRequest = createHandshakePacket();
            logger.info("Generated handshake packet: {}", bytesToHex(handshakeRequest));
            sendPacket(socket, target, handshakeRequest);

            byte[] responseBuffer = new byte[1024];
            DatagramPacket responsePacket = receivePacket(socket, responseBuffer);

            int sessionId = extractSessionId(responsePacket.getData());
            logger.info("Extracted session ID: {}", sessionId);

            byte[] fullQueryRequest = createFullQueryPacket(sessionId);
            logger.info("Generated full query packet: {}", bytesToHex(fullQueryRequest));
            sendPacket(socket, target, fullQueryRequest);

            DatagramPacket fullResponsePacket = receivePacket(socket, responseBuffer);

            Map<String, Object> serverInfo = parseFullQueryResponse(fullResponsePacket.getData());
            logger.info("Successfully parsed server response: {}", serverInfo);

            return serverInfo;
        }
    }

    private byte[] createHandshakePacket() {
        return new byte[]{(byte) 0xFE, (byte) 0xFD, 0x09, 0x01, 0x01, 0x01, 0x01};
    }

    private int extractSessionId(byte[] responseData) {
        int start = 5; // First 5 bytes are fixed headers
        int end = start;

        // Find the null terminator (0x00) marking the end of the challenge token
        while (end < responseData.length && responseData[end] != 0) {
            end++;
        }

        String challengeString = new String(responseData, start, end - start, StandardCharsets.UTF_8).trim();
        int challengeToken = Integer.parseInt(challengeString);

        logger.info("Parsed challenge token: {} -> Big-endian: {}", challengeToken, bytesToHex(ByteBuffer.allocate(4).putInt(challengeToken).array()));

        return challengeToken;
    }

    private byte[] createFullQueryPacket(int challengeToken) {
        ByteBuffer buffer = ByteBuffer.allocate(15); // 7 bytes header + 4 bytes token + 4 padding bytes
        buffer.put(new byte[]{(byte) 0xFE, (byte) 0xFD, 0x00, 0x01, 0x01, 0x01, 0x01}); // Header
        buffer.putInt(challengeToken); // Challenge token as big-endian
        buffer.put(new byte[]{0x00, 0x00, 0x00, 0x00}); // Padding

        byte[] fullQueryPacket = buffer.array();
        logger.info("Created full query packet: {}", bytesToHex(fullQueryPacket));

        return fullQueryPacket;
    }

    private Map<String, Object> parseFullQueryResponse(byte[] responseData) {
        Map<String, Object> serverInfo = new HashMap<>();
        int cursor = 11; // Skip the first 11 bytes

        String responseString = new String(responseData, cursor, responseData.length - cursor, StandardCharsets.UTF_8);
        logger.info("Raw response (excluding first 11 bytes): {}", responseString);

        String[] sections = responseString.split("\u0000\u0001player_\u0000\u0000");
        if (sections.length < 2) {
            logger.warn("Unexpected response format. Returning partial data.");
            return serverInfo;
        }

        String[] keyValuePairs = sections[0].split("\u0000");
        for (int i = 0; i < keyValuePairs.length - 1; i += 2) {
            serverInfo.put(keyValuePairs[i], keyValuePairs[i + 1]);
        }

        String[] players = sections[1].split("\u0000");
        List<String> playerList = Arrays.stream(players).filter(p -> !p.isEmpty()).collect(Collectors.toList());

        serverInfo.put("players", playerList);
        logger.info("Parsed server info: {}", serverInfo);

        return serverInfo;
    }

    /**
     * Helper method to send a datagram packet
     *
     * @param socket
     *            The connection the packet should be sent through
     * @param targetAddress
     *            The target IP
     * @param data
     *            The byte data to be sent
     */
    private static void sendPacket(DatagramSocket socket, InetSocketAddress targetAddress, byte... data) throws IOException {
        DatagramPacket sendPacket = new DatagramPacket(data, data.length, targetAddress.getAddress(), targetAddress.getPort());
        socket.send(sendPacket);
        logger.info("sent packet: {} to {} @ {}", bytesToHex(data), targetAddress.getAddress(), targetAddress.getPort());
    }

    /**
     * Helper method to send a datagram packet
     *
     * @param socket
     *            The connection the packet should be sent through
     * @param targetAddress
     *            The target IP
     * @param data
     *            The byte data to be sent, will be cast to bytes
     */
    private static void sendPacket(DatagramSocket socket, InetSocketAddress targetAddress, int... data) throws IOException {
        final byte[] d = new byte[data.length];
        int i = 0;
        for(int j : data)
            d[i++] = (byte)(j & 0xff);
        sendPacket(socket, targetAddress, d);
    }

    /**
     * Receive a packet from the given socket
     *
     * @param socket
     *            the socket
     * @param buffer
     *            the buffer for the information to be written into
     * @return the entire packet
     */
    private static DatagramPacket receivePacket(DatagramSocket socket, byte[] buffer) throws IOException {
        final DatagramPacket dp = new DatagramPacket(buffer, buffer.length);
        socket.receive(dp);
        logger.info("Received response: {}", bytesToHex(dp.getData()));
        return dp;
    }

    /**
     * Utility method to convert a byte array to a human-readable hex string.
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            hexString.append(String.format("%02X", bytes[i]));
            if (i < bytes.length - 1) {
                hexString.append(" ");
            }
        }
        return hexString.toString();
    }
}
