package com.dinosurio_G.Back.controller;

import com.dinosurio_G.Back.dto.GameRoomDTO;
import com.dinosurio_G.Back.dto.GameRoomMapper;
import com.dinosurio_G.Back.dto.PlayerHealthDTO;
import com.dinosurio_G.Back.model.GameRoom;
import com.dinosurio_G.Back.model.NPC;
import com.dinosurio_G.Back.model.Player;
import com.dinosurio_G.Back.service.GamePlayServices;
import com.dinosurio_G.Back.service.GameRoomService;
import com.dinosurio_G.Back.service.NPCManager;
import com.dinosurio_G.Back.service.PlayerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/players")
@CrossOrigin(origins = "*")
public class PlayerController {
    @Autowired
    private GameRoomService gameRoomService;

    @Autowired
    private PlayerService playerService;

    @Autowired
    private GamePlayServices gamePlayServices;

    @Autowired
    private NPCManager npcManager;
    // Marcar jugador como listo
    @PutMapping("/{roomCode}/ready")
    public GameRoomDTO toggleReady(@PathVariable String roomCode, @RequestParam String playerName) {
        Player player = playerService.toggleReady(roomCode, playerName);
        GameRoom updatedRoom = player.getGameRoom();
        return GameRoomMapper.toDTO(updatedRoom);
    }

    // Actualizar movimiento del jugador
    @PutMapping("/{roomCode}/move")
    public GameRoomDTO movePlayer(
            @PathVariable String roomCode,
            @RequestParam String playerName,
            @RequestParam boolean arriba,
            @RequestParam boolean abajo,
            @RequestParam boolean izquierda,
            @RequestParam boolean derecha) {

        gamePlayServices.updatePlayerInput(roomCode, playerName, arriba, abajo, izquierda, derecha);
        return GameRoomMapper.toDTO(gameRoomService.getRoomByCode(roomCode));
    }


    // Obtener vida actual de los jugadores
    @GetMapping("/{roomCode}/health")
    public List<PlayerHealthDTO> getPlayersHealth(@PathVariable String roomCode) {
        return gamePlayServices.getPlayersHealth(roomCode);
    }

    @GetMapping("/{roomCode}/positions")
    public Map<String, Object> getPlayerPositions(@PathVariable String roomCode) {
        GameRoom room = gameRoomService.getRoomByCode(roomCode);

        Map<String, Object> response = new HashMap<>();
        response.put("players", room.getPlayers().stream()
                .map(p -> Map.of(
                        "name", p.getPlayerName(),
                        "x", p.getX(),
                        "y", p.getY()
                ))
                .collect(Collectors.toList()));
        return response;
    }

    @GetMapping("/{roomCode}/npcs")
    public Map<String, Object> getNpcPositions(@PathVariable String roomCode) {
        List<NPC> npcs = npcManager.getNpcsForRoom(roomCode);
        Map<String, Object> response = new HashMap<>();
        response.put("npcs", npcs.stream().map(n -> Map.of(
                "id", n.getId(),
                "x", n.getX(),
                "y", n.getY(),
                "health", n.getHealth()
        )).collect(Collectors.toList()));
        return response;
    }
}
