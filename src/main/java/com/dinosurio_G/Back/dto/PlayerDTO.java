package com.dinosurio_G.Back.dto;

public class PlayerDTO {

    private Long id;
    private String playerName;
    private boolean ready;
    private boolean host;
    private int x;
    private int y;
    private int health;

    public PlayerDTO() {}

    public PlayerDTO(Long id, String playerName, boolean ready, boolean host, int x, int y, int health) {
        this.id = id;
        this.playerName = playerName;
        this.ready = ready;
        this.host = host;
        this.x = x;
        this.y = y;
        this.health = health;
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

    public int getX() { return x; }
    public void setX(int x) { this.x = x; }
    public int getY() { return y; }
    public void setY(int y) { this.y = y; }
    public int getHealth() { return health; }
    public void setHealth(int health) { this.health = health; }
}
