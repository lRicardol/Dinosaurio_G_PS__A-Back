package com.dinosurio_G.Back.repository;

import com.dinosurio_G.Back.model.Chest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChestRepository extends JpaRepository<Chest, Long> {
    List<Chest> findByMapId(Long mapId);
}
