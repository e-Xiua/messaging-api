package com.iwellness.messaging.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.iwellness.messaging.entity.Conversation;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    @Query("SELECT c FROM Conversation c WHERE (c.user1Id = :userId OR c.user2Id = :userId)")
    List<Conversation> findByParticipant(@Param("userId") Long userId);

    @Query("SELECT c FROM Conversation c WHERE (c.user1Id = :user1 AND c.user2Id = :user2) OR (c.user1Id = :user2 AND c.user2Id = :user1)")
    Optional<Conversation> findByUsers(@Param("user1") Long user1, @Param("user2") Long user2);
}
