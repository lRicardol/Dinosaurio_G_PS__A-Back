package com.dinosurio_G.Back.dto;

import com.dinosurio_G.Back.model.GameRoom;
import com.dinosurio_G.Back.model.Player;

import java.util.List;
import java.util.stream.Collectors;

public class GameRoomMapper {

    public static GameRoomDTO toDTO(GameRoom room) {
        List<PlayerDTO> players = room.getPlayers().stream()
                .map(p -> new PlayerDTO(p.getId(), p.getPlayerName(), p.isReady(), p.isHost()))
                .collect(Collectors.toList());

        return new GameRoomDTO(
                room.getId(),
                room.getRoomCode(),
                room.getRoomName(),
                room.isGameStarted(),
                room.getMaxPlayers(),
                players
        );
    }
}
