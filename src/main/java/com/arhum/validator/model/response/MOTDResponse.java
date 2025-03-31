package com.arhum.validator.model.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.ALWAYS)
public class MOTDResponse {

    private String hostname;
    private String hostIp;
    private String plugins;
    private int numPlayers;
    private List<String> players;
    private String gameType;
    private String um;
    private int maxPlayers;
    private int hostPort;
    private String version;
    private String map;
    private String gameId;

    public MOTDResponse(Map<String, Object> rawData) {
        this.hostname = (String) rawData.getOrDefault("hostname", "");
        this.hostIp = (String) rawData.getOrDefault("hostip", "");
        this.plugins = (String) rawData.getOrDefault("plugins", "");
        this.numPlayers = Integer.parseInt((String) rawData.getOrDefault("numplayers", "0"));
        this.players = (List<String>) rawData.getOrDefault("players", List.of());
        this.gameType = (String) rawData.getOrDefault("gametype", "");
        this.um = (String) rawData.getOrDefault("um", "");
        this.maxPlayers = Integer.parseInt((String) rawData.getOrDefault("maxplayers", "0"));
        this.hostPort = Integer.parseInt((String) rawData.getOrDefault("hostport", "25565"));
        this.version = (String) rawData.getOrDefault("version", "");
        this.map = (String) rawData.getOrDefault("map", "");
        this.gameId = (String) rawData.getOrDefault("game_id", "");
    }

    // I'm pretty sure I will never use this, but keeping it just because I can.
    @Override
    public String toString() {
        return "ServerInfo{" +
                "hostname='" + hostname + '\'' +
                ", hostIp='" + hostIp + '\'' +
                ", plugins='" + plugins + '\'' +
                ", numPlayers=" + numPlayers +
                ", players=" + players +
                ", gameType='" + gameType + '\'' +
                ", um='" + um + '\'' +
                ", maxPlayers=" + maxPlayers +
                ", hostPort=" + hostPort +
                ", version='" + version + '\'' +
                ", map='" + map + '\'' +
                ", gameId='" + gameId + '\'' +
                '}';
    }
}
