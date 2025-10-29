package com.dinosurio_G.Back.model;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;


public class NPC {
    private final String id = UUID.randomUUID().toString();
    private volatile double x;
    private volatile double y;


    private volatile int health;
    private final double speed;


    // Último jugador que hizo daño (para atribuir experiencia al morir)
    private volatile String lastHitBy;


    // Para evitar doble contabilización de la muerte/XP
    private final AtomicBoolean dead = new AtomicBoolean(false);
    private final AtomicBoolean xpAwarded = new AtomicBoolean(false);


    // Ataque cuerpo a cuerpo
    private volatile long lastAttackTime = 0;
    private final long attackCooldownMs = 800; // 0.8s entre ataques


    public NPC(double startX, double startY, int health, double speed) {
        this.x = startX;
        this.y = startY;
        this.health = health;
        this.speed = speed;
    }


    // Getters
    public String getId() { return id; }
    public double getX() { return x; }
    public double getY() { return y; }
    public int getHealth() { return health; }
    public double getSpeed() { return speed; }
    public boolean isDead() { return dead.get(); }


    public String getLastHitBy() { return lastHitBy; }


    // Movimiento: mover hacía (tx, ty) con paso igual a `speed`
    public void moveTowards(double tx, double ty) {
        if (isDead()) return;
        double dx = tx - x;
        double dy = ty - y;
        double dist = Math.sqrt(dx*dx + dy*dy);
        if (dist <= 0.0001) return;
        double step = Math.min(speed, dist);
        x += dx / dist * step;
        y += dy / dist * step;
    }


    // Recibir daño. Retorna true si el ataque mató al NPC.
    public synchronized boolean receiveDamage(int damage, String playerName) {
        if (isDead()) return false;
        if (damage <= 0) return false;
        this.health -= damage;
        this.lastHitBy = playerName;
        if (this.health <= 0) {
            this.health = 0;
            dead.set(true);
            return true;
        }
        return false;
    }


    // Marca que la XP ya fue otorgada (devuelve true si fue la primera vez)
    public boolean markXpAwarded() {
        return xpAwarded.compareAndSet(false, true);
    }

    // Intentar atacar a un jugador: retorna true si se aplicó daño.
    public boolean tryAttack(Player player, int damage, double meleeRange) {
        if (isDead() || player == null || !player.isAlive()) return false;
        double dx = player.getX() - this.x;
        double dy = player.getY() - this.y;
        double dist = Math.sqrt(dx*dx + dy*dy);
        long now = System.currentTimeMillis();
        if (dist <= meleeRange && (now - lastAttackTime) >= attackCooldownMs) {
            lastAttackTime = now;
            player.receiveDamage(damage);
            return true;
        }
        return false;
    }
}