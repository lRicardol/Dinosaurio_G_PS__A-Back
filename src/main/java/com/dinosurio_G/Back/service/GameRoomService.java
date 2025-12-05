package com.dinosurio_G.Back.service;

import com.dinosurio_G.Back.exception.GameConflictException;
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

    @Autowired
    private DistributedCacheService distributedCache; //  NUEVO

    @Transactional
    public GameRoom createRoom(String roomName, int maxPlayers, String playerName) {

        UserAccount hostAccount = userAccountRepository.findByPlayerName(playerName)
                .orElseThrow(() -> new RuntimeException("El jugador " + playerName + " no está registrado"));

        // VALIDACIÓN con Redis
        if (distributedCache.hasActiveSession(playerName)) {
            throw new GameConflictException("Ya tienes una sesión activa en otro dispositivo. " +
                    "Cierra la otra sesión primero.");
        }

        Player existingPlayer = playerRepository.findByPlayerName(playerName).orElse(null);
        if (existingPlayer != null && existingPlayer.getGameRoom() != null) {
            GameRoom currentRoom = existingPlayer.getGameRoom();

            if (currentRoom.isGameStarted()) {
                throw new GameConflictException("No puedes crear una sala mientras estás en una partida activa. " +
                        "Termina tu partida actual primero (Sala: " + currentRoom.getRoomCode() + ")");
            }

            existingPlayer.setGameRoom(null);
            existingPlayer.setReady(false);
            existingPlayer.setHost(false);
            existingPlayer.setX(0);
            existingPlayer.setY(0);
            playerRepository.save(existingPlayer);
        }

        GameRoom room = new GameRoom();
        room.setRoomName(roomName);
        room.setMaxPlayers(maxPlayers);
        room.setRoomCode(UUID.randomUUID().toString().substring(0, 6).toUpperCase());
        room.setGameStarted(false);

        GameMap map = gameMapService.createMapForRoom();
        room.setMap(map);

        GameRoom savedRoom = gameRoomRepository.save(room);

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

        // ACTIVAR SESIÓN en BD y Redis
        hostAccount.startSession();
        userAccountRepository.save(hostAccount);
        distributedCache.startSession(playerName); //  Redis

        return gameRoomRepository.save(savedRoom);
    }

    public List<GameRoom> getAllRooms() {
        return gameRoomRepository.findAll();
    }

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

        if (room.isGameStarted()) {
            throw new RuntimeException("No puedes unirte a una partida que ya ha comenzado");
        }

        UserAccount userAccount = userAccountRepository.findByPlayerName(playerName)
                .orElseThrow(() -> new RuntimeException("El jugador " + playerName + " no está registrado"));

        Player player = playerRepository.findByPlayerName(playerName).orElse(null);

        if (player != null && player.getGameRoom() != null) {
            GameRoom currentRoom = player.getGameRoom();

            if (currentRoom.getId().equals(room.getId())) {
                System.out.println(" Reconexión de " + playerName + " a la sala " + roomCode);
                return gameRoomRepository.save(room);
            }

            if (currentRoom.isGameStarted()) {
                throw new GameConflictException("Ya estás en una partida activa (Sala: " +
                        currentRoom.getRoomCode() + "). Termina tu partida actual antes de unirte a otra.");
            }

            currentRoom.getPlayers().remove(player);
            gameRoomRepository.save(currentRoom);
        }

        // VALIDACIÓN con Redis
        if (distributedCache.hasActiveSession(playerName)) {
            throw new GameConflictException("Esta cuenta ya tiene una sesión activa en otro dispositivo. " +
                    "Cierra la otra sesión primero.");
        }

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

            // ACTIVAR SESIÓN en BD y Redis
            userAccount.startSession();
            userAccountRepository.save(userAccount);
            distributedCache.startSession(playerName); //  Redis
        }

        return gameRoomRepository.save(room);
    }

    @Transactional
    public GameRoom startGame(String roomCode) {
        GameRoom room = getRoomByCode(roomCode);

        for (Player player : room.getPlayers()) {
            Player dbPlayer = playerRepository.findByPlayerName(player.getPlayerName())
                    .orElseThrow(() -> new RuntimeException("Jugador no encontrado: " + player.getPlayerName()));

            if (dbPlayer.getGameRoom() != null &&
                    !dbPlayer.getGameRoom().getId().equals(room.getId()) &&
                    dbPlayer.getGameRoom().isGameStarted()) {
                throw new GameConflictException("El jugador " + player.getPlayerName() +
                        " está en otra partida activa. No se puede iniciar el juego.");
            }
        }

        experienceService.resetRoomXp(roomCode);
        gamePlayServices.spawnPlayers(roomCode);

        room.setGameStarted(true);
        GameRoom saved = gameRoomRepository.saveAndFlush(room);

        messagingTemplate.convertAndSend("/topic/game/" + roomCode + "/event",
                Map.of("type", "GAME_STARTED", "roomCode", roomCode));

        messagingTemplate.convertAndSend("/topic/game/" + roomCode + "/state",
                GameRoomMapper.toDTO(saved));

        return saved;
    }

    @Transactional
    public void endGame(String roomCode) {
        GameRoom room = getRoomByCode(roomCode);
        room.setGameStarted(false);

        for (Player player : room.getPlayers()) {
            UserAccount account = player.getUserAccount();
            if (account != null) {
                account.endSession();
                userAccountRepository.save(account);
                distributedCache.endSession(player.getPlayerName()); //  Redis
            }
        }

        gameRoomRepository.save(room);

        messagingTemplate.convertAndSend("/topic/game/" + roomCode + "/event",
                Map.of("type", "GAME_ENDED", "roomCode", roomCode));
    }

    @Transactional
    public void leaveRoom(String roomCode, String playerName) {
        GameRoom room = getRoomByCode(roomCode);

        if (room.isGameStarted()) {
            throw new RuntimeException("No puedes salir de una partida que ya ha comenzado.");
        }

        Player player = playerRepository.findByPlayerName(playerName)
                .orElseThrow(() -> new RuntimeException("Jugador no encontrado"));

        if (player.getGameRoom() != null && player.getGameRoom().getId().equals(room.getId())) {
            room.getPlayers().remove(player);
            player.setGameRoom(null);
            player.setReady(false);
            player.setHost(false);

            UserAccount account = player.getUserAccount();
            if (account != null) {
                account.endSession();
                userAccountRepository.save(account);
                distributedCache.endSession(playerName); //  Redis
            }

            playerRepository.save(player);
            gameRoomRepository.save(room);
        }
    }

    @Transactional
    public void deleteRoom(String roomCode) {
        GameRoom room = getRoomByCode(roomCode);

        for (Player player : room.getPlayers()) {
            UserAccount account = player.getUserAccount();
            if (account != null) {
                account.endSession();
                userAccountRepository.save(account);
                distributedCache.endSession(player.getPlayerName()); //  Redis
            }
            player.setGameRoom(null);
            playerRepository.save(player);
        }

        gameRoomRepository.delete(room);
        distributedCache.deleteRoom(roomCode); //  Redis
    }
}