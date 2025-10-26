package com.dinosurio_G.Back.controller;

import com.dinosurio_G.Back.dto.CreateRoomRequest;
import com.dinosurio_G.Back.dto.GameRoomDTO;
import com.dinosurio_G.Back.dto.GameRoomMapper;
import com.dinosurio_G.Back.dto.PlayerHealthDTO;
import com.dinosurio_G.Back.model.GameRoom;
import com.dinosurio_G.Back.model.Player;
import com.dinosurio_G.Back.service.GameRoomService;
import com.dinosurio_G.Back.service.PlayerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/rooms")
@CrossOrigin(origins = "*") // Permite conexiones desde el front
public class GameRoomController {

    @Autowired
    private GameRoomService gameRoomService;

    @Autowired
    private PlayerService playerService;

    // Crear una nueva sala
    @PostMapping("/create")
    public GameRoomDTO createRoom(@RequestBody CreateRoomRequest request) {
        GameRoom room = gameRoomService.createRoom(request.getRoomName(), request.getMaxPlayers(), request.getHostName());
        return GameRoomMapper.toDTO(room);
    }

    // Unirse a una sala
    @PostMapping("/{roomCode}/join")
    public GameRoomDTO joinRoom(@PathVariable String roomCode, @RequestParam String playerName) {
        GameRoom room = gameRoomService.joinRoom(roomCode, playerName);
        return GameRoomMapper.toDTO(room);
    }

    // Marcar jugador como listo
    @PutMapping("/{roomCode}/ready")
    public GameRoomDTO toggleReady(@PathVariable String roomCode, @RequestParam String playerName) {
        Player player = playerService.toggleReady(roomCode, playerName);
        GameRoom updatedRoom = player.getGameRoom();
        return GameRoomMapper.toDTO(updatedRoom);
    }

    // Obtener todas las salas
    @GetMapping
    public List<GameRoomDTO> getAllRooms() {
        return gameRoomService.getAllRooms()
                .stream()
                .map(GameRoomMapper::toDTO)
                .collect(Collectors.toList());
    }

    // Obtener una sala por código
    @GetMapping("/{roomCode}")
    public GameRoomDTO getRoomByCode(@PathVariable String roomCode) {
        GameRoom room = gameRoomService.getRoomByCode(roomCode);
        return GameRoomMapper.toDTO(room);
    }

    // Iniciar el juego (solo host)
    @PutMapping("/{roomCode}/start")
    public GameRoomDTO startGame(@PathVariable String roomCode) {
        GameRoom startedRoom = gameRoomService.startGame(roomCode);
        return GameRoomMapper.toDTO(startedRoom);
    }

    // Eliminar sala (opcional)
    @DeleteMapping("/{roomCode}")
    public void deleteRoom(@PathVariable String roomCode) {
        gameRoomService.deleteRoom(roomCode);
    }



}
