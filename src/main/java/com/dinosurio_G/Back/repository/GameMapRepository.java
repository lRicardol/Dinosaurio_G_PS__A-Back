package com.dinosurio_G.Back.repository;

import com.dinosurio_G.Back.model.GameMap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GameMapRepository extends JpaRepository<GameMap, Long> {
}
