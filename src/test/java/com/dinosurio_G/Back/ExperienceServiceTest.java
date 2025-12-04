package com.dinosurio_G.Back;

import com.dinosurio_G.Back.service.ExperienceService;
import com.dinosurio_G.Back.service.GamePlayServices;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ExperienceServiceTest {

    @Mock private SimpMessagingTemplate messagingTemplate;
    @Mock private GamePlayServices gamePlayServices;

    @InjectMocks
    private ExperienceService xpService;

    private final String ROOM_CODE = "TEST123";

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    // ---------------------------------------
    // TEST: addExperience normal
    // ---------------------------------------
    @Test
    void testAddExperience_Normal() {
        xpService.addExperience(ROOM_CODE, 100);

        double progress = xpService.getProgress(ROOM_CODE);
        assertEquals(0.1, progress, 0.001);

        // Verificar que se notificó al frontend
        verify(messagingTemplate, times(1))
                .convertAndSend(eq("/topic/game/" + ROOM_CODE + "/xp"), any(Map.class));

        // No debe llamar a onGameWon todavía
        verify(gamePlayServices, never()).onGameWon(any());
    }

    // ---------------------------------------
    // TEST: addExperience alcanza objetivo
    // ---------------------------------------
    @Test
    void testAddExperience_ReachesGoal() {
        // Sumar 1000 XP de golpe
        xpService.addExperience(ROOM_CODE, 1000);

        // Progreso máximo
        assertEquals(1.0, xpService.getProgress(ROOM_CODE), 0.001);

        // onGameWon debe ser llamado
        verify(gamePlayServices, times(1)).onGameWon(ROOM_CODE);

        // Debe notificar al frontend al menos una vez
        verify(messagingTemplate, atLeastOnce())
                .convertAndSend(eq("/topic/game/" + ROOM_CODE + "/xp"), any(Map.class));
    }

    // ---------------------------------------
    // TEST: resetRoomXp
    // ---------------------------------------
    @Test
    void testResetRoomXp() {
        xpService.addExperience(ROOM_CODE, 500);

        assertEquals(0.5, xpService.getProgress(ROOM_CODE), 0.001);

        xpService.resetRoomXp(ROOM_CODE);

        assertEquals(0.0, xpService.getProgress(ROOM_CODE), 0.001);

    }

    // ---------------------------------------
    // TEST: getProgress sin XP previa
    // ---------------------------------------
    @Test
    void testGetProgress_NoXP() {
        assertEquals(0.0, xpService.getProgress("UNKNOWN"), 0.001);
    }
}
