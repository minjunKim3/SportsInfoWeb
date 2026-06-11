package com.sportsinfo.backend.game;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface GameRepository extends JpaRepository<Game, String> {

    List<Game> findByGameDateOrderByGameDateTimeAsc(LocalDate gameDate);
}
