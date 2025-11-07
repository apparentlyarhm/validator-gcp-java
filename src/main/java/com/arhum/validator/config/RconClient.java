package com.arhum.validator.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

public class RconClient implements AutoCloseable {

    public final Socket socket;
    private final OutputStream outputStream;
    private final InputStream inputStream;

    public RconClient(String host, int port) throws IOException {
        this.socket = new Socket(); // create object
        this.socket.connect(new InetSocketAddress(host, port), 2000); // then connect. this way the timeout will take effect
        this.outputStream = socket.getOutputStream();
        this.inputStream = socket.getInputStream();
    }

    @Override
    public void close() throws IOException {
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }
}
