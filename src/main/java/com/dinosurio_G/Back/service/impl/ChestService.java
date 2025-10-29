package com.dinosurio_G.Back.service.impl;

import com.dinosurio_G.Back.model.Chest;
import com.dinosurio_G.Back.repository.ChestRepository;
import com.dinosurio_G.Back.service.interfaces.IChestService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ChestService implements IChestService {

    private final ChestRepository chestRepository;

    public ChestService(ChestRepository chestRepository) {
        this.chestRepository = chestRepository;
    }

    @Override
    public Chest save(Chest chest) {
        return chestRepository.save(chest);
    }

    @Override
    public Optional<Chest> findById(Long id) {
        return chestRepository.findById(id);
    }

    @Override
    public List<Chest> findAll() {
        return chestRepository.findAll();
    }

    @Override
    public void deleteById(Long id) {
        chestRepository.deleteById(id);
    }

    /**
     * Intenta abrir un cofre de forma transaccional. Retorna true si se abrió y se guardó.
     */
    @Transactional
    public boolean tryOpenChest(Long chestId) {
        Chest chest = chestRepository.findById(chestId).orElse(null);
        if (chest == null || !chest.isActive()) {
            return false;
        }
        chest.openChest();
        chestRepository.save(chest);
        return true;
    }


    @Override
    public List<Chest> findByMapId(Long mapId) {
        return chestRepository.findByMapId(mapId);
    }
}
