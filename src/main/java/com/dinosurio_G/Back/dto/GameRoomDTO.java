package com.dinosurio_G.Back.dto;

import java.util.List;

public class GameRoomDTO {

    private Long id;
    private String roomCode;
    private String name;
    private boolean gameStarted;
    private int maxPlayers;
    private List<PlayerDTO> players;

    public GameRoomDTO() {}

    public GameRoomDTO(Long id, String roomCode, String name, boolean gameStarted, int maxPlayers, List<PlayerDTO> players) {
        this.id = id;
        this.roomCode = roomCode;
        this.name = name;
        this.gameStarted = gameStarted;
        this.maxPlayers = maxPlayers;
        this.players = players;
    }

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getRoomCode() { return roomCode; }
    public void setRoomCode(String roomCode) { this.roomCode = roomCode; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public boolean isGameStarted() { return gameStarted; }
    public void setGameStarted(boolean gameStarted) { this.gameStarted = gameStarted; }

    public int getMaxPlayers() { return maxPlayers; }
    public void setMaxPlayers(int maxPlayers) { this.maxPlayers = maxPlayers; }

    public List<PlayerDTO> getPlayers() { return players; }
    public void setPlayers(List<PlayerDTO> players) { this.players = players; }
}
