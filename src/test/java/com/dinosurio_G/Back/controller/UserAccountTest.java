package com.dinosurio_G.Back.controller;
import com.dinosurio_G.Back.model.UserAccount;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
public class UserAccountTest {
    @Test
    void testDefaultConstructor() {
        UserAccount user = new UserAccount();
        assertNotNull(user.getCreatedAt());
        assertNotNull(user.getLastLoginAt());
        assertTrue(user.isActive());
    }

    @Test
    void testParameterizedConstructor() {
        UserAccount user = new UserAccount("azure123", "test@example.com", "PlayerX");
        assertEquals("azure123", user.getAzureUserId());
        assertEquals("test@example.com", user.getEmail());
        assertEquals("PlayerX", user.getPlayerName());
        assertEquals("PlayerX", user.getDisplayName());
        assertTrue(user.isActive());
    }

    @Test
    void testUpdateLastLogin() {
        UserAccount user = new UserAccount();
        LocalDateTime beforeUpdate = user.getLastLoginAt();
        user.updateLastLogin();
        assertTrue(user.getLastLoginAt().isAfter(beforeUpdate) ||
                user.getLastLoginAt().isEqual(beforeUpdate));
    }

    @Test
    void testSettersAndGetters() {
        UserAccount user = new UserAccount();
        user.setId(1L);
        user.setAzureUserId("azure456");
        user.setEmail("new@example.com");
        user.setPlayerName("PlayerY");
        user.setDisplayName("DisplayY");
        LocalDateTime now = LocalDateTime.now();
        user.setCreatedAt(now);
        user.setLastLoginAt(now);
        user.setActive(false);

        assertEquals(1L, user.getId());
        assertEquals("azure456", user.getAzureUserId());
        assertEquals("new@example.com", user.getEmail());
        assertEquals("PlayerY", user.getPlayerName());
        assertEquals("DisplayY", user.getDisplayName());
        assertEquals(now, user.getCreatedAt());
        assertEquals(now, user.getLastLoginAt());
        assertFalse(user.isActive());
    }

    @Test
    void testToString() {
        UserAccount user = new UserAccount("azure789", "test2@example.com", "PlayerZ");
        String str = user.toString();
        assertTrue(str.contains("azure789"));
        assertTrue(str.contains("test2@example.com"));
        assertTrue(str.contains("PlayerZ"));
    }
}
