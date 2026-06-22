package org.example.primemobile.repository;

import org.example.primemobile.entity.TinNhanChat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TinNhanChatRepository extends JpaRepository<TinNhanChat, Integer> {
}
