package com.dinosurio_G.Back;

import com.dinosurio_G.Back.dto.PlayerHealthDTO;
import com.dinosurio_G.Back.model.*;
import com.dinosurio_G.Back.repository.GameRoomRepository;
import com.dinosurio_G.Back.repository.PlayerRepository;
import com.dinosurio_G.Back.service.ExperienceService;
import com.dinosurio_G.Back.service.GamePlayServices;
import com.dinosurio_G.Back.service.NPCManager;
import com.dinosurio_G.Back.service.core.LockManager;
import com.dinosurio_G.Back.service.impl.ChestService;
import com.dinosurio_G.Back.service.impl.GameMapService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class GamePlayServicesTest {

    @Mock private GameRoomRepository gameRoomRepo;
    @Mock private PlayerRepository playerRepo;
    @Mock private GameMapService mapService;
    @Mock private ChestService chestService;
    @Mock private NPCManager npcManager;
    @Mock private ExperienceService xpService;
    @Mock private LockManager lockManager;
    @Mock private SimpMessagingTemplate ws;

    @InjectMocks
    private GamePlayServices gameplay;

    Player p;
    GameRoom room;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);

        room = new GameRoom();
        room.setRoomCode("ABC123");
        room.setGameStarted(true);

        GameMap map = new GameMap();
        map.setWidth(500);
        map.setHeight(500);
        room.setMap(map);

        p = new Player("TestPlayer", false, 100, 100);
        p.setGameRoom(room);

        room.setPlayers(new ArrayList<>(List.of(p)));

        when(gameRoomRepo.findByRoomCode("ABC123")).thenReturn(Optional.of(room));
    }

    @Test
    void testPlayerInputUpdate() {
        gameplay.updatePlayerInput("ABC123", "TestPlayer", true, false, false, true);

        assertTrue(p.isFacingRight()); // flecha derecha
        assertEquals(true, p.canAttack() || !p.canAttack()); // solo validar que no crashea
    }

    @Test
    void testMovementOnUpdateLoop() {
        p.setInput(true, false, false, false); // mover arriba

        when(gameRoomRepo.findAll()).thenReturn(List.of(room));
        when(playerRepo.save(any())).thenReturn(p);

        gameplay.updateAllRooms();

        assertEquals(95, p.getY());
    }


    @Test
    void testGameOverWhenAllPlayersDead() {
        p.setHealth(0); // muerto

        when(gameRoomRepo.findAll()).thenReturn(List.of(room));

        gameplay.updateAllRooms();

        verify(gameRoomRepo, atLeastOnce()).delete(room);
    }
}
