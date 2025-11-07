package com.arhum.validator.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.nio.charset.StandardCharsets;

@Getter
@Setter
@AllArgsConstructor
public class RconPacket {

    /**
     * Helper constant for the character set recommended by the protocol.
     * <i><p>
     * "Using the ISO-LATIN-1/ISO-8859_1 charset instead of the US-ASCII charset yields much better results for those servers.
     * (those => which color code responses)
     * <p>
     * Alternatively removing byte 167 and one subsequent byte from the payload will remove all color tokens making the text more
     * human-readable for clients that do not subsequently colorize those tokens."</i>
     */
    public static final java.nio.charset.Charset CHARSET = StandardCharsets.ISO_8859_1;

    private int requestId;
    private int type;
    private String body;

}
