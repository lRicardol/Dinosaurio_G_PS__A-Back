package com.dinosurio_G.Back.service.interfaces;

import com.dinosurio_G.Back.model.GameMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface IGameMapService {

    GameMap save(GameMap map);
    Optional<GameMap> findById(Long id);
    List<GameMap> findAll();
    void deleteById(Long id);

    void updateSafely(Long mapId, Runnable updateAction);
    CompletableFuture<Void> updateAsync(Long mapId, Runnable asyncUpdateAction);
}
