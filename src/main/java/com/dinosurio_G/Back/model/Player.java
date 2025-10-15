package com.dinosurio_G.Back.model;

import jakarta.persistence.*;

@Entity
public class Player {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String playerName;
    private boolean ready = false;
    private boolean host = false;

    @ManyToOne
    @JoinColumn(name = "game_room_id")
    private GameRoom gameRoom;

    public Player() {}

    public Player(String playerName, boolean host) {
        this.playerName = playerName;
        this.host = host;
    }

    // Getters y Setters
    public Long getId() { return id; }
    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }
    public boolean isReady() { return ready; }
    public void setReady(boolean ready) { this.ready = ready; }
    public boolean isHost() { return host; }
    public void setHost(boolean host) { this.host = host; }
    public GameRoom getGameRoom() { return gameRoom; }
    public void setGameRoom(GameRoom gameRoom) { this.gameRoom = gameRoom; }
}
