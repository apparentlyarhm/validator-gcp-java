package com.arhum.validator.model.enums;

import lombok.Getter;

public enum RconCommands {

    // player related
    KICK("kick %s", true, true),
    BAN("ban %s", true, true),
    PARDON("pardon %s", true, true),
    TELEPORT("tp %s %s", true,false), // player1 player2

    // other
    SAY("say %s", true, false),
    TIME_SET("time set %s", true, false),
    WEATHER_SET("weather %s", true, false),
    STOP("stop", true, true),
    CUSTOM("custom command can have anything", true, true),

    // might support these later
    WHITELIST_ADD("whitelist add %s", false, true),
    WHITELIST_REMOVE("whitelist remove %s", false, true),
    WHITELIST_RELOAD("whitelist reload", false, true),
    GIVE("give %s %s %s", false, true);

    @Getter
    private final String commandFormat;

    @Getter
    private final Boolean isEnabled;

    @Getter
    private final Boolean isAdmin;

    RconCommands(String command, Boolean isEnabled, Boolean isAdmin) {
        this.commandFormat = command;
        this.isEnabled = isEnabled;
        this.isAdmin = isAdmin;
    }
    public String format(Object... args) {
        return String.format(this.commandFormat, args);
    }

}
