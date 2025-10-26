package com.dinosurio_G.Back.service;

import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ExperienceService {

    private static final int GOAL_XP = 1000;
    private final Map<String, Integer> experienceByRoom = new ConcurrentHashMap<>();

    public void addExperience(String roomCode, int amount) {
        experienceByRoom.merge(roomCode, amount, Integer::sum);
        int currentXp = experienceByRoom.get(roomCode);

        if (currentXp >= GOAL_XP) {
            experienceByRoom.put(roomCode, GOAL_XP);
            onGameWon(roomCode);
        }
    }

    public double getProgress(String roomCode) {
        return experienceByRoom.getOrDefault(roomCode, 0) / (double) GOAL_XP;
    }

    private void onGameWon(String roomCode) {
        System.out.println("¡La partida en " + roomCode + " se ha ganado!");
    }
}