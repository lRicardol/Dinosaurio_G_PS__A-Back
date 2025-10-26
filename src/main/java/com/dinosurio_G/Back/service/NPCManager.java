package com.dinosurio_G.Back.service;

import com.dinosurio_G.Back.model.*;
import com.dinosurio_G.Back.service.core.LockManager;
import com.dinosurio_G.Back.repository.PlayerRepository;
import com.dinosurio_G.Back.repository.GameRoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;


import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;


@Service
public class NPCManager {


    // Config
    private static final int NPC_INITIAL = 10;
    private static final int NPC_BATCH = 5;
    private static final int NPC_MAX = 20;
    private static final int NPC_HEALTH = 50;
    private static final double NPC_SPEED = 5.0;
    private static final int NPC_DAMAGE = 10; // daño por ataque
    private static final double MELEE_RANGE = 10.0; // rango cuerpo a cuerpo
    private static final int XP_PER_KILL = 100;


    // NPCs por roomCode
    private final Map<String, CopyOnWriteArrayList<NPC>> npcsByRoom = new ConcurrentHashMap<>();
    private final Random random = new Random();


    private final GameRoomRepository gameRoomRepository;
    private final PlayerRepository playerRepository;
    private final GamePlayServices gamePlayServices;
    private final LockManager lockManager;


    @Autowired
    public NPCManager(GameRoomRepository gameRoomRepository,
                      PlayerRepository playerRepository,
                      GamePlayServices gamePlayServices,
                      LockManager lockManager) {
        this.gameRoomRepository = gameRoomRepository;
        this.playerRepository = playerRepository;
        this.gamePlayServices = gamePlayServices;
        this.lockManager = lockManager;
    }


    // Obtener lista segura (no null)
    public List<NPC> getNpcsForRoom(String roomCode) {
        return npcsByRoom.computeIfAbsent(roomCode, k -> new CopyOnWriteArrayList<>());
    }


    // Spawn inicial (llamar cuando se inicia la partida)
    public void spawnInitialNpcs(String roomCode) {
        GameRoom room = gameRoomRepository.findByRoomCode(roomCode).orElse(null);
        if (room == null) return;
        CopyOnWriteArrayList<NPC> list = getMutableList(roomCode);
        synchronized (list) {
            int toSpawn = Math.min(NPC_INITIAL, NPC_MAX - list.size());
            for (int i = 0; i < toSpawn; i++) {
                Position pos = room.getMap().randomValidPosition(random);
                NPC npc = new NPC(pos.getX(), pos.getY(), NPC_HEALTH, NPC_SPEED);
                list.add(npc);
            }
        }
    }

    // Forzar spawn de hasta NPC_BATCH respetando NPC_MAX
    public void trySpawnBatch(String roomCode) {
        CopyOnWriteArrayList<NPC> list = getMutableList(roomCode);
        GameRoom room = gameRoomRepository.findByRoomCode(roomCode).orElse(null);
        if (room == null) return;
        synchronized (list) {
            int space = NPC_MAX - list.size();
            if (space <= 0) return;
            int toSpawn = Math.min(NPC_BATCH, space);
            for (int i = 0; i < toSpawn; i++) {
                Position p = room.getMap().randomValidPosition(random);
                NPC npc = new NPC(p.getX(), p.getY(), NPC_HEALTH, NPC_SPEED);
                list.add(npc);
            }
        }
    }

    private CopyOnWriteArrayList<NPC> getMutableList(String roomCode) {
        return npcsByRoom.computeIfAbsent(roomCode, k -> new CopyOnWriteArrayList<>());
    }

    // Scheduler: cada 50ms actualizamos comportamiento de NPCs (movimiento + ataque)
    @Scheduled(fixedRate = 50)
    public void updateAllRoomsNpcs() {
        // Para cada sala con NPCs
        List<String> roomCodes = new ArrayList<>(npcsByRoom.keySet());
        for (String roomCode : roomCodes) {
            GameRoom room = gameRoomRepository.findByRoomCode(roomCode).orElse(null);
            if (room == null) {
                npcsByRoom.remove(roomCode);
                continue;
            }
            if (!room.isGameStarted()) continue;


            CopyOnWriteArrayList<NPC> list = getMutableList(roomCode);
            if (list.isEmpty()) continue;


            // Obtener jugadores vivos de la sala
            List<Player> players = room.getPlayers().stream()
                    .filter(Player::isAlive)
                    .collect(Collectors.toList());

            // Para cada NPC: elegir objetivo, mover, intentar atacar
            for (NPC npc : list) {
                if (npc.isDead()) continue;
                // elegir jugador más cercano
                Player target = findNearestPlayer(npc, players);
                if (target != null) {
                    npc.moveTowards(target.getX(), target.getY());

                    // si está en rango, atacar
                    boolean attacked = npc.tryAttack(target, NPC_DAMAGE, MELEE_RANGE);
                    if (attacked) {
                        // persistir el cambio de vida del jugador
                        playerRepository.save(target);
                    }
                }
            }

            // Limpieza: eliminar NPCs muertos y asignar XP atómicamente
            List<NPC> toRemove = new ArrayList<>();
            for (NPC npc : list) {
                if (npc.isDead()) {
                    String killer = npc.getLastHitBy();
                    // Solo uno podrá asignar XP: usamos markXpAwarded() + lock por sala
                    if (npc.markXpAwarded() && killer != null) {
                        // protección por sala para que no se duplique
                        lockManager.withLock("room_" + roomCode, () -> {
                            // Atribuir 100 XP al room (GamePlayServices) y opcionalmente al jugador
                            gamePlayServices.addExperience(roomCode, XP_PER_KILL);
                        });
                    }
                    toRemove.add(npc);
                }
            }

            if (!toRemove.isEmpty()) {
                list.removeAll(toRemove);
            }

            // Si hay menos de NPC_INITIAL y espacio -> spawnear en batch
            if (list.size() < NPC_INITIAL) {
                trySpawnBatch(roomCode);
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
}