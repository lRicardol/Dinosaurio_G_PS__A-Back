package com.dinosurio_G.Back;

import com.dinosurio_G.Back.model.GameMap;
import com.dinosurio_G.Back.model.GameRoom;
import com.dinosurio_G.Back.model.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GameRoomTest {

    private GameRoom room;
    private Player player1;
    private Player player2;

    @BeforeEach
    void setUp() {
        room = new GameRoom("Sala Test", 4);
        player1 = new Player();
        player1.setPlayerName("Jugador1");

        player2 = new Player();
        player2.setPlayerName("Jugador2");
    }

    @Test
    void testDefaultConstructorGeneratesRoomCode() {
        GameRoom defaultRoom = new GameRoom();
        assertNotNull(defaultRoom.getRoomCode());
        assertEquals(6, defaultRoom.getRoomCode().length());
    }

    @Test
    void testConstructorWithParameters() {
        assertEquals("Sala Test", room.getRoomName());
        assertEquals(4, room.getMaxPlayers());
        assertNotNull(room.getRoomCode());
    }

    @Test
    void testAddPlayer() {
        room.addPlayer(player1);
        List<Player> players = room.getPlayers();
        assertEquals(1, players.size());
        assertTrue(players.contains(player1));
        assertEquals(room, player1.getGameRoom());
    }

    @Test
    void testRemovePlayer() {
        room.addPlayer(player1);
        room.addPlayer(player2);
        room.removePlayer(player1);

        List<Player> players = room.getPlayers();
        assertEquals(1, players.size());
        assertFalse(players.contains(player1));
        assertNull(player1.getGameRoom());
    }

    @Test
    void testSettersAndGetters() {
        GameMap map = new GameMap();
        room.setMap(map);
        assertEquals(map, room.getMap());

        room.setGameStarted(true);
        assertTrue(room.isGameStarted());

        room.setRoomName("Nueva Sala");
        assertEquals("Nueva Sala", room.getRoomName());

        room.setMaxPlayers(6);
        assertEquals(6, room.getMaxPlayers());
    }
}
