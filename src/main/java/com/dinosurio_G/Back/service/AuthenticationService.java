package com.dinosurio_G.Back.service;

import com.dinosurio_G.Back.model.UserAccount;
import com.dinosurio_G.Back.repository.UserAccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Servicio para manejar la autenticación con Azure EasyAuth
 */
@Service
public class AuthenticationService {

    @Autowired
    private UserAccountRepository userAccountRepository;

    /**
     * Obtener o crear usuario basado en la información de Azure AD
     * Este método se llama cuando un usuario autenticado hace una petición
     */
    @Transactional
    public UserAccount getOrCreateUser(String azureUserId, String email, String name) {
        // Buscar usuario existente por Azure ID
        Optional<UserAccount> existingUser = userAccountRepository.findByAzureUserId(azureUserId);

        if (existingUser.isPresent()) {
            // Usuario existe, actualizar último login
            UserAccount user = existingUser.get();
            user.updateLastLogin();
            return userAccountRepository.save(user);
        }

        // Usuario nuevo: necesita registrarse
        // Retornamos null para indicar que debe completar el registro
        return null;
    }

    /**
     * Registrar un nuevo usuario con nombre de jugador
     */
    @Transactional
    public UserAccount registerUser(String azureUserId, String email, String playerName) {
        // Verificar que el nombre de jugador no existe
        if (userAccountRepository.existsByPlayerName(playerName)) {
            throw new RuntimeException("El nombre de jugador ya está en uso");
        }

        // Verificar que el email no existe
        if (userAccountRepository.existsByEmail(email)) {
            throw new RuntimeException("El email ya está registrado");
        }

        // Crear nueva cuenta
        UserAccount newAccount = new UserAccount(azureUserId, email, playerName);
        return userAccountRepository.save(newAccount);
    }

    /**
     * Obtener usuario por nombre de jugador
     */
    public Optional<UserAccount> getUserByPlayerName(String playerName) {
        return userAccountRepository.findByPlayerName(playerName);
    }

    /**
     * Obtener usuario por Azure ID
     */
    public Optional<UserAccount> getUserByAzureId(String azureUserId) {
        return userAccountRepository.findByAzureUserId(azureUserId);
    }

    /**
     * Verificar si un nombre de jugador está disponible
     */
    public boolean isPlayerNameAvailable(String playerName) {
        return !userAccountRepository.existsByPlayerName(playerName);
    }
}
