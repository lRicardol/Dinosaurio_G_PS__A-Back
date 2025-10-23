package com.dinosurio_G.Back.service;

import com.dinosurio_G.Back.dto.PlayerHealthDTO;
import com.dinosurio_G.Back.model.GameRoom;
import com.dinosurio_G.Back.model.Player;
import com.dinosurio_G.Back.repository.GameRoomRepository;
import com.dinosurio_G.Back.repository.PlayerRepository;
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
        playerRepository.save(host);

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
        playerRepository.save(player);

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
    //Recibir input del jugador
    public void updatePlayerInput(String roomCode, String playerName,
                                  boolean arriba, boolean abajo,
                                  boolean izquierda, boolean derecha) {
        GameRoom room = getRoomByCode(roomCode);
        Optional<Player> playerOpt = room.getPlayers().stream()
                .filter(p -> p.getPlayerName().equals(playerName))
                .findFirst();

        if (playerOpt.isPresent()) {
            Player player = playerOpt.get();
            player.setInput(arriba, abajo, izquierda, derecha);
            playerRepository.save(player);
        } else {
            throw new RuntimeException("Jugador no encontrado en la sala");
        }
    }

    // Actualizar posiciones de todos los jugadores
    public void updateGame(String roomCode) {
        GameRoom room = getRoomByCode(roomCode);
        room.getPlayers().forEach(player -> {
            player.actualizar();
            playerRepository.save(player);
        });
    }

    @Scheduled(fixedRate = 50) // Cada 50 ms
    public void updateAllRooms() {
        gameRoomRepository.findAll().forEach(room -> {
            if (room.isGameStarted()) {
                updateGame(room.getRoomCode());
            }
        });
    }

    public List<PlayerHealthDTO> getPlayersHealth(String roomCode) {
        GameRoom room = getRoomByCode(roomCode);

        return room.getPlayers().stream()
                .map(p -> new PlayerHealthDTO(p.getPlayerName(), p.getHealth()))
                .collect(Collectors.toList());
    }



}
