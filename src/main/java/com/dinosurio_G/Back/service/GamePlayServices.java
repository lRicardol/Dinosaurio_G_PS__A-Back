package com.dinosurio_G.Back.service;

import com.dinosurio_G.Back.dto.PlayerHealthDTO;
import com.dinosurio_G.Back.model.*;
import com.dinosurio_G.Back.repository.GameRoomRepository;
import com.dinosurio_G.Back.repository.PlayerRepository;
import com.dinosurio_G.Back.service.impl.ChestService;
import com.dinosurio_G.Back.service.impl.GameMapService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class GamePlayServices {
    private static final int goalXp = 1000;
    private final Map<String, Integer> experienceByRoom = new ConcurrentHashMap<>();
    @Autowired
    private GameRoomRepository gameRoomRepository;

    @Autowired
    private PlayerRepository playerRepository;
    @Autowired
    private GameMapService gameMapService;

    @Autowired
    private ChestService chestService;


    public void updatePlayerInput(String roomCode, String playerName,
                                  boolean arriba, boolean abajo,
                                  boolean izquierda, boolean derecha) {
        GameRoom room = getRoomByCode(roomCode);

        room.getPlayers().stream()
                .filter(p -> p.getPlayerName().equals(playerName))
                .findFirst()
                .ifPresent(player -> {
                    player.setInput(arriba, abajo, izquierda, derecha);
                });
    }

    // Loop global del juego (cada 50 ms)
    @Scheduled(fixedRate = 50)
    public void updateAllRooms() {
        gameRoomRepository.findAll().forEach(room -> {
            if (room.isGameStarted()) {
                room.getPlayers().forEach(Player::actualizar);
            }
        });
    }

    //  Obtener la vida actual de los jugadores
    public List<PlayerHealthDTO> getPlayersHealth(String roomCode) {
        GameRoom room = getRoomByCode(roomCode);

        return room.getPlayers().stream()
                .map(p -> new PlayerHealthDTO(p.getPlayerName(), p.getHealth()))
                .collect(Collectors.toList());
    }

    //  Utilidad interna
    private GameRoom getRoomByCode(String roomCode) {
        return gameRoomRepository.findByRoomCode(roomCode)
                .orElseThrow(() -> new RuntimeException("La sala con código " + roomCode + " no existe"));
    }


    // Sumar XP al room
    public void addExperience(String roomCode, int amount) {
        experienceByRoom.merge(roomCode, amount, Integer::sum);
        int currentXp = experienceByRoom.get(roomCode);

        if (currentXp >= goalXp) {
            experienceByRoom.put(roomCode, goalXp);
            onGameWon(roomCode);
        }
    }
    // Obtener progreso (0.0 a 1.0)
    public double getProgress(String roomCode) {
        return experienceByRoom.getOrDefault(roomCode, 0) / (double) goalXp;
    }

    // Metodo que maneja victoria
    private void onGameWon(String roomCode) {
        // Aquí pones la lógica de final de partida
        System.out.println("¡La partida de la sala " + roomCode + " se ha ganado!");
    }

    public List<Map<String, Object>> spawnPlayersWithoutMap(String roomCode) {
        GameRoom room = getRoomByCode(roomCode);
    public void spawnPlayers(GameRoom room, GameMap map, NPCManager npcManager) {
        List<Player> players = room.getPlayers();

        double startX = 100; // posición inicial base
        double startY = 100;
        double offset = 80; // distancia entre jugadores

        for (int i = 0; i < players.size(); i++) {
            Player player = players.get(i);
            player.setX(startX + (i * offset));
            player.setY(startY);
            playerRepository.save(player);
        }

        // Retornamos la lista con sus posiciones
        return players.stream()
                .map(p -> {
                    Map<String, Object> playerData = new HashMap<>();
                    playerData.put("name", p.getPlayerName());
                    playerData.put("x", p.getX());
                    playerData.put("y", p.getY());
                    return playerData;
                })
                .collect(Collectors.toList());
        // Spawn inicial de NPCs
        npcManager.spawnInitialNpcs(room.getRoomCode());
    }



    // Obtener sala en memoria
    public GameRoom getRoomInMemory(String roomCode) {
        return getRoomByCode(roomCode);
    }

}
