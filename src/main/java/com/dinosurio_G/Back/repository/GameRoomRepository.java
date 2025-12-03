package com.dinosurio_G.Back.repository;

import com.dinosurio_G.Back.model.GameRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GameRoomRepository extends JpaRepository<GameRoom, Long> {
    // se encarga de buscar una sala por su código (por ejemplo, cuando un jugador quiere unirse)
    Optional<GameRoom> findByRoomCode(String roomCode);
    // Buscar sala activa de un jugador específico
    @Query("SELECT gr FROM GameRoom gr JOIN gr.players p " +
            "WHERE p.playerName = :playerName AND gr.gameStarted = true")
    Optional<GameRoom> findActiveGameByPlayerName(@Param("playerName") String playerName);
}
