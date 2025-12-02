package com.dinosurio_G.Back.controller;

import com.dinosurio_G.Back.model.UserAccount;
import com.dinosurio_G.Back.service.AuthenticationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class AuthControllerTest {

    @Mock
    private AuthenticationService authenticationService;

    @InjectMocks
    private AuthController authController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    // ---------------------- /api/auth/me ----------------------

    @Test
    void testGetCurrentUser_NotAuthenticated() {
        ResponseEntity<Map<String, Object>> response = authController.getCurrentUser(null, null);
        Map<String, Object> body = response.getBody();

        assertNotNull(body);
        assertFalse((Boolean) body.get("isAuthenticated"));
        assertFalse((Boolean) body.get("isRegistered"));
        assertEquals("Usuario no autenticado", body.get("message"));
    }

    @Test
    void testGetCurrentUser_AuthenticatedButNotRegistered() {
        String azureUserId = "123";
        String email = "user@example.com";

        when(authenticationService.getOrCreateUser(azureUserId, email, email)).thenReturn(null);

        Map<String, Object> body = authController.getCurrentUser(azureUserId, email).getBody();

        assertNotNull(body);
        assertTrue((Boolean) body.get("isAuthenticated"));
        assertFalse((Boolean) body.get("isRegistered"));
        assertTrue((Boolean) body.get("needsRegistration"));
        assertEquals(azureUserId, body.get("azureUserId"));
        assertEquals(email, body.get("email"));
    }

    @Test
    void testGetCurrentUser_AuthenticatedAndRegistered() {
        String azureUserId = "123";
        String email = "user@example.com";

        UserAccount user = new UserAccount();
        user.setId(1L);
        user.setEmail(email);
        user.setPlayerName("PlayerX");
        user.setDisplayName("Player X Display");

        when(authenticationService.getOrCreateUser(azureUserId, email, email)).thenReturn(user);

        Map<String, Object> body = authController.getCurrentUser(azureUserId, email).getBody();

        assertNotNull(body);
        assertTrue((Boolean) body.get("isAuthenticated"));
        assertTrue((Boolean) body.get("isRegistered"));
        assertEquals(user.getId(), body.get("userId"));
        assertEquals(user.getEmail(), body.get("email"));
        assertEquals(user.getPlayerName(), body.get("playerName"));
        assertEquals(user.getDisplayName(), body.get("displayName"));
    }

    // ---------------------- /api/auth/register ----------------------



    @Test
    void testCompleteRegistration_MissingPlayerName() {
        String azureUserId = "123";
        String email = "user@example.com";

        Map<String, String> payload = Map.of("playerName", "   "); // solo espacios
        ResponseEntity<Map<String, Object>> response = authController.completeRegistration(azureUserId, email, payload);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("El nombre de jugador es requerido", response.getBody().get("error"));
    }

    @Test
    void testCompleteRegistration_Success() {
        String azureUserId = "123";
        String email = "user@example.com";
        String playerName = "PlayerX";

        UserAccount user = new UserAccount();
        user.setId(1L);
        user.setEmail(email);
        user.setPlayerName(playerName);

        when(authenticationService.registerUser(azureUserId, email, playerName)).thenReturn(user);

        Map<String, String> payload = Map.of("playerName", playerName);
        ResponseEntity<Map<String, Object>> response = authController.completeRegistration(azureUserId, email, payload);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertTrue((Boolean) body.get("success"));
        assertEquals(user.getId(), body.get("userId"));
        assertEquals(user.getEmail(), body.get("email"));
        assertEquals(user.getPlayerName(), body.get("playerName"));
    }

    @Test
    void testCompleteRegistration_Conflict() {
        String azureUserId = "123";
        String email = "user@example.com";
        String playerName = "PlayerX";

        when(authenticationService.registerUser(azureUserId, email, playerName))
                .thenThrow(new RuntimeException("Nombre de jugador ya existe"));

        Map<String, String> payload = Map.of("playerName", playerName);
        ResponseEntity<Map<String, Object>> response = authController.completeRegistration(azureUserId, email, payload);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("Nombre de jugador ya existe", response.getBody().get("error"));
    }

    // ---------------------- /api/auth/check-playername ----------------------

    @Test
    void testCheckPlayerName_Available() {
        String playerName = "PlayerX";
        when(authenticationService.isPlayerNameAvailable(playerName)).thenReturn(true);

        Map<String, Object> body = authController.checkPlayerName(playerName).getBody();
        assertNotNull(body);
        assertEquals(playerName, body.get("playerName"));
        assertTrue((Boolean) body.get("available"));
    }

    @Test
    void testCheckPlayerName_NotAvailable() {
        String playerName = "PlayerX";
        when(authenticationService.isPlayerNameAvailable(playerName)).thenReturn(false);

        Map<String, Object> body = authController.checkPlayerName(playerName).getBody();
        assertNotNull(body);
        assertEquals(playerName, body.get("playerName"));
        assertFalse((Boolean) body.get("available"));
    }

    // ---------------------- /api/auth/urls ----------------------

    @Test
    void testGetAuthUrls() {
        Map<String, String> body = authController.getAuthUrls().getBody();
        assertNotNull(body);
        assertEquals("/.auth/login/aad", body.get("login"));
        assertEquals("/.auth/logout", body.get("logout"));
        assertEquals("/.auth/me", body.get("userInfo"));
    }
}
