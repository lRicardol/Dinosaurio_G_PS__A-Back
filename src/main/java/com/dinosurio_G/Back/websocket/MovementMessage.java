package com.dinosurio_G.Back.websocket;

public class MovementMessage {
    private String playerName;
    private boolean arriba;
    private boolean abajo;
    private boolean izquierda;
    private boolean derecha;

    public MovementMessage() {}

    public MovementMessage(String playerName, boolean arriba, boolean abajo, boolean izquierda, boolean derecha) {
        this.playerName = playerName;
        this.arriba = arriba;
        this.abajo = abajo;
        this.izquierda = izquierda;
        this.derecha = derecha;
    }

    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }
    public boolean isArriba() { return arriba; }
    public void setArriba(boolean arriba) { this.arriba = arriba; }
    public boolean isAbajo() { return abajo; }
    public void setAbajo(boolean abajo) { this.abajo = abajo; }
    public boolean isIzquierda() { return izquierda; }
    public void setIzquierda(boolean izquierda) { this.izquierda = izquierda; }
    public boolean isDerecha() { return derecha; }
    public void setDerecha(boolean derecha) { this.derecha = derecha; }
}
