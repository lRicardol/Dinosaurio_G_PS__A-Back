package com.dinosurio_G.Back;
import com.dinosurio_G.Back.model.GameMap;
import com.dinosurio_G.Back.model.GameRoom;
import com.dinosurio_G.Back.model.Player;
import com.dinosurio_G.Back.model.UserAccount;
import com.dinosurio_G.Back.repository.GameRoomRepository;
import com.dinosurio_G.Back.repository.PlayerRepository;
import com.dinosurio_G.Back.repository.UserAccountRepository;
import com.dinosurio_G.Back.service.ExperienceService;
import com.dinosurio_G.Back.service.GamePlayServices;
import com.dinosurio_G.Back.service.GameRoomService;
import com.dinosurio_G.Back.service.impl.GameMapService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
public class GameRoomServiceTest {

    @InjectMocks
    private GameRoomService gameRoomService;

    @Mock private GameRoomRepository gameRoomRepository;
    @Mock private PlayerRepository playerRepository;
    @Mock private UserAccountRepository userAccountRepository;
    @Mock private GameMapService gameMapService;
    @Mock private SimpMessagingTemplate messagingTemplate;
    @Mock private GamePlayServices gamePlayServices;
    @Mock private ExperienceService experienceService;

    private UserAccount account;
    private Player player;
    private GameRoom room;

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);

        account = new UserAccount();
        account.setId(1L);
        account.setPlayerName("Host");
        account.setHasActiveSession(false);

        player = new Player();
        player.setPlayerName("Host");
        player.setUserAccount(account);

        room = new GameRoom();
        room.setRoomCode("ABC123");
        room.setGameStarted(false);
        room.setPlayers(new ArrayList<>());

        GameMap map = new GameMap();
        map.setWidth(1000);
        map.setHeight(1000);
        when(gameMapService.createMapForRoom()).thenReturn(map);
    }

    // ------------------------------------------------------------
    // 1. Crear sala correctamente
    // ------------------------------------------------------------

    @Test
    void testCreateRoom_OK() {
        when(userAccountRepository.findByPlayerName("Host"))
                .thenReturn(Optional.of(account));

        when(playerRepository.findByPlayerName("Host"))
                .thenReturn(Optional.empty());

        when(gameRoomRepository.save(any(GameRoom.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        GameRoom created = gameRoomService.createRoom("Sala1", 4, "Host");

        assertNotNull(created);
        assertTrue(created.getPlayers().size() == 1);
        assertTrue(created.getPlayers().get(0).isHost());
        verify(userAccountRepository, times(1)).save(account);
    }

    // ------------------------------------------------------------
    // 2. No permite crear sala si está en otra partida iniciada
    // ------------------------------------------------------------

    @Test
    void testCreateRoom_BlockedIfInActiveGame() {
        GameRoom other = new GameRoom();
        other.setRoomCode("ZZZ111");
        other.setGameStarted(true);

        player.setGameRoom(other);

        when(userAccountRepository.findByPlayerName("Host"))
                .thenReturn(Optional.of(account));

        when(playerRepository.findByPlayerName("Host"))
                .thenReturn(Optional.of(player));

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                gameRoomService.createRoom("SalaX", 4, "Host")
        );

        assertTrue(ex.getMessage().contains("partida activa"));
    }

    // ------------------------------------------------------------
    // 4. Join a sala
    // ------------------------------------------------------------

    @Test
    void testJoinRoom_OK() {
        when(gameRoomRepository.findByRoomCode("ABC123"))
                .thenReturn(Optional.of(room));

        when(userAccountRepository.findByPlayerName("Host"))
                .thenReturn(Optional.of(account));

        when(playerRepository.findByPlayerName("Host"))
                .thenReturn(Optional.empty());

        gameRoomService.joinRoom("ABC123", "Host");

        assertEquals(1, room.getPlayers().size());
    }

    // ------------------------------------------------------------
    // 5. Join bloqueado por sesión activa
    // ------------------------------------------------------------

    @Test
    void testJoinRoom_BlockedByActiveSession() {
        account.setHasActiveSession(true);

        when(gameRoomRepository.findByRoomCode("ABC123"))
                .thenReturn(Optional.of(room));
        when(userAccountRepository.findByPlayerName("Host"))
                .thenReturn(Optional.of(account));

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                gameRoomService.joinRoom("ABC123", "Host")
        );

        assertTrue(ex.getMessage().contains("sesión activa"));
    }

    // ------------------------------------------------------------
    // 6. Reconexión permitida
    // ------------------------------------------------------------



    // ------------------------------------------------------------
    // 7. Start game
    // ------------------------------------------------------------

    @Test
    void testStartGame_OK() {
        room.getPlayers().add(player);

        when(gameRoomRepository.findByRoomCode("ABC123"))
                .thenReturn(Optional.of(room));

        when(playerRepository.findByPlayerName("Host"))
                .thenReturn(Optional.of(player));

        when(gameRoomRepository.saveAndFlush(any()))
                .thenAnswer(inv -> inv.getArgument(0));

        GameRoom started = gameRoomService.startGame("ABC123");

        assertTrue(started.isGameStarted());
        verify(experienceService).resetRoomXp("ABC123");
        verify(gamePlayServices).spawnPlayers("ABC123");
    }

    // ------------------------------------------------------------
    // 8. End game
    // ------------------------------------------------------------

    @Test
    void testEndGame() {
        room.getPlayers().add(player);

        account.startSession(); // activar sesión

        when(gameRoomRepository.findByRoomCode("ABC123"))
                .thenReturn(Optional.of(room));

        gameRoomService.endGame("ABC123");

        assertFalse(room.isGameStarted());
        assertFalse(account.isHasActiveSession());
    }

    // ------------------------------------------------------------
    // 9. Leave room
    // ------------------------------------------------------------



    // ------------------------------------------------------------
    // 10. Delete room
    // ------------------------------------------------------------

    @Test
    void testDeleteRoom() {
        room.getPlayers().add(player);

        when(gameRoomRepository.findByRoomCode("ABC123")).thenReturn(Optional.of(room));

        gameRoomService.deleteRoom("ABC123");

        assertNull(player.getGameRoom());
        verify(gameRoomRepository).delete(room);
    }


    // ------------------------------------------------------------
// 9b. Leave room bloqueado si partida inició
// ------------------------------------------------------------
    @Test
    void testLeaveRoom_BlockedIfGameStarted() {
        room.setGameStarted(true);
        room.getPlayers().add(player);
        player.setGameRoom(room);

        when(gameRoomRepository.findByRoomCode("ABC123")).thenReturn(Optional.of(room));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> gameRoomService.leaveRoom("ABC123", "Host"));

        assertTrue(ex.getMessage().contains("No puedes salir de una partida"));
    }


}
