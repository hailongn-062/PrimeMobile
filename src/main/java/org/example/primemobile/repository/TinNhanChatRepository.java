package org.example.primemobile.repository;

import org.example.primemobile.entity.TinNhanChat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TinNhanChatRepository extends JpaRepository<TinNhanChat, Integer> {
    List<TinNhanChat> findByCuocHoiThoaiIdOrderByThoiGianAsc(Integer cuocHoiThoaiId);
}
