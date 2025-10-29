package com.dinosurio_G.Back.model;

import jakarta.persistence.*;

@Entity
public class Player {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(unique = true)
    private String playerName;
    private boolean ready = false;
    private boolean host = false;


    private double speed;
    private int health;
    public static final int DEFAULT_HEALTH = 100;
    public static final double DEFAULT_SPEED = 5;

    @Transient
    private long lastAttackTime = 0;
    @Transient
    private boolean facingRight = true;


    private boolean arriba = false;
    private boolean abajo = false;
    private boolean izquierda = false;
    private boolean derecha = false;

    private boolean isAlive = true;

    private double x; // posición en X
    private double y; // posición en Y
    

    @ManyToOne(optional = true)
    @JoinColumn(name = "game_room_id", nullable = true)
    private GameRoom gameRoom;

    public Player() {
        this.health = DEFAULT_HEALTH;
        this.speed = DEFAULT_SPEED;
    }

    public Player(String playerName, boolean host, double startX, double startY ) {
        this.playerName = playerName;
        this.host = host;
        this.x = startX;
        this.y = startY;
        this.speed = DEFAULT_SPEED;
        this.health = DEFAULT_HEALTH;
    }

    //Input de la dirección a la que se mueve el jugador
    public void setInput(boolean arriba, boolean abajo, boolean izquierda, boolean derecha) {
        this.arriba = arriba;
        this.abajo = abajo;
        this.izquierda = izquierda;
        this.derecha = derecha;
    }

    // Actualizar posición del jugador
    public synchronized void actualizar() {
        if (gameRoom == null || gameRoom.getMap() == null) return;

        double newX = x;
        double newY = y;

        // Actualizar posición
        if (arriba && !abajo) newY -= speed;
        if (abajo && !arriba) newY += speed;

        if (izquierda && !derecha) {
            newX -= speed;
            facingRight = false; // Si se mueve a la izquierda, mira a la izquierda
        }
        if (derecha && !izquierda) {
            newX += speed;
            facingRight = true;  // Si se mueve a la derecha, mira a la derecha
        }

        // Limitar dentro del mapa
        GameMap map = gameRoom.getMap();
        newX = Math.max(0, Math.min(map.getWidth(), newX));
        newY = Math.max(0, Math.min(map.getHeight(), newY));

        x = newX;
        y = newY;
    }

// Tiempo de ataque
    public boolean canAttack() {
        long now = System.currentTimeMillis();
        long attackCooldownMs = 1500; //Tiempo de ataque en milisegundos
        if (now - lastAttackTime >= attackCooldownMs) {
            lastAttackTime = now;
            return true;
        }
        return false;
    }

    //Recibir daño
    public synchronized void receiveDamage(int damage) {
        if (!isAlive() || damage <= 0) return;

        this.health -= damage;

        if (this.health <= 0) {
            this.health = 0;
            onDeath();
        }
    }

    public boolean isFacingRight() {
        return facingRight;
    }

    /**
     * Lógica que ocurre cuando el jugador muere.
     */
    private void onDeath() {
        System.out.println(playerName + " ha muerto.");
        // Aquí podrías resetear posición, notificar al front, etc.
    }

    public long getLastAttackTime() {
        return lastAttackTime;
    }

    public void updateLastAttackTime() {
        this.lastAttackTime = System.currentTimeMillis();
    }

    public boolean isAlive() {
        return this.health > 0;
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


    public double getSpeed() { return speed; }
    public void setSpeed(double speed) { this.speed = speed; }
    public int getHealth() { return health; }
    public void setHealth(int health) { this.health = health; }
    public double getX() { return x; }
    public void setX(double x) { this.x = x; }

    public double getY() { return y; }
    public void setY(double y) { this.y = y; }
}
