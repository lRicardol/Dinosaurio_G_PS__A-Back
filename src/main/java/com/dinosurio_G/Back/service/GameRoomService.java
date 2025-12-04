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

    /**
     * Crear una nueva sala
     * Ahora requiere que el host tenga una cuenta de usuario
     */
    @Transactional
    public GameRoom createRoom(String roomName, int maxPlayers, String playerName) {

        // Buscar la cuenta de usuario por playerName
        UserAccount hostAccount = userAccountRepository.findByPlayerName(playerName)
                .orElseThrow(() -> new RuntimeException("El jugador " + playerName + " no está registrado"));

        // VALIDACIÓN: Verificar si el jugador ya está en una partida activa
        Player existingPlayer = playerRepository.findByPlayerName(playerName).orElse(null);
        if (existingPlayer != null && existingPlayer.getGameRoom() != null) {
            GameRoom currentRoom = existingPlayer.getGameRoom();

            // Si la sala actual está en juego, no permitir crear nueva sala
            if (currentRoom.isGameStarted()) {
                throw new GameConflictException("No puedes crear una sala mientras estás en una partida activa. " +
                        "Termina tu partida actual primero (Sala: " + currentRoom.getRoomCode() + ")");
            }

            // Si la sala no está iniciada, limpiar automáticamente
            System.out.println(" Limpiando sala anterior (no iniciada) para " + playerName);
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

        // ACTIVAR SESIÓN al crear sala
        hostAccount.startSession();
        userAccountRepository.save(hostAccount);
        System.out.println(" Sesión activada para " + playerName);

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

        // VALIDACIÓN: No permitir unirse a una sala que ya empezó
        if (room.isGameStarted()) {
            throw new RuntimeException("No puedes unirte a una partida que ya ha comenzado");
        }

        // Buscar la cuenta de usuario
        UserAccount userAccount = userAccountRepository.findByPlayerName(playerName)
                .orElseThrow(() -> new RuntimeException("El jugador " + playerName + " no está registrado"));

        // Verificar si ya existe un Player con ese nombre
        Player player = playerRepository.findByPlayerName(playerName).orElse(null);

        // VALIDACIÓN MEJORADA: Verificar si está en otra partida ACTIVA
        if (player != null && player.getGameRoom() != null) {
            GameRoom currentRoom = player.getGameRoom();

            // Si está en la misma sala, permitir (reconexión)
            if (currentRoom.getId().equals(room.getId())) {
                System.out.println(" Reconexión de " + playerName + " a la sala " + roomCode);
                return gameRoomRepository.save(room);
            }

            // Si está en otra sala y esa sala está en juego, bloquear
            if (currentRoom.isGameStarted()) {
                throw new GameConflictException("Ya estás en una partida activa (Sala: " +
                        currentRoom.getRoomCode() + "). Termina tu partida actual antes de unirte a otra.");
            }

            // Si está en otra sala que NO ha empezado, limpiar automáticamente
            System.out.println(" Limpiando sala anterior (no iniciada) para " + playerName);
            currentRoom.getPlayers().remove(player);
            gameRoomRepository.save(currentRoom);
        }

        // VALIDACIÓN ANTI-SUPLANTACIÓN: Verificar si tiene sesión activa
        if (userAccount.isHasActiveSession()) {
            throw new GameConflictException("Esta cuenta ya tiene una sesión activa en otro dispositivo. " +
                    "Cierra la otra sesión primero.");
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

            // ACTIVAR SESIÓN al unirse a sala
            userAccount.startSession();
            userAccountRepository.save(userAccount);
            System.out.println(" Sesión activada para " + playerName);
        }

        return gameRoomRepository.save(room);
    }

    @Transactional
    public GameRoom startGame(String roomCode) {
        GameRoom room = getRoomByCode(roomCode);

        // VALIDACIÓN: Verificar que ningún jugador esté en otra partida activa
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

        // 1) Reset XP antes de empezar
        experienceService.resetRoomXp(roomCode);

        // 2) Spawn players (esto posiciona y persiste jugadores)
        gamePlayServices.spawnPlayers(roomCode);

        // 3) marcamos la sala como iniciada y persistimos
        room.setGameStarted(true);
        GameRoom saved = gameRoomRepository.saveAndFlush(room);
        System.out.println(" GAME STARTED? -> " + saved.isGameStarted());

        // 4) Notificar a todos los clientes que la partida empezó
        messagingTemplate.convertAndSend("/topic/game/" + roomCode + "/event",
                Map.of("type", "GAME_STARTED", "roomCode", roomCode));

        // 5) Enviar estado completo inmediatamente
        messagingTemplate.convertAndSend("/topic/game/" + roomCode + "/state",
                GameRoomMapper.toDTO(saved));

        return saved;
    }

    /**
     * Método para terminar una partida y liberar a los jugadores
     */
    @Transactional
    public void endGame(String roomCode) {
        GameRoom room = getRoomByCode(roomCode);

        // Marcar partida como terminada
        room.setGameStarted(false);

        // Desactivar sesiones de todos los jugadores
        for (Player player : room.getPlayers()) {
            UserAccount account = player.getUserAccount();
            if (account != null) {
                account.endSession();
                userAccountRepository.save(account);
                System.out.println(" Sesión finalizada para " + player.getPlayerName());
            }
        }

        gameRoomRepository.save(room);

        // Notificar a los clientes
        messagingTemplate.convertAndSend("/topic/game/" + roomCode + "/event",
                Map.of("type", "GAME_ENDED", "roomCode", roomCode));
    }

    /**
     * Método para salir de una sala (solo si la partida NO ha empezado)
     */
    @Transactional
    public void leaveRoom(String roomCode, String playerName) {
        GameRoom room = getRoomByCode(roomCode);

        // VALIDACIÓN: No permitir salir de una partida en curso
        if (room.isGameStarted()) {
            throw new RuntimeException("No puedes salir de una partida que ya ha comenzado. " +
                    "Espera a que termine.");
        }

        Player player = playerRepository.findByPlayerName(playerName)
                .orElseThrow(() -> new RuntimeException("Jugador no encontrado"));

        if (player.getGameRoom() != null && player.getGameRoom().getId().equals(room.getId())) {
            room.getPlayers().remove(player);
            player.setGameRoom(null);
            player.setReady(false);
            player.setHost(false);

            // Desactivar sesión
            UserAccount account = player.getUserAccount();
            if (account != null) {
                account.endSession();
                userAccountRepository.save(account);
            }

            playerRepository.save(player);
            gameRoomRepository.save(room);

            System.out.println(" " + playerName + " salió de la sala " + roomCode);
        }
    }

    // Eliminar sala
    @Transactional
    public void deleteRoom(String roomCode) {
        GameRoom room = getRoomByCode(roomCode);

        // Liberar a todos los jugadores
        for (Player player : room.getPlayers()) {
            UserAccount account = player.getUserAccount();
            if (account != null) {
                account.endSession();
                userAccountRepository.save(account);
            }
            player.setGameRoom(null);
            playerRepository.save(player);
        }

        gameRoomRepository.delete(room);
        System.out.println(" Sala " + roomCode + " eliminada");
    }
}