package com.dinosurio_G.Back.exception;

/**
 * Excepción lanzada cuando hay un conflicto de estado en el juego
 * (Ej: usuario ya en partida, sesión activa en otro dispositivo)
 * Se mapea a HTTP 409 Conflict
 */
public class GameConflictException extends RuntimeException {

    public GameConflictException(String message) {
        super(message);
    }

    public GameConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
