package com.dinosurio_G.Back.repository;

import com.dinosurio_G.Back.model.GameRoom;
import com.dinosurio_G.Back.model.Player;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PlayerRepository extends JpaRepository<Player, Long> {
    Optional<Player> findByPlayerNameAndGameRoom(String playerName, GameRoom gameRoom);
    Optional<Player> findByPlayerName(String playerName);
}
