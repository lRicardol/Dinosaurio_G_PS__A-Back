package com.dinosurio_G.Back.service;

import com.dinosurio_G.Back.model.GameRoom;
import com.dinosurio_G.Back.model.Player;
import com.dinosurio_G.Back.repository.GameRoomRepository;
import com.dinosurio_G.Back.repository.PlayerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PlayerService {

    private final PlayerRepository playerRepository;
    private final GameRoomRepository gameRoomRepository;

    @Autowired
    public PlayerService(PlayerRepository playerRepository, GameRoomRepository gameRoomRepository) {
        this.playerRepository = playerRepository;
        this.gameRoomRepository = gameRoomRepository;
    }

    // CRUD básico
    public List<Player> getAllPlayers() {
        return playerRepository.findAll();
    }

    public Optional<Player> getPlayerById(Long id) {
        return playerRepository.findById(id);
    }

    public Player savePlayer(Player player) {
        return playerRepository.save(player);
    }

    public void deletePlayer(Long id) {
        playerRepository.deleteById(id);
    }

    // Alternar estado "listo/no listo"
    public Player toggleReady(String roomCode, String playerName) {
        GameRoom room = gameRoomRepository.findByRoomCode(roomCode)
                .orElseThrow(() -> new RuntimeException("Sala no encontrada con código: " + roomCode));

        Player player = playerRepository.findByPlayerNameAndGameRoom(playerName, room)
                .orElseThrow(() -> new RuntimeException("Jugador no encontrado en la sala"));

        player.setReady(!player.isReady()); // Cambia el estado
        return playerRepository.save(player);
    }

    // Se encarga de eliminar jugador de una sala (opcional para futuro)
    public void removePlayer(String roomCode, String playerName) {
        GameRoom room = gameRoomRepository.findByRoomCode(roomCode)
                .orElseThrow(() -> new RuntimeException("Sala no encontrada con código: " + roomCode));

        Player player = playerRepository.findByPlayerNameAndGameRoom(playerName, room)
                .orElseThrow(() -> new RuntimeException("Jugador no encontrado en la sala"));

        playerRepository.delete(player);
    }
}
