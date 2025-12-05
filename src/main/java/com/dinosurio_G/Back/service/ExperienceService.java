package com.dinosurio_G.Back.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class ExperienceService {

    private static final int GOAL_XP = 1000;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    @Lazy
    private GamePlayServices gamePlayServices;

    @Autowired
    private DistributedCacheService distributedCache; //  NUEVO

    public void addExperience(String roomCode, int amount) {
        // Obtener XP actual desde Redis
        Integer currentXp = distributedCache.getXp(roomCode);
        if (currentXp == null) {
            currentXp = 0;
        }

        // Sumar XP
        currentXp += amount;

        // Guardar en Redis
        distributedCache.saveXp(roomCode, currentXp);

        if (currentXp >= GOAL_XP) {
            distributedCache.saveXp(roomCode, GOAL_XP);

            System.out.println(" ¡La partida en " + roomCode + " se ha GANADO!");
            gamePlayServices.onGameWon(roomCode);
        }

        // Enviar progreso
        double progress = getProgress(roomCode);
        messagingTemplate.convertAndSend(
                "/topic/game/" + roomCode + "/xp",
                Map.of("progress", progress, "currentXp", currentXp)
        );
    }

    public double getProgress(String roomCode) {
        Integer xp = distributedCache.getXp(roomCode);
        if (xp == null) {
            xp = 0;
        }
        return xp / (double) GOAL_XP;
    }

    public void resetRoomXp(String roomCode) {
        distributedCache.saveXp(roomCode, 0);
        messagingTemplate.convertAndSend(
                "/topic/game/" + roomCode + "/xp",
                Map.of("progress", 0.0, "currentXp", 0)
        );
        System.out.println(" XP reseteado para sala " + roomCode);
    }
}