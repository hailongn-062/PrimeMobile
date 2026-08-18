package org.example.primemobile.repository;

import org.example.primemobile.entity.TinNhanChat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TinNhanChatRepository extends JpaRepository<TinNhanChat, Integer> {
    List<TinNhanChat> findByCuocHoiThoaiIdOrderByThoiGianAsc(Integer cuocHoiThoaiId);

    List<TinNhanChat> findTop20ByCuocHoiThoaiIdOrderByThoiGianDesc(Integer cuocHoiThoaiId);

    long countByCuocHoiThoaiId(Integer cuocHoiThoaiId);

    @Modifying
    @Query(value = "DELETE FROM tin_nhan_chat WHERE cuoc_hoi_thoai_id = :chatId AND id NOT IN " +
           "(SELECT TOP 20 id FROM tin_nhan_chat WHERE cuoc_hoi_thoai_id = :chatId ORDER BY thoi_gian DESC)", nativeQuery = true)
    void xoaTinNhanCuNhat(@Param("chatId") Integer chatId);
}
