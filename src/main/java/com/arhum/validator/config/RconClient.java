package com.arhum.validator.config;

import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

@Getter
public class RconClient implements AutoCloseable {

    public final Socket socket;
    private final String password;


    public RconClient(String host, int port, String password) throws IOException {
        this.socket = new Socket(); // create object
        this.socket.connect(new InetSocketAddress(host, port), 2000); // then connect. this way the timeout will take effect
        this.password = password;
    }

    @Override
    public void close() throws IOException {
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }
}
