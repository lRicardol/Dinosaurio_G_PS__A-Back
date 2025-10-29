package com.dinosurio_G.Back.service.impl;

import com.dinosurio_G.Back.model.GameMap;
import com.dinosurio_G.Back.repository.GameMapRepository;
import com.dinosurio_G.Back.service.core.AsyncExecutor;
import com.dinosurio_G.Back.service.core.LockManager;
import com.dinosurio_G.Back.service.interfaces.IGameMapService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.Random;

@Service
@Transactional
public class GameMapService implements IGameMapService {

    private final GameMapRepository gameMapRepository;
    private final LockManager lockManager;
    private final AsyncExecutor asyncExecutor;
    private final Random random = new Random();

    public GameMapService(GameMapRepository gameMapRepository,
                          LockManager lockManager,
                          AsyncExecutor asyncExecutor) {
        this.gameMapRepository = gameMapRepository;
        this.lockManager = lockManager;
        this.asyncExecutor = asyncExecutor;
    }

    @Override
    public GameMap save(GameMap map) {
        return gameMapRepository.save(map);
    }

    @Override
    public Optional<GameMap> findById(Long id) {
        return gameMapRepository.findById(id);
    }

    @Override
    public List<GameMap> findAll() {
        return gameMapRepository.findAll();
    }

    @Override
    public void deleteById(Long id) {
        gameMapRepository.deleteById(id);
    }

    @Override
    public void updateSafely(Long mapId, Runnable updateAction) {
        lockManager.withLock("map_" + mapId, () -> {
            updateAction.run();
            gameMapRepository.findById(mapId).ifPresent(gameMapRepository::save);
        });
    }

    @Override
    public CompletableFuture<Void> updateAsync(Long mapId, Runnable asyncUpdateAction) {
        return asyncExecutor.runAsync(() -> updateSafely(mapId, asyncUpdateAction));
    }

    /**
     * Crea un mapa nuevo básico (posiciones y cofres vacíos) para una sala.
     * Ajusta parámetros según tu lógica de mapas.
     */
    @Override
    public GameMap createMapForRoom() {
        GameMap map = new GameMap("Mapa generado", "rectangle", "Mapa por defecto");
        map.setWidth(1000);
        map.setHeight(800);
        return gameMapRepository.save(map);
    }
}
