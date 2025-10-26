package com.dinosurio_G.Back.dto;

public class PlayerDTO {

    private Long id;
    private String playerName;
    private boolean ready;
    private boolean host;


    public PlayerDTO() {}

    public PlayerDTO(Long id, String playerName, boolean ready, boolean host) {
        this.id = id;
        this.playerName = playerName;
        this.ready = ready;
        this.host = host;

    }

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }

    public boolean isReady() { return ready; }
    public void setReady(boolean ready) { this.ready = ready; }

    public boolean isHost() { return host; }
    public void setHost(boolean host) { this.host = host; }



}
