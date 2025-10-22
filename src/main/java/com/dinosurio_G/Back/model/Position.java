package com.dinosurio_G.Back.model;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

/**
 * Representa una posición en el mapa mediante coordenadas (x, y).
 * Es inmutable para evitar problemas de concurrencia.
 */
@Embeddable
public class Position implements Serializable {

    private double x;
    private double y;

    protected Position() {
    }

    public Position(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public double getX() { return x; }
    public double getY() { return y; }

    /** Devuelve una nueva posición desplazada */
    public Position move(double deltaX, double deltaY) {
        return new Position(this.x + deltaX, this.y + deltaY);
    }

    /** Se encarga de calcular la distancia entre dos posiciones */
    public double distanceTo(Position other) {
        double dx = this.x - other.x;
        double dy = this.y - other.y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Position)) return false;
        Position position = (Position) o;
        return Double.compare(position.x, x) == 0 &&
                Double.compare(position.y, y) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }

    @Override
    public String toString() {
        return "Position{" + "x=" + x + ", y=" + y + '}';
    }
}
