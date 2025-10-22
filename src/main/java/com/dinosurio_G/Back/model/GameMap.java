package com.dinosurio_G.Back.model;

import jakarta.persistence.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Entidad que representa el mapa del juego.
 * Cumple con principios SOLID, es segura para concurrencia
 * y soporta persistencia con JPA.
 */
@Entity
@Table(name = "game_maps")
public class GameMap {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;           // Nombre del mapa
    private String description;    // Descripción textual del mapa
    private String shape;          // Forma: cuadrado, etc.
    private Integer size;          // Tamaño lógico o escala

    private int width;
    private int height;

    @OneToMany(mappedBy = "map", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Chest> chests = new CopyOnWriteArrayList<>();

    protected GameMap() {
    }

    public GameMap(String name, String shape, int width, int height, String description) {
        this.name = name;
        this.shape = shape;
        this.width = width;
        this.height = height;
        this.description = description;
        this.size = Math.max(width, height);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getShape() { return shape; }
    public void setShape(String shape) { this.shape = shape; }

    public Integer getSize() { return size; }
    public void setSize(Integer size) { this.size = size; }

    public int getWidth() { return width; }
    public void setWidth(int width) { this.width = width; }

    public int getHeight() { return height; }
    public void setHeight(int height) { this.height = height; }

    public List<Chest> getChests() {
        return Collections.unmodifiableList(chests);
    }

    /** Calcula la posición central del mapa */
    public Position getCenterPosition() {
        return new Position(width / 2.0, height / 2.0);
    }

    /** Verifica si una posición está dentro de los límites */
    public boolean isWithinBounds(Position position) {
        return position != null &&
                position.getX() >= 0 && position.getX() <= width &&
                position.getY() >= 0 && position.getY() <= height;
    }

    /** Añade un cofre si está dentro de los límites */
    public synchronized boolean addChest(Chest chest) {
        if (chest != null && isWithinBounds(chest.getPosition())) {
            chests.add(chest);
            chest.setMap(this);
            return true;
        }
        return false;
    }

    /** Elimina un cofre del mapa */
    public synchronized void removeChest(Chest chest) {
        if (chest != null) {
            chests.remove(chest);
            chest.setMap(null);
        }
    }

    /** Genera una posición aleatoria válida */
    public Position randomValidPosition(Random random) {
        double x = random.nextDouble() * width;
        double y = random.nextDouble() * height;
        return new Position(x, y);
    }

    @Override
    public String toString() {
        return "GameMap{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", shape='" + shape + '\'' +
                ", size=" + size +
                ", chests=" + chests.size() +
                '}';
    }
}
