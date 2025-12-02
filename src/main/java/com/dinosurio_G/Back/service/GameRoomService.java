package com.dinosurio_G.Back.service;

import com.dinosurio_G.Back.model.*;
import com.dinosurio_G.Back.repository.GameRoomRepository;
import com.dinosurio_G.Back.repository.PlayerRepository;
import com.dinosurio_G.Back.repository.UserAccountRepository;
import com.dinosurio_G.Back.service.impl.GameMapService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import com.dinosurio_G.Back.dto.GameRoomMapper;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class GameRoomService {

    @Autowired
    private GameRoomRepository gameRoomRepository;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private GameMapService gameMapService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private GamePlayServices gamePlayServices;

    @Autowired
    private ExperienceService experienceService;

    /**
     * Crear una nueva sala
     * Ahora requiere que el host tenga una cuenta de usuario
     */
    public GameRoom createRoom(String roomName, int maxPlayers, String playerName) {

        // Buscar la cuenta de usuario por playerName
        UserAccount hostAccount = userAccountRepository.findByPlayerName(playerName)
                .orElseThrow(() -> new RuntimeException("El jugador " + playerName + " no está registrado"));

        //  FIX: Limpiar jugador si tenía sala anterior
        Player existingPlayer = playerRepository.findByPlayerName(playerName).orElse(null);
        if (existingPlayer != null && existingPlayer.getGameRoom() != null) {
            System.out.println(" Limpiando sala anterior para " + playerName);
            existingPlayer.setGameRoom(null);
            existingPlayer.setReady(false);
            existingPlayer.setHost(false);
            existingPlayer.setX(0);
            existingPlayer.setY(0);
            playerRepository.save(existingPlayer);
        }

        // Crear la sala
        GameRoom room = new GameRoom();
        room.setRoomName(roomName);
        room.setMaxPlayers(maxPlayers);
        room.setRoomCode(UUID.randomUUID().toString().substring(0, 6).toUpperCase());
        room.setGameStarted(false);

        // Asignar un mapa nuevo por sala
        GameMap map = gameMapService.createMapForRoom();
        room.setMap(map);

        GameRoom savedRoom = gameRoomRepository.save(room);

        // Crear el Player vinculado al UserAccount
        Player host;
        if (existingPlayer != null) {
            host = existingPlayer;
        } else {
            host = new Player();
            host.setUserAccount(hostAccount);
            host.setPlayerName(playerName);
        }

        host.setHost(true);
        host.setGameRoom(savedRoom);
        host.setReady(false);
        playerRepository.save(host);

        savedRoom.getPlayers().add(host);
        return gameRoomRepository.save(savedRoom);
    }

    // Listar todas las salas
    public List<GameRoom> getAllRooms() {
        return gameRoomRepository.findAll();
    }

    // Buscar sala por código
    public GameRoom getRoomByCode(String roomCode) {
        return gameRoomRepository.findByRoomCode(roomCode)
                .orElseThrow(() -> new RuntimeException("La sala con código " + roomCode + " no existe"));
    }

    /**
     * Unirse a una sala
     * Ahora requiere que el jugador tenga una cuenta de usuario
     */
    @Transactional
    public GameRoom joinRoom(String roomCode, String playerName) {
        GameRoom room = getRoomByCode(roomCode);

        if (room.getPlayers().size() >= room.getMaxPlayers()) {
            throw new RuntimeException("La sala está llena");
        }

        // Buscar la cuenta de usuario
        UserAccount userAccount = userAccountRepository.findByPlayerName(playerName)
                .orElseThrow(() -> new RuntimeException("El jugador " + playerName + " no está registrado"));

        // Verificar si ya existe un Player con ese nombre
        Player player = playerRepository.findByPlayerName(playerName).orElse(null);

        if (player != null && player.getGameRoom() != null && !player.getGameRoom().getId().equals(room.getId())) {
            throw new RuntimeException("El jugador ya está en otra sala");
        }

        // Crear o reutilizar el Player
        if (player == null) {
            player = new Player();
            player.setUserAccount(userAccount);
            player.setPlayerName(playerName);
        }

        if (!room.getPlayers().contains(player)) {
            room.addPlayer(player);
            player.setHost(false);
            player.setReady(false);
            playerRepository.save(player);
        }

        return gameRoomRepository.save(room);
    }

    @Transactional
    public GameRoom startGame(String roomCode) {
        GameRoom room = getRoomByCode(roomCode);

        // 1) Reset XP antes de empezar
        experienceService.resetRoomXp(roomCode);

        // 2) Spawn players (esto posiciona y persiste jugadores)
        gamePlayServices.spawnPlayers(roomCode);

        // 3) marcamos la sala como iniciada y persistimos
        room.setGameStarted(true);
        GameRoom saved = gameRoomRepository.saveAndFlush(room);
        System.out.println("GAME STARTED? -> " + saved.isGameStarted());

        // 4) Notificar a todos los clientes que la partida empezó
        messagingTemplate.convertAndSend("/topic/game/" + roomCode + "/event",
                Map.of("type", "GAME_STARTED", "roomCode", roomCode));

        // 5) Enviar estado completo inmediatamente
        messagingTemplate.convertAndSend("/topic/game/" + roomCode + "/state", GameRoomMapper.toDTO(saved));

        return saved;
    }

    // Eliminar sala
    public void deleteRoom(String roomCode) {
        GameRoom room = getRoomByCode(roomCode);
        gameRoomRepository.delete(room);
    }
}