package com.dinosurio_G.Back;

import com.dinosurio_G.Back.model.GameMap;
import com.dinosurio_G.Back.model.GameRoom;
import com.dinosurio_G.Back.model.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
public class PlayerTest {
    private Player player;
    private GameMap map;
    private GameRoom room;

    @BeforeEach
    void setUp() {
        player = new Player("PlayerX", true, 50, 50);
        map = new GameMap();
        map.setWidth(100);
        map.setHeight(100);
        room = new GameRoom();
        room.setMap(map);
        player.setGameRoom(room);
    }

    // -------------------- Movimiento --------------------
    @Test
    void testActualizar_MovimientoArriba() {
        player.setInput(true, false, false, false);
        player.actualizar();
        assertTrue(player.getY() < 50);
    }

    @Test
    void testActualizar_MovimientoAbajo() {
        player.setInput(false, true, false, false);
        player.actualizar();
        assertTrue(player.getY() > 50);
    }

    @Test
    void testActualizar_MovimientoIzquierda() {
        player.setInput(false, false, true, false);
        player.actualizar();
        assertTrue(player.getX() < 50);
        assertFalse(player.isFacingRight());
    }

    @Test
    void testActualizar_MovimientoDerecha() {
        player.setInput(false, false, false, true);
        player.actualizar();
        assertTrue(player.getX() > 50);
        assertTrue(player.isFacingRight());
    }

    @Test
    void testActualizar_LimitesMapa() {
        player.setX(0);
        player.setY(0);
        player.setInput(true, false, true, false); // intenta salir del mapa
        player.actualizar();
        assertTrue(player.getX() >= 0);
        assertTrue(player.getY() >= 0);
    }

    // -------------------- Ataque --------------------
    @Test
    void testCanAttack_Cooldown() {
        assertTrue(player.canAttack()); // primera vez
        assertFalse(player.canAttack()); // inmediatamente después
    }

    // -------------------- Recibir daño --------------------
    @Test
    void testReceiveDamage_Normal() {
        player.receiveDamage(20);
        assertEquals(Player.DEFAULT_HEALTH - 20, player.getHealth());
        assertTrue(player.isAlive());
    }

    @Test
    void testReceiveDamage_Muerte() {
        player.receiveDamage(200);
        assertEquals(0, player.getHealth());
        assertFalse(player.isAlive());
    }

    @Test
    void testReceiveDamage_JugadorMuerto() {
        player.receiveDamage(200); // mata
        int healthBefore = player.getHealth();
        player.receiveDamage(50); // no debería cambiar
        assertEquals(healthBefore, player.getHealth());
    }

    @Test
    void testReceiveDamage_DanoNegativo() {
        int healthBefore = player.getHealth();
        player.receiveDamage(-10); // ignora
        assertEquals(healthBefore, player.getHealth());
    }

    // -------------------- Getters/Setters --------------------
    @Test
    void testGettersSetters() {
        player.setX(10); assertEquals(10, player.getX());
        player.setY(20); assertEquals(20, player.getY());
        player.setSpeed(3.5); assertEquals(3.5, player.getSpeed());
        player.setHealth(80); assertEquals(80, player.getHealth());
        player.setReady(true); assertTrue(player.isReady());
        player.setHost(false); assertFalse(player.isHost());
        player.setPlayerName("NewName"); assertEquals("NewName", player.getPlayerName());
    }
}
