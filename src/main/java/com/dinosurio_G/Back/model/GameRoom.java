package com.dinosurio_G.Back.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
public class GameRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String roomCode;
    private String roomName;
    private boolean gameStarted = false;
    private int maxPlayers = 4;

    @ManyToOne
    @JoinColumn(name = "map_id")
    private GameMap map;

    @OneToMany(mappedBy = "gameRoom",
            cascade = CascadeType.ALL,
            orphanRemoval = false,
            fetch = FetchType.EAGER)
    private List<Player> players = new ArrayList<>();

    // Constructor por defecto que genera un código aleatorio
    public GameRoom() {
        this.roomCode = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    // Constructor con parámetros
    public GameRoom(String roomName, int maxPlayers) {
        this();
        this.roomName = roomName;
        this.maxPlayers = maxPlayers;
    }

    // Getters y Setters
    public Long getId() {
        return id;
    }

    public String getRoomCode() {
        return roomCode;
    }

    public void setRoomCode(String roomCode) {
        this.roomCode = roomCode;
    }

    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public boolean isGameStarted() {
        return gameStarted;
    }

    public void setGameStarted(boolean gameStarted) {
        this.gameStarted = gameStarted;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public void setMaxPlayers(int maxPlayers) {
        this.maxPlayers = maxPlayers;
    }

    public List<Player> getPlayers() {
        return players;
    }

    public void setPlayers(List<Player> players) {
        this.players = players;
    }
    // ----- Métodos auxiliares -----
    public void addPlayer(Player player) {
        players.add(player);
        player.setGameRoom(this);
    }

    public void removePlayer(Player player) {
        players.remove(player);
        player.setGameRoom(null);
    }

    public GameMap getMap() {
        return map;
    }

    public void setMap(GameMap map) {this.map=map;
    }

    public void setId(long l) {
        this.id = id;
    }
}
