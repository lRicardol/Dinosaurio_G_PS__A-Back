package com.dinosurio_G.Back.websocket;

import com.dinosurio_G.Back.dto.GameRoomDTO;
import com.dinosurio_G.Back.dto.GameRoomMapper;
import com.dinosurio_G.Back.service.GamePlayServices;
import com.dinosurio_G.Back.service.GameRoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

// Mensajes del front se envían a /app/game/{roomCode}/input
// Notificaciones a clientes: /topic/game/{roomCode}
@Controller
public class GameWebSocketController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private GamePlayServices gamePlayServices;

    @Autowired
    private GameRoomService gameRoomService;

    // DTO MovementMessage
    @MessageMapping("/game/{roomCode}/input")
    public void receiveInput(@DestinationVariable String roomCode, MovementMessage msg) {
        // Actualiza input en servicio (no persiste posición aquí; loop tick se encarga)
        gamePlayServices.updatePlayerInput(roomCode, msg.getPlayerName(),
                msg.isArriba(), msg.isAbajo(), msg.isIzquierda(), msg.isDerecha());

        // Opcional: reenviamos el input a todos (útil para debug / sincronizar)
        messagingTemplate.convertAndSend("/topic/game/" + roomCode + "/input", msg);
    }

    // Método utilitario: enviar estado completo de la sala a todos los clientes
    public void broadcastRoomState(String roomCode) {
        try {
            var room = gameRoomService.getRoomByCode(roomCode);
            GameRoomDTO dto = GameRoomMapper.toDTO(room);
            messagingTemplate.convertAndSend("/topic/game/" + roomCode + "/state", dto);
        } catch (Exception e) {
            System.err.println("Error broadcasting room state: " + e.getMessage());
        }
    }

    // Exponer un helper para notificar eventos concretos (XP/progreso/ganador/perdedor)
    public void broadcastEvent(String roomCode, Object payload, String channelSuffix) {
        messagingTemplate.convertAndSend("/topic/game/" + roomCode + "/" + channelSuffix, payload);
    }
}
