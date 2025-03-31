package com.arhum.validator.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SocketUtils {
    private static final Logger logger = LoggerFactory.getLogger(SocketUtils.class);

    public static byte[] createHandshakePacket() {
        return new byte[]{(byte) 0xFE, (byte) 0xFD, 0x09, 0x01, 0x01, 0x01, 0x01};
    }

    public static int extractSessionId(byte[] responseData) {
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

    public static byte[] createFullQueryPacket(int challengeToken) {
        ByteBuffer buffer = ByteBuffer.allocate(15); // 7 bytes header + 4 bytes token + 4 padding bytes
        buffer.put(new byte[]{(byte) 0xFE, (byte) 0xFD, 0x00, 0x01, 0x01, 0x01, 0x01}); // Header
        buffer.putInt(challengeToken); // Challenge token as big-endian
        buffer.put(new byte[]{0x00, 0x00, 0x00, 0x00}); // Padding

        byte[] fullQueryPacket = buffer.array();
        logger.info("Created full query packet: {}", bytesToHex(fullQueryPacket));

        return fullQueryPacket;
    }

    public static Map<String, Object> parseFullQueryResponse(byte[] responseData) {
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
    public static void sendPacket(DatagramSocket socket, InetSocketAddress targetAddress, byte... data) throws IOException {
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
    public static DatagramPacket receivePacket(DatagramSocket socket, byte[] buffer) throws IOException {
        final DatagramPacket dp = new DatagramPacket(buffer, buffer.length);
        socket.receive(dp);
        logger.info("Received response: {}", bytesToHex(dp.getData()));
        return dp;
    }

    /**
     * Utility method to convert a byte array to a human-readable hex string.
     */
    public static String bytesToHex(byte[] bytes) {
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
