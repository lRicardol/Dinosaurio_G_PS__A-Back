package com.dinosurio_G.Back.repository;

import com.dinosurio_G.Back.model.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

    /**
     * Buscar usuario por su ID de Azure AD (sub claim del token)
     */
    Optional<UserAccount> findByAzureUserId(String azureUserId);

    /**
     * Buscar usuario por email
     */
    Optional<UserAccount> findByEmail(String email);

    /**
     * Buscar usuario por nombre de jugador
     */
    Optional<UserAccount> findByPlayerName(String playerName);

    /**
     * Verificar si existe un email
     */
    boolean existsByEmail(String email);

    /**
     * Verificar si existe un nombre de jugador
     */
    boolean existsByPlayerName(String playerName);
}
