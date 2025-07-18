package com.arhum.validator.util;

import com.arhum.validator.exception.BadRequestException;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class GeneralUtils {

    public static void validateIPv4Address(String ip) {
        try {
            InetAddress address = InetAddress.getByName(ip);
            if (!(address instanceof Inet4Address)) {
                throw new BadRequestException("Only IPv4 addresses are allowed", 4001);
            }
        } catch (UnknownHostException e) {
            throw new BadRequestException("Invalid IP address format", 4000);
        }
    }
}