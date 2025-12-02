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

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
public class GameRoomServiceTest {

    @Mock
    private GameRoomRepository gameRoomRepository;
    @Mock
    private PlayerRepository playerRepository;
    @Mock
    private UserAccountRepository userAccountRepository;
    @Mock
    private GameMapService gameMapService;
    @Mock
    private SimpMessagingTemplate messagingTemplate;
    @Mock
    private GamePlayServices gamePlayServices;
    @Mock
    private ExperienceService experienceService;

    @InjectMocks
    private GameRoomService gameRoomService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    // ---------------------- createRoom ----------------------
    @Test
    void testCreateRoom_UserNotRegistered() {
        String playerName = "PlayerX";
        when(userAccountRepository.findByPlayerName(playerName)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> gameRoomService.createRoom("Sala1", 4, playerName));

        assertEquals("El jugador PlayerX no está registrado", ex.getMessage());
    }

    @Test
    void testCreateRoom_Success() {
        String playerName = "PlayerX";
        UserAccount hostAccount = new UserAccount();
        hostAccount.setPlayerName(playerName);

        when(userAccountRepository.findByPlayerName(playerName)).thenReturn(Optional.of(hostAccount));
        when(playerRepository.findByPlayerName(playerName)).thenReturn(Optional.empty());
        when(gameMapService.createMapForRoom()).thenReturn(new GameMap());
        when(gameRoomRepository.save(any(GameRoom.class))).thenAnswer(i -> i.getArgument(0));
        when(playerRepository.save(any(Player.class))).thenAnswer(i -> i.getArgument(0));

        GameRoom room = gameRoomService.createRoom("Sala1", 4, playerName);

        assertNotNull(room);
        assertEquals("Sala1", room.getRoomName());
        assertEquals(1, room.getPlayers().size());
        assertTrue(room.getPlayers().get(0).isHost());
    }

    // ---------------------- getAllRooms ----------------------
    @Test
    void testGetAllRooms() {
        when(gameRoomRepository.findAll()).thenReturn(List.of(new GameRoom(), new GameRoom()));

        List<GameRoom> rooms = gameRoomService.getAllRooms();

        assertEquals(2, rooms.size());
    }

    // ---------------------- getRoomByCode ----------------------
    @Test
    void testGetRoomByCode_NotFound() {
        when(gameRoomRepository.findByRoomCode("ABC123")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> gameRoomService.getRoomByCode("ABC123"));

        assertEquals("La sala con código ABC123 no existe", ex.getMessage());
    }

    @Test
    void testGetRoomByCode_Success() {
        GameRoom room = new GameRoom();
        when(gameRoomRepository.findByRoomCode("ABC123")).thenReturn(Optional.of(room));

        GameRoom result = gameRoomService.getRoomByCode("ABC123");

        assertEquals(room, result);
    }

    // ---------------------- startGame ----------------------
    @Test
    void testStartGame() {
        String code = "ABC123";
        GameRoom room = new GameRoom();
        room.setRoomCode(code);
        room.setPlayers(List.of());

        when(gameRoomRepository.findByRoomCode(code)).thenReturn(Optional.of(room));
        when(gameRoomRepository.saveAndFlush(room)).thenReturn(room);

        GameRoom result = gameRoomService.startGame(code);

        verify(experienceService).resetRoomXp(code);
        verify(gamePlayServices).spawnPlayers(code);
        verify(messagingTemplate, times(2)).convertAndSend(anyString(), (Object) any());
        assertTrue(result.isGameStarted());
    }
}
