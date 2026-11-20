package com.example.gridstream.domain;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PositionHistoryRepository extends JpaRepository<PositionHistoryEntry, Long> {
}
