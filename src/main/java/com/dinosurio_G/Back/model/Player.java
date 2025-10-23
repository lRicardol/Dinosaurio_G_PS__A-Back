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

    private int x;
    private int y;
    private int speed;
    private int health;
    private static final int DEFAULT_HEALTH = 100;
    private static final int DEFAULT_SPEED = 5;

    @Transient
    private boolean arriba, abajo, izquierda, derecha;

    @ManyToOne
    @JoinColumn(name = "game_room_id")
    private GameRoom gameRoom;

    public Player() {}

    public Player(String playerName, boolean host, int x, int y) {
        this.playerName = playerName;
        this.host = host;
        this.x = x;
        this.y = y;
        this.speed = DEFAULT_SPEED;
        this.health = DEFAULT_HEALTH;
    }

    public void setInput(boolean arriba, boolean abajo, boolean izquierda, boolean derecha) {
        this.arriba = arriba;
        this.abajo = abajo;
        this.izquierda = izquierda;
        this.derecha = derecha;
    }

    public void actualizar() {
        if (arriba && !abajo) y -= speed;
        if (abajo && !arriba) y += speed;
        if (izquierda && !derecha) x -= speed;
        if (derecha && !izquierda) x += speed;
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

    public int getX() { return x; }
    public void setX(int x) { this.x = x; }
    public int getY() { return y; }
    public void setY(int y) { this.y = y; }
    public int getSpeed() { return speed; }
    public void setSpeed(int speed) { this.speed = speed; }
    public int getHealth() { return health; }
    public void setHealth(int health) { this.health = health; }
}
