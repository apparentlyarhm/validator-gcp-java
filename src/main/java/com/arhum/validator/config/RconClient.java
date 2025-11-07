package com.arhum.validator.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class RconClient {
    private final Socket socket;
    private final OutputStream outputStream;
    private final InputStream inputStream;

    public RconClient(String host, int port) throws IOException {
        this.socket = new Socket(host, port);
        this.outputStream = socket.getOutputStream();
        this.inputStream = socket.getInputStream();
    }
}
