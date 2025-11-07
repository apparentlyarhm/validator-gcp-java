package com.arhum.validator.util;

import com.arhum.validator.config.RconClient;
import com.arhum.validator.model.RconPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.atomic.AtomicInteger;

public class RconUtils {
    private static final Logger logger = LoggerFactory.getLogger(RconUtils.class);

    private static final int PACKET_TYPE_LOGIN = 3;
    private static final int PACKET_TYPE_COMMAND = 2;
    private static final int PACKET_TYPE_INVALID_AUTH_RESPONSE = -1;
    private static final int SENTINEL_REQUEST_TYPE = 200; // suggested by the protocol spec to detect the end of a fragmented response
    private static final AtomicInteger requestIdGenerator = new AtomicInteger(0);

    private static int sendPacket(int type, String payload, RconClient rconClient) throws IOException {
        int requestId = requestIdGenerator.incrementAndGet();
        byte[] payloadBytes = payload.getBytes(RconPacket.CHARSET);

        // length of: Request ID (4) + Type (4) + Payload (var) + Null Terminator (1) + Pad (1)
        int packetLength = 4 + 4 + payloadBytes.length + 1 + 1;
        ByteBuffer buffer = ByteBuffer.allocate(4 + packetLength);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        buffer.putInt(packetLength);
        buffer.putInt(requestId);
        buffer.putInt(type);
        buffer.put(payloadBytes);
        buffer.put((byte) 0x00); // Null-terminated payload
        buffer.put((byte) 0x00); // 1-byte pad

        rconClient.socket.getOutputStream().write(buffer.array());
        rconClient.socket.getOutputStream().flush();

        return requestId;
    }

    private static RconPacket readPacket(RconClient client) throws IOException {
        byte[] lengthBytes = client.socket.getInputStream().readNBytes(4); // read packet length (first 4 bytes)

        if (lengthBytes.length < 4) {
            throw new IOException("Connection closed while reading packet length.");
        }

        int packetLength = ByteBuffer.wrap(lengthBytes).order(ByteOrder.LITTLE_ENDIAN).getInt();

        byte[] packetData = client.socket.getInputStream().readNBytes(packetLength);
        if (packetData.length < packetLength) {
            throw new IOException("Connection closed while reading packet data.");
        }

        ByteBuffer buffer = ByteBuffer.wrap(packetData).order(ByteOrder.LITTLE_ENDIAN);

        int requestId = buffer.getInt();
        int type = buffer.getInt();

        // The rest is the payload, terminated by a null byte.
        // We find the null byte to correctly size the string.
        byte[] bodyBytes = new byte[buffer.remaining() - 2]; // Subtract 2 for the two null bytes
        buffer.get(bodyBytes);
        String body = new String(bodyBytes, RconPacket.CHARSET);

        // Discard the final two null bytes from the buffer
        buffer.get(); // null terminator
        buffer.get(); // padding

        return new RconPacket(requestId, type, body);
    }

    /**
     * Authenticates the client with the RCON server.
     *
     * @param client The RCON client instance.
     * @throws IOException if a network error occurs.
     */
    public static Boolean authenticate(RconClient client) throws IOException {
        int requestId = sendPacket(PACKET_TYPE_LOGIN, client.getPassword(), client);
        RconPacket response = readPacket(client);

        // response can be either -1 or same (failure, success respectively)
        // added a sanity check if-branch. it should NOT reach at least as per specs
        if (response.getRequestId() == PACKET_TYPE_INVALID_AUTH_RESPONSE) {
            logger.info("RCON password seems incorrect");
            return false;
        }

        if (response.getRequestId() != requestId) {
            logger.info("wow this should NOT happen, received different request id");
            return false;
        }
        logger.info("RCON password is correct..");
        return true;
    }

    /**
     * Executes a RCON command and returns response.
     * <p>
     * The Minecraft server can fragment responses across multiple packets. There's no simple
     * way to know when the last response packet has been received; approaches include:
     *
     * <li>Wait until we receive a packet with a payload length < 4096 (not 100% reliable!)</li>
     * <li>Wait for n seconds</li>
     * <li><b>[We are using this]</b> Send two command packets; the second command triggers a response
     *   from the server with the same Request ID, and from this we know that we've already received
     *   the full response to the first command. The second packet should use a command that will not produce
     *   fragmented output.</li>
     *
     * <br>
     * <a href='https://minecraft.wiki/w/RCON'>See more</a>
     * @param command The command to execute.
     * @param client The RCON client instance
     * @return The server's response to the command.
     * @throws IOException if a network error occurs or if password is wrong.
     */
    public static String executeCommand(String command, RconClient client) throws IOException {

        if (authenticate(client)) {
            // we first send the actual command, then a dummy
            int mainRequestId = sendPacket(PACKET_TYPE_COMMAND, command, client);
            int sentinelRequestId = sendPacket(SENTINEL_REQUEST_TYPE, "", client);

            StringBuilder responseBody = new StringBuilder();
            while (true) {
                RconPacket response = readPacket(client);

                if (response.getRequestId() == mainRequestId) {
                    // This is part of our main command's response. Append it.
                    responseBody.append(response.getBody());

                } else if (response.getRequestId() == sentinelRequestId) {
                    // We received the reply to our sentinel packet, which means the
                    // main response is complete. We can stop listening.
                    break;

                } else {
                    // we should not reach here
                    logger.info("Warning: Received packet with unexpected ID: {}", response.getRequestId());
                    throw new IOException("req id mismatch: aborting..");
                }
            }
            return responseBody.toString();
        }

        throw new IOException("Authentication has failed");
    }
}
