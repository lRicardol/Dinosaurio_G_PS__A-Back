package com.dinosurio_G.Back.repository;

import com.dinosurio_G.Back.model.GameRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GameRoomRepository extends JpaRepository<GameRoom, Long> {
    // se encarga de buscar una sala por su código (por ejemplo, cuando un jugador quiere unirse)
    Optional<GameRoom> findByRoomCode(String roomCode);
}
