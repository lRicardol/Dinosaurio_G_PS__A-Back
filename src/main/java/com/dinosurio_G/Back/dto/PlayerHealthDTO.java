package com.dinosurio_G.Back.dto;

public class PlayerHealthDTO {
    private String playerName;
    private int health;

    public PlayerHealthDTO(String playerName, int health) {
        this.playerName = playerName;
        this.health = health;
    }

    public String getPlayerName() {
        return playerName;
    }
    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }
    public int getHealth() {
        return health;
    }
    public void setHealth(int health) {
        this.health = health;
    }
}
