package com.dinosurio_G.Back.controller;

import com.dinosurio_G.Back.model.UserAccount;
import com.dinosurio_G.Back.service.AuthenticationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controlador simplificado para Azure EasyAuth
 * Azure inyecta headers automáticamente cuando un usuario está autenticado
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthenticationService authenticationService;

    /**
     * Obtener información del usuario autenticado
     * Azure EasyAuth inyecta estos headers automáticamente:
     * - X-MS-CLIENT-PRINCIPAL-ID: El ID único del usuario
     * - X-MS-CLIENT-PRINCIPAL-NAME: El email del usuario
     */
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser(
            @RequestHeader(value = "X-MS-CLIENT-PRINCIPAL-ID", required = false) String azureUserId,
            @RequestHeader(value = "X-MS-CLIENT-PRINCIPAL-NAME", required = false) String email) {

        Map<String, Object> response = new HashMap<>();

        // Si no hay headers, el usuario no está autenticado
        if (azureUserId == null || email == null) {
            response.put("isAuthenticated", false);
            response.put("isRegistered", false);
            response.put("message", "Usuario no autenticado");
            return ResponseEntity.ok(response);
        }

        // Usuario autenticado - verificar si está registrado
        UserAccount user = authenticationService.getOrCreateUser(azureUserId, email, email);

        if (user == null) {
            // Autenticado pero no registrado (primera vez)
            response.put("isAuthenticated", true);
            response.put("isRegistered", false);
            response.put("azureUserId", azureUserId);
            response.put("email", email);
            response.put("needsRegistration", true);
            response.put("message", "Usuario autenticado pero debe elegir nombre de jugador");
        } else {
            // Completamente registrado
            response.put("isAuthenticated", true);
            response.put("isRegistered", true);
            response.put("userId", user.getId());
            response.put("email", user.getEmail());
            response.put("playerName", user.getPlayerName());
            response.put("displayName", user.getDisplayName());
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Completar registro eligiendo nombre de jugador
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> completeRegistration(
            @RequestHeader(value = "X-MS-CLIENT-PRINCIPAL-ID", required = false) String azureUserId,
            @RequestHeader(value = "X-MS-CLIENT-PRINCIPAL-NAME", required = false) String email,
            @RequestBody Map<String, String> payload) {

        Map<String, Object> response = new HashMap<>();

        // Verificar autenticación
        if (azureUserId == null || email == null) {
            response.put("error", "Usuario no autenticado");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        String playerName = payload.get("playerName");

        if (playerName == null || playerName.trim().isEmpty()) {
            response.put("error", "El nombre de jugador es requerido");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            UserAccount user = authenticationService.registerUser(azureUserId, email, playerName.trim());

            response.put("success", true);
            response.put("userId", user.getId());
            response.put("email", user.getEmail());
            response.put("playerName", user.getPlayerName());
            response.put("message", "Registro completado exitosamente");

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }
    }

    /**
     * Verificar si un nombre de jugador está disponible
     */
    @GetMapping("/check-playername")
    public ResponseEntity<Map<String, Object>> checkPlayerName(@RequestParam String playerName) {
        boolean available = authenticationService.isPlayerNameAvailable(playerName);

        Map<String, Object> response = new HashMap<>();
        response.put("playerName", playerName);
        response.put("available", available);

        return ResponseEntity.ok(response);
    }

    /**
     * Información sobre las URLs de autenticación
     */
    @GetMapping("/urls")
    public ResponseEntity<Map<String, String>> getAuthUrls() {
        Map<String, String> urls = new HashMap<>();
        urls.put("login", "/.auth/login/aad");
        urls.put("logout", "/.auth/logout");
        urls.put("userInfo", "/.auth/me");
        return ResponseEntity.ok(urls);
    }
}