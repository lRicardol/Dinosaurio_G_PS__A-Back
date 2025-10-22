package com.dinosurio_G.Back.controller;

import com.dinosurio_G.Back.model.Chest;
import com.dinosurio_G.Back.service.impl.ChestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Controlador REST para gestionar cofres (Chest).
 * Cumple con los principios SOLID, Clean Architecture y soporta asincronía.
 */
@RestController
@RequestMapping("/api/chests")
public class ChestController {

    private final ChestService chestService;

    @Autowired
    public ChestController(ChestService chestService) {
        this.chestService = chestService;
    }

    @Async
    @GetMapping
    public CompletableFuture<ResponseEntity<List<Chest>>> getAllChests() {
        return CompletableFuture.supplyAsync(() -> {
            List<Chest> chests = chestService.findAll();
            return chests.isEmpty()
                    ? ResponseEntity.noContent().build()
                    : ResponseEntity.ok(chests);
        });
    }

    @Async
    @GetMapping("/{id}")
    public CompletableFuture<ResponseEntity<Chest>> getChestById(@PathVariable Long id) {
        return CompletableFuture.supplyAsync(() ->
                chestService.findById(id)
                        .map(ResponseEntity::ok)
                        .orElse(ResponseEntity.notFound().build()));
    }

    @Async
    @PostMapping
    public CompletableFuture<ResponseEntity<Chest>> createChest(@RequestBody Chest chest) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Chest saved = chestService.save(chest);
                return ResponseEntity.ok(saved);
            } catch (Exception e) {
                return ResponseEntity.internalServerError().build();
            }
        });
    }

    @Async
    @PutMapping("/{id}")
    public CompletableFuture<ResponseEntity<Chest>> updateChest(@PathVariable Long id, @RequestBody Chest updatedChest) {
        return CompletableFuture.supplyAsync(() -> {
            Optional<Chest> existing = chestService.findById(id);
            if (existing.isEmpty()) return ResponseEntity.notFound().build();

            updatedChest.setId(id);
            Chest saved = chestService.save(updatedChest);
            return ResponseEntity.ok(saved);
        });
    }

    @Async
    @PatchMapping("/{id}")
    public CompletableFuture<ResponseEntity<Chest>> patchChest(@PathVariable Long id, @RequestBody Chest partialUpdate) {
        return CompletableFuture.supplyAsync(() -> {
            Optional<Chest> existingOpt = chestService.findById(id);
            if (existingOpt.isEmpty()) return ResponseEntity.notFound().build();

            Chest existing = existingOpt.get();

            if (partialUpdate.getType() != null) existing.setType(partialUpdate.getType());
            if (partialUpdate.getPosition() != null) existing.setPosition(partialUpdate.getPosition());
            if (partialUpdate.getContents() != null) existing.setContents(partialUpdate.getContents());

            Chest saved = chestService.save(existing);
            return ResponseEntity.ok(saved);
        });
    }

    @Async
    @DeleteMapping("/{id}")
    public CompletableFuture<ResponseEntity<Void>> deleteChest(@PathVariable Long id) {
        return CompletableFuture.runAsync(() -> chestService.deleteById(id))
                .thenApply(v -> ResponseEntity.noContent().build());
    }
}
