package com.sportsinfo.backend.analysis;

import org.springframework.data.jpa.repository.JpaRepository;

public interface GameAnalysisRepository extends JpaRepository<GameAnalysis, String> {
}
