package com.dinosurio_G.Back.dto;

public class PlayerHealthDTO {
    private String playerName;
    private int health;
    private int maxHealth;
    private boolean alive;

    // Constructor original (para compatibilidad)
    public PlayerHealthDTO(String playerName, int health) {
        this.playerName = playerName;
        this.health = health;
        this.maxHealth = 100; // Default
        this.alive = health > 0;
    }

    //  Nuevo constructor completo
    public PlayerHealthDTO(String playerName, int health, int maxHealth, boolean alive) {
        this.playerName = playerName;
        this.health = health;
        this.maxHealth = maxHealth;
        this.alive = alive;
    }

    // Getters y Setters
    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }

    public int getHealth() { return health; }
    public void setHealth(int health) { this.health = health; }

    public int getMaxHealth() { return maxHealth; }
    public void setMaxHealth(int maxHealth) { this.maxHealth = maxHealth; }

    public boolean isAlive() { return alive; }
    public void setAlive(boolean alive) { this.alive = alive; }
}
