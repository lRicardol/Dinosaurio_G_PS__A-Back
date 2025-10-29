package com.dinosurio_G.Back.service;

import com.dinosurio_G.Back.model.NPC;
import com.dinosurio_G.Back.model.Player;
import com.dinosurio_G.Back.model.Position;
import com.dinosurio_G.Back.model.GameRoom;
import com.dinosurio_G.Back.repository.GameRoomRepository;
import com.dinosurio_G.Back.repository.PlayerRepository;
import com.dinosurio_G.Back.service.core.LockManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Service
public class NPCManager {

    // Configuración de NPCs
    private static final int NPC_INITIAL = 6;
    private static final int NPC_BATCH = 5;
    private static final int NPC_MAX = 20;
    private static final int NPC_HEALTH = 50;
    private static final double NPC_SPEED = 2.0;
    private static final int NPC_DAMAGE = 10;
    private static final double MELEE_RANGE = 10.0;
    private static final int XP_PER_KILL = 50;

    // Distancia de spawn aumentada y delay de inicio
    private static final double MIN_DIST_FROM_PLAYERS = 250.0; // Aumentado de 140 a 250
    private static final long GAME_START_GRACE_PERIOD_MS = 3000; // 3 segundos de gracia

    private final Map<String, CopyOnWriteArrayList<NPC>> npcsByRoom = new ConcurrentHashMap<>();
    private final Map<String, Long> gameStartTimes = new ConcurrentHashMap<>();
    private final Random random = new Random();

    private final GameRoomRepository gameRoomRepository;
    private final PlayerRepository playerRepository;
    private final LockManager lockManager;
    private final ExperienceService experienceService;

    @Autowired
    public NPCManager(GameRoomRepository gameRoomRepository,
                      PlayerRepository playerRepository,
                      ExperienceService experienceService,
                      LockManager lockManager) {
        this.gameRoomRepository = gameRoomRepository;
        this.playerRepository = playerRepository;
        this.lockManager = lockManager;
        this.experienceService = experienceService;
    }

    public List<NPC> getNpcsForRoom(String roomCode) {
        return npcsByRoom.computeIfAbsent(roomCode, k -> new CopyOnWriteArrayList<>());
    }

    public void spawnInitialNpcs(String roomCode) {
        GameRoom room = gameRoomRepository.findByRoomCode(roomCode).orElse(null);
        if (room == null || room.getMap() == null) return;

        CopyOnWriteArrayList<NPC> list = getMutableList(roomCode);

        // VERIFICAR SI YA HAY NPCs SPAWNEADOS
        if (!list.isEmpty()) {
            System.out.println("Ya hay " + list.size() + " NPCs en la sala " + roomCode);
            return;
        }

        // Registrar el tiempo de inicio del juego
        gameStartTimes.put(roomCode, System.currentTimeMillis());
        System.out.println("Período de gracia iniciado para sala " + roomCode);

        synchronized (list) {
            int toSpawn = Math.min(NPC_INITIAL, NPC_MAX - list.size());
            int attemptsLimit = 50;

            for (int i = 0; i < toSpawn; i++) {
                Position pos = null;
                int attempts = 0;
                while (attempts++ < attemptsLimit) {
                    Position candidate = room.getMap().randomValidPosition(random);
                    if (isFarFromPlayers(candidate, room)) {
                        pos = candidate;
                        break;
                    }
                }

                if (pos == null) {
                    pos = getEdgePosition(room.getMap());
                }

                NPC npc = new NPC(pos.getX(), pos.getY(), NPC_HEALTH, NPC_SPEED);
                list.add(npc);
                System.out.println("NPC " + (i+1) + "/" + toSpawn + " spawneado en (" +
                        (int)pos.getX() + ", " + (int)pos.getY() + ")");
            }
        }
        System.out.println("Total NPCs en sala " + roomCode + ": " + list.size());
    }

    // Método para spawnear en los bordes del mapa (zona segura)
    private Position getEdgePosition(com.dinosurio_G.Back.model.GameMap map) {
        int edge = random.nextInt(4); // 0=arriba, 1=derecha, 2=abajo, 3=izquierda
        double x, y;

        switch (edge) {
            case 0: // Arriba
                x = random.nextDouble() * map.getWidth();
                y = 50;
                break;
            case 1: // Derecha
                x = map.getWidth() - 50;
                y = random.nextDouble() * map.getHeight();
                break;
            case 2: // Abajo
                x = random.nextDouble() * map.getWidth();
                y = map.getHeight() - 50;
                break;
            default: // Izquierda
                x = 50;
                y = random.nextDouble() * map.getHeight();
                break;
        }

        return new Position(x, y);
    }

    public void trySpawnBatch(String roomCode) {
        CopyOnWriteArrayList<NPC> list = getMutableList(roomCode);
        GameRoom room = gameRoomRepository.findByRoomCode(roomCode).orElse(null);
        if (room == null || room.getMap() == null) return;

        synchronized (list) {
            int space = NPC_MAX - list.size();
            if (space <= 0) return;
            int toSpawn = Math.min(NPC_BATCH, space);
            int attemptsLimit = 50;

            for (int i = 0; i < toSpawn; i++) {
                Position pos = null;
                int attempts = 0;
                while (attempts++ < attemptsLimit) {
                    Position candidate = room.getMap().randomValidPosition(random);
                    if (isFarFromPlayers(candidate, room)) {
                        pos = candidate;
                        break;
                    }
                }
                if (pos == null) pos = getEdgePosition(room.getMap());

                NPC npc = new NPC(pos.getX(), pos.getY(), NPC_HEALTH, NPC_SPEED);
                list.add(npc);
            }
        }
    }

    private boolean isFarFromPlayers(Position p, GameRoom room) {
        double px = p.getX();
        double py = p.getY();
        for (Player player : room.getPlayers()) {
            double dx = player.getX() - px;
            double dy = player.getY() - py;
            double d = Math.sqrt(dx*dx + dy*dy);
            if (d < MIN_DIST_FROM_PLAYERS) return false;
        }
        return true;
    }

    private CopyOnWriteArrayList<NPC> getMutableList(String roomCode) {
        return npcsByRoom.computeIfAbsent(roomCode, k -> new CopyOnWriteArrayList<>());
    }

    @Transactional
    @Scheduled(fixedRate = 50)
    public void updateAllRoomsNpcs() {
        List<String> roomCodes = new ArrayList<>(npcsByRoom.keySet());

        for (String roomCode : roomCodes) {
            GameRoom room = gameRoomRepository.findByRoomCode(roomCode).orElse(null);
            if (room == null) {
                npcsByRoom.remove(roomCode);
                gameStartTimes.remove(roomCode);
                continue;
            }
            if (!room.isGameStarted()) continue;

            // Verificar período de gracia
            Long startTime = gameStartTimes.get(roomCode);
            if (startTime != null) {
                long elapsedTime = System.currentTimeMillis() - startTime;
                if (elapsedTime < GAME_START_GRACE_PERIOD_MS) {
                    // Durante el período de gracia, solo mover NPCs, NO atacar
                    updateNpcsMovementOnly(roomCode, room);
                    continue;
                }
            }

            // Después del período de gracia, comportamiento normal
            CopyOnWriteArrayList<NPC> list = getMutableList(roomCode);
            if (list.isEmpty()) continue;

            List<Player> players = room.getPlayers().stream()
                    .filter(Player::isAlive)
                    .collect(Collectors.toList());

            for (NPC npc : list) {
                if (npc.isDead()) continue;
                Player target = findNearestPlayer(npc, players);
                if (target != null) {
                    npc.moveTowards(target.getX(), target.getY());
                    boolean attacked = npc.tryAttack(target, NPC_DAMAGE, MELEE_RANGE);
                    if (attacked) {
                        playerRepository.save(target);
                        System.out.println("⚔️ NPC atacó a " + target.getPlayerName() +
                                " - Vida restante: " + target.getHealth());
                    }
                }
            }

            // Limpiar NPCs muertos y dar XP
            List<NPC> toRemove = new ArrayList<>();
            for (NPC npc : list) {
                if (npc.isDead()) {
                    String killer = npc.getLastHitBy();
                    if (npc.markXpAwarded() && killer != null) {
                        lockManager.withLock("room_" + roomCode, () -> {
                            experienceService.addExperience(roomCode, XP_PER_KILL);
                            System.out.println(" " + killer + " ganó " + XP_PER_KILL + " XP");
                        });
                    }
                    toRemove.add(npc);
                }
            }
            if (!toRemove.isEmpty()) {
                list.removeAll(toRemove);
            }

            // Respawnear NPCs si hay pocos
            if (list.size() < NPC_INITIAL) {
                trySpawnBatch(roomCode);
            }
        }
    }

    // Método para mover NPCs sin atacar (período de gracia)
    private void updateNpcsMovementOnly(String roomCode, GameRoom room) {
        CopyOnWriteArrayList<NPC> list = getMutableList(roomCode);
        if (list.isEmpty()) return;

        List<Player> players = room.getPlayers().stream()
                .filter(Player::isAlive)
                .collect(Collectors.toList());

        for (NPC npc : list) {
            if (npc.isDead()) continue;
            Player target = findNearestPlayer(npc, players);
            if (target != null) {
                // Solo mover, NO atacar
                npc.moveTowards(target.getX(), target.getY());
            }
        }
    }

    private Player findNearestPlayer(NPC npc, List<Player> players) {
        Player best = null;
        double bestDist = Double.MAX_VALUE;
        for (Player p : players) {
            double dx = p.getX() - npc.getX();
            double dy = p.getY() - npc.getY();
            double d = Math.sqrt(dx*dx + dy*dy);
            if (d < bestDist) {
                bestDist = d;
                best = p;
            }
        }
        return best;
    }

    // Método para limpiar sala cuando termina el juego
    public void cleanupRoom(String roomCode) {
        npcsByRoom.remove(roomCode);
        gameStartTimes.remove(roomCode);
        System.out.println("Sala " + roomCode + " limpiada");
    }
}