package com.dinosurio_G.Back.service;

import com.dinosurio_G.Back.model.GameMap;
import com.dinosurio_G.Back.model.GameRoom;
import com.dinosurio_G.Back.model.Player;
import com.dinosurio_G.Back.repository.GameRoomRepository;
import com.dinosurio_G.Back.repository.PlayerRepository;
import com.dinosurio_G.Back.service.impl.GameMapService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import com.dinosurio_G.Back.service.GamePlayServices;
import com.dinosurio_G.Back.service.ExperienceService;
import com.dinosurio_G.Back.dto.GameRoomMapper;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;



@Service
public class GameRoomService {

    @Autowired
    private GameRoomRepository gameRoomRepository;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private GameMapService gameMapService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private GamePlayServices gamePlayServices;

    @Autowired
    private ExperienceService experienceService;

    // Crear una nueva sala
    public GameRoom createRoom(String roomName, int maxPlayers, String hostName) {

        Player host = playerRepository.findByPlayerName(hostName)
                .orElseThrow(() -> new RuntimeException("El jugador " + hostName + " no existe"));

        GameRoom room = new GameRoom();
        room.setRoomName(roomName);
        room.setMaxPlayers(maxPlayers);
        room.setRoomCode(UUID.randomUUID().toString().substring(0, 6).toUpperCase());
        room.setGameStarted(false);

        // Asignar un mapa nuevo por sala
        GameMap map = gameMapService.createMapForRoom();
        room.setMap(map);

        GameRoom savedRoom = gameRoomRepository.save(room);

        host.setHost(true);
        host.setGameRoom(savedRoom);
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

    @Transactional
    public GameRoom joinRoom(String roomCode, String playerName) {
        GameRoom room = getRoomByCode(roomCode);

        if (room.getPlayers().size() >= room.getMaxPlayers()) {
            throw new RuntimeException("La sala está llena");
        }

        Player player = playerRepository.findByPlayerName(playerName)
                .orElseThrow(() -> new RuntimeException("El jugador " + playerName + " no existe"));

        if (player.getGameRoom() != null && !player.getGameRoom().getId().equals(room.getId())) {
            throw new RuntimeException("El jugador ya está en otra sala");
        }

        if (!room.getPlayers().contains(player)) {
            room.addPlayer(player);
            player.setHost(false);
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
