package com.dinosurio_G.Back.controller;

import com.dinosurio_G.Back.model.GameMap;
import com.dinosurio_G.Back.service.impl.GameMapService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Controlador REST para gestionar operaciones sobre GameMap.
 * Diseñado bajo principios SOLID, Clean Architecture y preparado para alta concurrencia.
 */
@RestController
@RequestMapping("/api/maps")
public class GameMapController {

    private final GameMapService gameMapService;

    @Autowired
    public GameMapController(GameMapService gameMapService) {
        this.gameMapService = gameMapService;
    }

    @Async
    @GetMapping
    public CompletableFuture<ResponseEntity<List<GameMap>>> getAllMaps() {
        return CompletableFuture.supplyAsync(() -> {
            List<GameMap> maps = gameMapService.findAll();
            return maps.isEmpty()
                    ? ResponseEntity.noContent().build()
                    : ResponseEntity.ok(maps);
        });
    }

    @Async
    @GetMapping("/{id}")
    public CompletableFuture<ResponseEntity<GameMap>> getMapById(@PathVariable Long id) {
        return CompletableFuture.supplyAsync(() -> {
            Optional<GameMap> mapOpt = gameMapService.findById(id);
            return mapOpt.map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        });
    }

    @Async
    @PostMapping
    public CompletableFuture<ResponseEntity<GameMap>> createMap(@RequestBody GameMap gameMap) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                GameMap saved = gameMapService.save(gameMap);
                return ResponseEntity.ok(saved);
            } catch (Exception e) {
                return ResponseEntity.internalServerError().build();
            }
        });
    }

    @Async
    @PutMapping("/{id}")
    public CompletableFuture<ResponseEntity<GameMap>> updateMap(@PathVariable Long id, @RequestBody GameMap updatedMap) {
        return CompletableFuture.supplyAsync(() -> {
            Optional<GameMap> existing = gameMapService.findById(id);
            if (existing.isEmpty()) return ResponseEntity.notFound().build();

            updatedMap.setId(id);
            GameMap saved = gameMapService.save(updatedMap);
            return ResponseEntity.ok(saved);
        });
    }

    @Async
    @PatchMapping("/{id}")
    public CompletableFuture<ResponseEntity<GameMap>> patchMap(@PathVariable Long id, @RequestBody GameMap partialUpdate) {
        return CompletableFuture.supplyAsync(() -> {
            Optional<GameMap> existingOpt = gameMapService.findById(id);
            if (existingOpt.isEmpty()) return ResponseEntity.notFound().build();

            GameMap existing = existingOpt.get();

            if (partialUpdate.getName() != null) existing.setName(partialUpdate.getName());
            if (partialUpdate.getSize() != null) existing.setSize(partialUpdate.getSize());
            if (partialUpdate.getDescription() != null) existing.setDescription(partialUpdate.getDescription());

            GameMap saved = gameMapService.save(existing);
            return ResponseEntity.ok(saved);
        });
    }

    @Async
    @DeleteMapping("/{id}")
    public CompletableFuture<ResponseEntity<Void>> deleteMap(@PathVariable Long id) {
        return CompletableFuture.runAsync(() -> gameMapService.deleteById(id))
                .thenApply(v -> ResponseEntity.noContent().build());
    }

    @Async
    @PostMapping("/{id}/update-safe")
    public CompletableFuture<ResponseEntity<Void>> safeUpdate(@PathVariable Long id) {
        return gameMapService.updateAsync(id, () -> {
            System.out.println("Actualización concurrente segura ejecutada en mapa " + id);
        }).thenApply(v -> ResponseEntity.ok().build());
    }
}
