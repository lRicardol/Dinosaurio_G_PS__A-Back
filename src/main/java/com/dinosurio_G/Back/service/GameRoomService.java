package com.dinosurio_G.Back.service;

import com.dinosurio_G.Back.dto.PlayerHealthDTO;
import com.dinosurio_G.Back.model.GameMap;
import com.dinosurio_G.Back.model.GameRoom;
import com.dinosurio_G.Back.model.Player;
import com.dinosurio_G.Back.model.Position;
import com.dinosurio_G.Back.repository.GameRoomRepository;
import com.dinosurio_G.Back.repository.PlayerRepository;
import com.dinosurio_G.Back.service.impl.GameMapService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class GameRoomService {

    @Autowired
    private GameRoomRepository gameRoomRepository;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private GamePlayServices gamePlayServices;

    @Autowired
    private GameMapService gameMapService;

    // Crear una nueva sala
    public GameRoom createRoom(String roomName, int maxPlayers, String hostName) {
        GameRoom room = new GameRoom();
        room.setRoomName(roomName);
        room.setMaxPlayers(maxPlayers);
        room.setRoomCode(UUID.randomUUID().toString().substring(0, 6).toUpperCase());
        room.setGameStarted(false);

        GameRoom savedRoom = gameRoomRepository.save(room);

        // Crear jugador host
        Player host = new Player();
        host.setPlayerName(hostName);
        host.setHost(true);
        host.setReady(false);
        host.setGameRoom(savedRoom);
        host.setHealth(Player.DEFAULT_HEALTH);
        host.setSpeed(Player.DEFAULT_SPEED);
        playerRepository.save(host);
        savedRoom.getPlayers().add(host);
        gameRoomRepository.save(savedRoom);
        return savedRoom;
    }

    // Obtiene todas las salas
    public List<GameRoom> getAllRooms() {
        return gameRoomRepository.findAll();
    }

    // Obtiene una sala por código
    public GameRoom getRoomByCode(String roomCode) {
        Optional<GameRoom> room = gameRoomRepository.findByRoomCode(roomCode);
        if (room.isEmpty()) {
            throw new RuntimeException("La sala con código " + roomCode + " no existe");
        }
        return room.get();
    }

    // Unirse a una sala
    public GameRoom joinRoom(String roomCode, String playerName) {
        GameRoom room = getRoomByCode(roomCode);

        if (room.getPlayers().size() >= room.getMaxPlayers()) {
            throw new RuntimeException("La sala está llena");
        }

        Player player = new Player();
        player.setPlayerName(playerName);
        player.setReady(false);
        player.setHost(false);
        player.setGameRoom(room);
        room.getPlayers().add(player);
        player.setHealth(Player.DEFAULT_HEALTH);
        player.setSpeed(Player.DEFAULT_SPEED);
        playerRepository.save(player);
        gameRoomRepository.save(room);

        return room;
    }

    // Iniciar juego (solo host)
    public GameRoom startGame(String roomCode) {
        GameRoom room = getRoomByCode(roomCode);
        boolean allReady = room.getPlayers().stream().allMatch(Player::isReady);

        if (!allReady) {
            throw new RuntimeException("No todos los jugadores están listos");
        }


        room.setGameStarted(true);

        return gameRoomRepository.save(room);
    }

    // Eliminar sala
    public void deleteRoom(String roomCode) {
        GameRoom room = getRoomByCode(roomCode);
        gameRoomRepository.delete(room);
    }




}
