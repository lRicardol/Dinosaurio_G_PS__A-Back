package com.dinosurio_G.Back.service.interfaces;

import com.dinosurio_G.Back.model.Chest;
import java.util.List;
import java.util.Optional;

public interface IChestService {

    Chest save(Chest chest);
    Optional<Chest> findById(Long id);
    List<Chest> findAll();
    void deleteById(Long id);
    public List<Chest> findByMapId(Long mapId);
}
