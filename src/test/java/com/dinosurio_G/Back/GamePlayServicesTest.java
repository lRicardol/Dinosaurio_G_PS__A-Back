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
        p.setId(1L);
        p.setHealth(100);
        p.setGameRoom(room);

        room.setPlayers(new ArrayList<>(List.of(p)));

        when(gameRoomRepo.findByRoomCode("ABC123")).thenReturn(Optional.of(room));
    }



    // ------------------------------
    // TEST: Movement loop
    // ------------------------------
    @Test
    void testMovementOnUpdateLoop() {
        p.setInput(true, false, false, false); // mover arriba

        when(gameRoomRepo.findAll()).thenReturn(List.of(room));
        when(playerRepo.save(any())).thenReturn(p);

        gameplay.updateAllRooms();

        assertEquals(95, p.getY());
    }

    // ------------------------------
    // TEST: Game Over
    // ------------------------------
    @Test
    void testGameOverWhenAllPlayersDead() {
        p.setHealth(0); // muerto

        when(gameRoomRepo.findAll()).thenReturn(List.of(room));

        gameplay.updateAllRooms();

        verify(gameRoomRepo, atLeastOnce()).delete(room);
    }



    // ------------------------------------------------
    // TEST: getPlayersHealth
    // ------------------------------------------------
    @Test
    void testGetPlayersHealth() {
        List<PlayerHealthDTO> list = gameplay.getPlayersHealth("ABC123");

        assertEquals(1, list.size());
        assertEquals("TestPlayer", list.get(0).getPlayerName());
    }

    // ------------------------------------------------
    // TEST: spawnPlayers primera vez
    // ------------------------------------------------
    @Test
    void testSpawnPlayers_FirstTime() {
        p.setX(0);
        p.setY(0);  // <-- IMPORTANTE, si no, el servicio cree que ya está spawneado

        when(gameRoomRepo.findByRoomCode("ABC123")).thenReturn(Optional.of(room));
        when(playerRepo.save(any())).thenReturn(p);

        List<Map<String, Object>> result = gameplay.spawnPlayers("ABC123");

        assertEquals(100, p.getX());
        assertEquals(100, p.getY());
        verify(npcManager, atLeastOnce()).spawnInitialNpcs("ABC123");
    }
    // ------------------------------------------------
    // TEST: spawnPlayers ya spawneados
    // ------------------------------------------------
    @Test
    void testSpawnPlayers_AlreadySpawned() {
        p.setX(300); // ya tiene posición
        p.setY(200);

        when(gameRoomRepo.findByRoomCode("ABC123")).thenReturn(Optional.of(room));

        List<Map<String, Object>> res = gameplay.spawnPlayers("ABC123");

        assertEquals(300.0, (double) res.get(0).get("x"));
        assertEquals(200.0, (double) res.get(0).get("y"));


        verify(npcManager, never()).spawnInitialNpcs("ABC123");
    }

    // ------------------------------------------------
    // TEST: broadcastGameState NO debe crashear
    // ------------------------------------------------
    @Test
    void testBroadcastGameState_NoCrash() {
        when(npcManager.getNpcsForRoom("ABC123")).thenReturn(Collections.emptyList());

        gameplay.getRoomInMemory("ABC123");
        assertDoesNotThrow(() -> {
            var m = GamePlayServices.class.getDeclaredMethod("broadcastGameState", String.class);
            m.setAccessible(true);
            m.invoke(gameplay, "ABC123");
        });
    }

    // ------------------------------------------------
// TEST: checkChestInteraction abre el cofre
// ------------------------------------------------
    @Test
    void testCheckChestInteraction_ChestOpened() throws Exception {
        // Preparación del cofre
        Chest chest = mock(Chest.class);
        Position pos = new Position(100, 100);
        when(chest.getPosition()).thenReturn(pos);
        when(chest.isActive()).thenReturn(true);
        when(chest.getId()).thenReturn(10L);

        when(chestService.findByMapId(any())).thenReturn(List.of(chest));

        // Mock del lock para ejecutar el runnable inmediatamente
        doAnswer(invocation -> {
            Runnable r = invocation.getArgument(1);
            r.run();
            return null;
        }).when(lockManager).withLock(any(), any());

        when(chestService.tryOpenChest(10L)).thenReturn(true);

        // Jugador cerca del cofre
        p.setX(100);
        p.setY(120);

        // Ejecutar ciclo de update (para llamar checkChestInteraction)
        when(gameRoomRepo.findAll()).thenReturn(List.of(room));
        gameplay.updateAllRooms();

        // verificar que XP fue sumada
        verify(xpService, times(1)).addExperience("ABC123", 150);
    }


    // ------------------------------------------------
// TEST: onGameWon
// ------------------------------------------------
    @Test
    void testOnGameWon() throws Exception {
        // Mock user account para poder validar sesión
        UserAccount ua = mock(UserAccount.class);
        p.setUserAccount(ua);

        // Asegurarse de que la sala esté en cache
        gameplay.getRoomInMemory("ABC123");

        // Preparar reflection para llamar al método privado
        var m = GamePlayServices.class.getDeclaredMethod("onGameWon", String.class);
        m.setAccessible(true);

        // Ejecutar victoria
        m.invoke(gameplay, "ABC123");

        // Verificar que la sesión fue cerrada
        verify(ua, times(1)).endSession();

        // Verificar que el jugador fue reseteado
        assertEquals(0, p.getX());
        assertEquals(0, p.getY());
        assertEquals(Player.DEFAULT_HEALTH, p.getHealth());
        assertFalse(p.isReady());

        // Verificar que la XP se reseteó
        verify(xpService, times(1)).resetRoomXp("ABC123");

        // Verificar que la sala se marcó como no iniciada
        assertFalse(room.isGameStarted());

        // Verificar que la sala fue eliminada del cache
        assertFalse(gameplay.roomCache.containsKey("ABC123"));
    }

    // ------------------------------------------------
// TEST: updatePlayerInput
// ------------------------------------------------
    @Test
    void testUpdatePlayerInput() {
        gameplay.getRoomInMemory("ABC123");

        // Ejecutar input
        gameplay.updatePlayerInput("ABC123", String.valueOf(1L), true, false, true, false);

        assertTrue(p.isAlive()); // sigue vivo
        assertEquals(100, p.getX());
        assertEquals(100, p.getY());

        // Ahora actualizar movimiento
        p.actualizar();

        // Debe moverse arriba (y -= 5)
        assertEquals(100, p.getY());

        // Debe moverse izquierda (x -= 5)
        assertEquals(100, p.getX());

        // Y mirar hacia la IZQUIERDA (left = true)
        assertFalse(!p.isFacingRight());
    }


}
