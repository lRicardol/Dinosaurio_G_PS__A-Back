package com.dinosurio_G.Back;
import com.dinosurio_G.Back.controller.PlayerController;
import com.dinosurio_G.Back.model.GameRoom;
import com.dinosurio_G.Back.model.NPC;
import com.dinosurio_G.Back.model.Player;
import com.dinosurio_G.Back.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class PlayerControllerTest {
    @Mock
    private GameRoomService gameRoomService;
    @Mock
    private PlayerService playerService;
    @Mock
    private GamePlayServices gamePlayServices;
    @Mock
    private NPCManager npcManager;
    @Mock
    private AuthenticationService authenticationService;

    @InjectMocks
    private PlayerController playerController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testDeletePlayer() {
        Long playerId = 1L;
        doNothing().when(playerService).deletePlayer(playerId);

        playerController.deletePlayer(playerId);

        verify(playerService, times(1)).deletePlayer(playerId);
    }

    @Test
    void testGetAllPlayers() {
        Player p = new Player();
        p.setId(1L);
        p.setPlayerName("PlayerX");
        p.setHost(true);
        when(playerService.getAllPlayers()).thenReturn(List.of(p));

        var result = playerController.getAllPlayers();

        assertEquals(1, result.size());
        assertEquals("PlayerX", result.get(0).getPlayerName());
        assertTrue(result.get(0).isHost());
    }

    @Test
    void testToggleReady() {
        GameRoom room = new GameRoom();
        Player player = new Player();
        player.setPlayerName("PlayerX");
        player.setGameRoom(room);

        when(playerService.toggleReady("ROOM1", "PlayerX")).thenReturn(player);

        var dto = playerController.toggleReady("ROOM1", "PlayerX", null);

        assertNotNull(dto);
        verify(playerService, times(1)).toggleReady("ROOM1", "PlayerX");
    }

    @Test
    void testMovePlayer() {
        GameRoom room = new GameRoom();
        when(gameRoomService.getRoomByCode("ROOM1")).thenReturn(room);

        var dto = playerController.movePlayer("ROOM1", "PlayerX", true, false, false, true, null);

        verify(gamePlayServices, times(1))
                .updatePlayerInput("ROOM1", "PlayerX", true, false, false, true);
        assertNotNull(dto);
    }

    @Test
    void testGetPlayerPositions() {
        GameRoom room = new GameRoom();
        Player p = new Player("PlayerX", false, 10, 20);
        room.getPlayers().add(p);
        when(gameRoomService.getRoomByCode("ROOM1")).thenReturn(room);

        Map<String, Object> response = playerController.getPlayerPositions("ROOM1");

        List<?> players = (List<?>) response.get("players");
        assertEquals(1, players.size());
    }

    @Test
    void testGetPlayersHealth() {
        when(gamePlayServices.getPlayersHealth("ROOM1")).thenReturn(List.of());

        var result = playerController.getPlayersHealth("ROOM1");

        assertTrue(result.isEmpty());
    }

    @Test
    void testSpawnPlayers() {
        when(gamePlayServices.spawnPlayers("ROOM1")).thenReturn(List.of(Map.of("player", "PlayerX")));

        ResponseEntity<Map<String, Object>> response = playerController.spawnPlayers("ROOM1");

        assertEquals("ROOM1", response.getBody().get("roomCode"));
        assertTrue(((List<?>) response.getBody().get("players")).size() > 0);
    }

    @Test
    void testAddExperience() {
        when(gamePlayServices.getProgress("ROOM1")).thenReturn(0.5);

        Map<String, Object> result = playerController.addExperience("ROOM1", 100);

        assertEquals(100, result.get("addedXP"));
        assertEquals(0.5, result.get("progress"));
    }

    @Test
    void testGetExperienceProgress() {
        when(gamePlayServices.getProgress("ROOM1")).thenReturn(0.75);

        Map<String, Object> result = playerController.getExperienceProgress("ROOM1");

        assertEquals(0.75, result.get("progress"));
        assertEquals(75.0, result.get("percentage"));
    }

    @Test
    void testGetNpcPositions() {
        NPC npc = new NPC(0,0,100,5);
        when(npcManager.getNpcsForRoom("ROOM1")).thenReturn(List.of(npc));

        Map<String, Object> response = playerController.getNpcPositions("ROOM1");

        List<?> npcs = (List<?>) response.get("npcs");
        assertEquals(1, npcs.size());
    }
}
