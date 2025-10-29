package com.dinosurio_G.Back.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ExperienceService {

    private static final int GOAL_XP = 1000;
    private final Map<String, Integer> experienceByRoom = new ConcurrentHashMap<>();

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public void addExperience(String roomCode, int amount) {
        experienceByRoom.merge(roomCode, amount, Integer::sum);
        int currentXp = experienceByRoom.get(roomCode);

        if (currentXp >= GOAL_XP) {
            experienceByRoom.put(roomCode, GOAL_XP);
            onGameWon(roomCode);
        }

        // Enviar progreso a frontend
        double progress = getProgress(roomCode);
        messagingTemplate.convertAndSend("/topic/game/" + roomCode + "/xp", Map.of("progress", progress, "currentXp", currentXp));
    }

    public double getProgress(String roomCode) {
        return experienceByRoom.getOrDefault(roomCode, 0) / (double) GOAL_XP;
    }

    public void resetRoomXp(String roomCode) {
        experienceByRoom.put(roomCode, 0);
        messagingTemplate.convertAndSend("/topic/game/" + roomCode + "/xp", Map.of("progress", 0.0, "currentXp", 0));
    }

    private void onGameWon(String roomCode) {
        System.out.println("¡La partida en " + roomCode + " se ha ganado!");
        // Notificar a los clientes
        messagingTemplate.convertAndSend("/topic/game/" + roomCode + "/event", Map.of("type", "GAME_WON", "roomCode", roomCode));
    }
}
