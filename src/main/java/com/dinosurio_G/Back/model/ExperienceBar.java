package com.dinosurio_G.Back.model;

import jakarta.persistence.*;

@Entity
public class ExperienceBar {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private int currentXp;
    private int goalXp;
    private boolean completed;

    @OneToOne
    @JoinColumn(name = "game_room_id")
    private GameRoom gameRoom;


    protected ExperienceBar() {}

    public ExperienceBar(int goalXp) {
        this.currentXp = 0;
        this.goalXp = goalXp;
        this.completed = false;
    }

    public synchronized void addXp(int amount) {
        if (completed) return;

        currentXp += amount;
        if (currentXp >= goalXp) {
            currentXp = goalXp;
            completed = true;
        }
    }

    public double getProgress() {
        return goalXp == 0 ? 0 : (double) currentXp / goalXp;
    }

    public boolean isCompleted() {
        return completed;
    }

    public int getCurrentXp() {
        return currentXp;
    }

    public int getGoalXp() {
        return goalXp;
    }

    public void setGameRoom(GameRoom gameRoom) {
        this.gameRoom = gameRoom;
    }

    public GameRoom getGameRoom() { return   gameRoom; }
}
