package com.dinosurio_G.Back;

import com.dinosurio_G.Back.model.UserAccount;
import com.dinosurio_G.Back.repository.UserAccountRepository;
import com.dinosurio_G.Back.service.AuthenticationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
public class AuthenticationServiceTest {
    @Mock
    private UserAccountRepository userAccountRepository;

    @InjectMocks
    private AuthenticationService authenticationService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    // ---------------------- getOrCreateUser ----------------------

    @Test
    void testGetOrCreateUser_UserExists() {
        String azureUserId = "123";
        String email = "user@example.com";
        String name = "PlayerX";

        UserAccount existingUser = new UserAccount(azureUserId, email, "OldPlayer");
        when(userAccountRepository.findByAzureUserId(azureUserId))
                .thenReturn(Optional.of(existingUser));
        when(userAccountRepository.save(existingUser)).thenReturn(existingUser);

        UserAccount result = authenticationService.getOrCreateUser(azureUserId, email, name);

        assertNotNull(result);
        assertEquals(existingUser, result);
        verify(userAccountRepository).save(existingUser);
    }

    @Test
    void testGetOrCreateUser_UserDoesNotExist() {
        String azureUserId = "123";
        String email = "user@example.com";
        String name = "PlayerX";

        when(userAccountRepository.findByAzureUserId(azureUserId))
                .thenReturn(Optional.empty());

        UserAccount result = authenticationService.getOrCreateUser(azureUserId, email, name);

        assertNull(result);
    }

    // ---------------------- registerUser ----------------------

    @Test
    void testRegisterUser_Success() {
        String azureUserId = "123";
        String email = "user@example.com";
        String playerName = "PlayerX";

        when(userAccountRepository.existsByPlayerName(playerName)).thenReturn(false);
        when(userAccountRepository.existsByEmail(email)).thenReturn(false);

        UserAccount newUser = new UserAccount(azureUserId, email, playerName);
        when(userAccountRepository.save(any(UserAccount.class))).thenReturn(newUser);

        UserAccount result = authenticationService.registerUser(azureUserId, email, playerName);

        assertNotNull(result);
        assertEquals(azureUserId, result.getAzureUserId());
        assertEquals(email, result.getEmail());
        assertEquals(playerName, result.getPlayerName());
    }

    @Test
    void testRegisterUser_PlayerNameExists() {
        String azureUserId = "123";
        String email = "user@example.com";
        String playerName = "PlayerX";

        when(userAccountRepository.existsByPlayerName(playerName)).thenReturn(true);

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                authenticationService.registerUser(azureUserId, email, playerName)
        );

        assertEquals("El nombre de jugador ya está en uso", exception.getMessage());
    }

    @Test
    void testRegisterUser_EmailExists() {
        String azureUserId = "123";
        String email = "user@example.com";
        String playerName = "PlayerX";

        when(userAccountRepository.existsByPlayerName(playerName)).thenReturn(false);
        when(userAccountRepository.existsByEmail(email)).thenReturn(true);

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                authenticationService.registerUser(azureUserId, email, playerName)
        );

        assertEquals("El email ya está registrado", exception.getMessage());
    }

    // ---------------------- getUserByPlayerName ----------------------

    @Test
    void testGetUserByPlayerName() {
        String playerName = "PlayerX";
        UserAccount user = new UserAccount("123", "user@example.com", playerName);

        when(userAccountRepository.findByPlayerName(playerName))
                .thenReturn(Optional.of(user));

        Optional<UserAccount> result = authenticationService.getUserByPlayerName(playerName);

        assertTrue(result.isPresent());
        assertEquals(playerName, result.get().getPlayerName());
    }

    // ---------------------- getUserByAzureId ----------------------

    @Test
    void testGetUserByAzureId() {
        String azureUserId = "123";
        UserAccount user = new UserAccount(azureUserId, "user@example.com", "PlayerX");

        when(userAccountRepository.findByAzureUserId(azureUserId))
                .thenReturn(Optional.of(user));

        Optional<UserAccount> result = authenticationService.getUserByAzureId(azureUserId);

        assertTrue(result.isPresent());
        assertEquals(azureUserId, result.get().getAzureUserId());
    }

    // ---------------------- isPlayerNameAvailable ----------------------

    @Test
    void testIsPlayerNameAvailable_True() {
        String playerName = "PlayerX";
        when(userAccountRepository.existsByPlayerName(playerName)).thenReturn(false);

        boolean available = authenticationService.isPlayerNameAvailable(playerName);

        assertTrue(available);
    }

    @Test
    void testIsPlayerNameAvailable_False() {
        String playerName = "PlayerX";
        when(userAccountRepository.existsByPlayerName(playerName)).thenReturn(true);

        boolean available = authenticationService.isPlayerNameAvailable(playerName);

        assertFalse(available);
    }
}
