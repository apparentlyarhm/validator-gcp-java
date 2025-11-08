package com.arhum.validator.model.enums;

import lombok.Getter;

public enum RconCommands {

    // player related
    KICK("kick %s", true),
    BAN("ban %s", true),
    PARDON("pardon %s", true),
    TELEPORT("tp %s %s", true), // player1 player2

    // other
    SAY("say %s", true),
    TIME_SET("time set %s", true),
    WEATHER_SET("weather %s", true),
    STOP("stop", true),

    // might support these later
    WHITELIST_ADD("whitelist add %s", false),
    WHITELIST_REMOVE("whitelist remove %s", false),
    WHITELIST_RELOAD("whitelist reload", false),
    GIVE("give %s %s %s", false);

    @Getter
    private final String commandFormat;

    @Getter
    private final Boolean isEnabled;

    RconCommands(String command, Boolean isEnabled) {
        this.commandFormat = command;
        this.isEnabled = isEnabled;
    }
    public String format(Object... args) {
        return String.format(this.commandFormat, args);
    }

}
