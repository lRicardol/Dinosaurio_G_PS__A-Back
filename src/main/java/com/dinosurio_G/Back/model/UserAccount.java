package com.dinosurio_G.Back.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entidad que representa una cuenta de usuario autenticada.
 * Se vincula con Azure AD a través del email/principalId.
 */
@Entity
@Table(name = "user_accounts")
public class UserAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Identificador único del usuario en Azure AD (sub claim)
     * Este es el ObjectId que viene en el token de Azure AD
     */
    @Column(unique = true, nullable = false)
    private String azureUserId;

    /**
     * Email del usuario (usado para login)
     */
    @Column(unique = true, nullable = false)
    private String email;

    /**
     * Nombre de jugador (único en el sistema)
     * Este es el nombre que se usa en las partidas
     */
    @Column(unique = true, nullable = false)
    private String playerName;

    /**
     * Nombre para mostrar (puede coincidir con playerName o ser diferente)
     */
    private String displayName;

    /**
     * Fecha de creación de la cuenta
     */
    private LocalDateTime createdAt;

    /**
     * Última vez que el usuario inició sesión
     */
    private LocalDateTime lastLoginAt;

    /**
     * Indica si la cuenta está activa
     */
    private boolean active = true;

    /**
     * Indica si el usuario tiene una sesión activa (está jugando)
     */
    private boolean hasActiveSession = false;

    /**
     * Timestamp de la última actividad (para detectar sesiones inactivas)
     */
    private LocalDateTime lastActivity;

    // Constructores
    public UserAccount() {
        this.createdAt = LocalDateTime.now();
        this.lastLoginAt = LocalDateTime.now();
    }

    public UserAccount(String azureUserId, String email, String playerName) {
        this();
        this.azureUserId = azureUserId;
        this.email = email;
        this.playerName = playerName;
        this.displayName = playerName;
    }

    // Método para actualizar último login
    public void updateLastLogin() {
        this.lastLoginAt = LocalDateTime.now();
        this.lastActivity = LocalDateTime.now();
    }

    // Método para actualizar última actividad
    public void updateActivity() {
        this.lastActivity = LocalDateTime.now();
    }

    // Método para activar sesión
    public void startSession() {
        this.hasActiveSession = true;
        this.lastActivity = LocalDateTime.now();
    }

    // Método para desactivar sesión
    public void endSession() {
        this.hasActiveSession = false;
    }

    // Getters y Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAzureUserId() {
        return azureUserId;
    }

    public void setAzureUserId(String azureUserId) {
        this.azureUserId = azureUserId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(LocalDateTime lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isHasActiveSession() {
        return hasActiveSession;
    }

    public void setHasActiveSession(boolean hasActiveSession) {
        this.hasActiveSession = hasActiveSession;
    }

    public LocalDateTime getLastActivity() {
        return lastActivity;
    }

    public void setLastActivity(LocalDateTime lastActivity) {
        this.lastActivity = lastActivity;
    }

    @Override
    public String toString() {
        return "UserAccount{" +
                "id=" + id +
                ", email='" + email + '\'' +
                ", playerName='" + playerName + '\'' +
                ", azureUserId='" + azureUserId + '\'' +
                '}';
    }
}