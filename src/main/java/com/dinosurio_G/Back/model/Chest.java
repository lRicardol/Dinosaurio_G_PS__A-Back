package com.dinosurio_G.Back.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entidad que representa un cofre dentro del mapa.
 * Compatible con controladores REST y servicios asincrónicos.
 */
@Entity
@Table(name = "chests")
public class Chest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String type;          // Tipo: "exp", etc.
    private String contents;      // Contenido del cofre (JSON, texto, etc.)
    private boolean active = true;

    @Embedded
    private Position position;

    private LocalDateTime generatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "map_id")
    private GameMap map;

    protected Chest() {
    }

    public Chest(String contents, Position position) {
        this.contents = contents;
        this.position = position;
        this.generatedAt = LocalDateTime.now();
        this.active = true;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getContents() { return contents; }
    public void setContents(String contents) { this.contents = contents; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public Position getPosition() { return position; }
    public void setPosition(Position position) { this.position = position; }

    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }

    public GameMap getMap() { return map; }
    public void setMap(GameMap map) { this.map = map; }

    /** Marca el cofre como abierto/desactivado */
    public synchronized void openChest() {
        if (active) {
            active = false;
        }
    }

    /** Reactiva el cofre (si fuese regenerado en el mapa) */
    public synchronized void reactivate() {
        if (!active) {
            active = true;
            this.generatedAt = LocalDateTime.now();
        }
    }

    @Override
    public String toString() {
        return "Chest{" +
                "id=" + id +
                ", type='" + type + '\'' +
                ", contents='" + contents + '\'' +
                ", active=" + active +
                ", position=" + position +
                '}';
    }
}
