package com.kanta.kanban.domain.card.repository;

import com.kanta.kanban.domain.card.entity.Card;
import com.kanta.kanban.domain.card.enumeration.CardStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CardRepository extends JpaRepository<Card, UUID> {
    @Query("""
        select c from Card c
        where c.board.id = :boardId
          and (:status is null or c.status = :status)
          and (:assigneeMemberId is null or c.assigneeMemberId = :assigneeMemberId)
        order by c.createdAt desc
        """)
    Page<Card> findCards(
        @Param("boardId") UUID boardId,
        @Param("status") CardStatus status,
        @Param("assigneeMemberId") UUID assigneeMemberId,
        Pageable pageable
    );

    @Query("""
        select c from Card c
        where c.board.id = :boardId
          and lower(c.title) like lower(concat('%', :titleContains, '%'))
        order by c.createdAt desc
        """)
    List<Card> findByBoardIdAndTitleContains(@Param("boardId") UUID boardId, @Param("titleContains") String titleContains, Pageable pageable);
}
