package com.arhum.validator.model.enums;

import lombok.Getter;

public enum RconCommands {

    SAY("say %s", true),
    KICK("kick %s", true),
    BAN("ban %s", true),
    PARDON("pardon %s", true),
    WHITELIST_ADD("whitelist add %s", false),
    WHITELIST_REMOVE("whitelist remove %s", false),
    WHITELIST_RELOAD("whitelist reload", false),
    TIME_SET("time set %s", true),
    WEATHER_SET("weather %s", true),
    GIVE("give %s %s %s", false), // pain in the ass so not gonna support for now
    TELEPORT("tp %s %s", true), // player1 player2
    STOP("stop", true);

    @Getter
    private final String commandFormat;

    @Getter
    private final Boolean isSupported;

    RconCommands(String command, Boolean isSupported) {
        this.commandFormat = command;
        this.isSupported = isSupported;
    }
    public String format(Object... args) {
        return String.format(this.commandFormat, args);
    }

}
