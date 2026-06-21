package com.kanta.kanban.domain.board.repository;

import com.kanta.kanban.domain.board.entity.Board;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BoardRepository extends JpaRepository<Board, UUID> {
}
