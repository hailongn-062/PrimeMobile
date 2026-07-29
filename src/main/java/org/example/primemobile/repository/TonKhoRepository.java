package org.example.primemobile.repository;

import org.example.primemobile.entity.BienTheSanPham;
import org.example.primemobile.entity.Kho;
import org.example.primemobile.entity.TonKho;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Lock;

import jakarta.persistence.LockModeType;

@Repository
public interface TonKhoRepository extends JpaRepository<TonKho, Integer> {

    /**
     * Tìm bản ghi tồn kho theo đối tượng Kho và BienTheSanPham.
     * Dùng để kiểm tra trước khi cộng / trừ tồn kho.
     */
    Optional<TonKho> findByKhoAndBienTheSanPham(Kho kho, BienTheSanPham bienTheSanPham);

    /**
     * Tìm bản ghi tồn kho với Pessimistic Write Lock (SELECT ... FOR UPDATE).
     * <p>
     * <b>TUYỆT ĐỐI chỉ dùng tại bước EXECUTE (trừ kho thực tế)</b> — không dùng
     * cho bước pre-validate thông thường.
     * <p>
     * Cơ chế: Khi luồng A giữ lock, luồng B bị block cho đến khi A commit.
     * Sau đó B đọc lại tồn kho thực tế, tránh Lost Update (race condition §3.1).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM TonKho t WHERE t.kho = :kho AND t.bienTheSanPham = :bienTheSanPham")
    Optional<TonKho> findByKhoAndBienTheSanPhamForUpdate(@Param("kho") Kho kho,
                                                         @Param("bienTheSanPham") BienTheSanPham bienTheSanPham);


    /**
     * Tìm bản ghi tồn kho theo ID kho và ID biến thể.
     * Dùng khi chỉ có ID, tránh phải load full Entity trước.
     */
    @Query("SELECT t FROM TonKho t WHERE t.kho.id = :khoId AND t.bienTheSanPham.id = :bienTheId")
    Optional<TonKho> findByKhoIdAndBienTheId(@Param("khoId") Integer khoId,
                                             @Param("bienTheId") Integer bienTheId);

    @Query("SELECT t FROM TonKho t JOIN FETCH t.bienTheSanPham bt JOIN FETCH bt.sanPham sp " +
           "WHERE (:khoId IS NULL OR t.kho.id = :khoId) " +
           "AND (:maSku IS NULL OR LOWER(bt.maSku) LIKE LOWER(CONCAT('%', :maSku, '%'))) " +
           "AND (:tenSanPham IS NULL OR LOWER(sp.tenSanPham) LIKE LOWER(CONCAT('%', :tenSanPham, '%')))")
    List<TonKho> timKiemTonKho(@Param("khoId") Integer khoId,
                               @Param("maSku") String maSku,
                               @Param("tenSanPham") String tenSanPham);

    /**
     * Lấy toàn bộ tồn kho của một kho cụ thể, eager-load biến thể để hiển thị UI.
     */
    @Query("SELECT t FROM TonKho t JOIN FETCH t.bienTheSanPham bt JOIN FETCH bt.sanPham WHERE t.kho.id = :khoId")
    List<TonKho> findByKhoIdWithDetails(@Param("khoId") Integer khoId);

    /**
     * Lấy tồn kho hiện tại của 1 biến thể SKU tại tất cả các kho.
     * Dùng cho màn hình chi tiết sản phẩm (hiển thị "Còn hàng / Hết hàng").
     */
    List<TonKho> findByBienTheSanPham(BienTheSanPham bienTheSanPham);

    /**
     * Lấy tất cả SKU có tồn kho > 0 tại một kho cụ thể, kèm đầy đủ thông tin
     * biến thể và sản phẩm cha — dùng cho màn hình POS.
     *
     * <p>JOIN FETCH tất cả liên kết trong 1 query để tránh N+1.
     * Chỉ trả về biến thể còn hàng ({@code soLuong > 0}).
     *
     * @param khoId ID kho cần lấy danh sách (ví dụ: kho chính của cửa hàng).
     * @return Danh sách {@link TonKho} kèm biến thể và sản phẩm cha.
     */
    @Query("""
            SELECT t FROM TonKho t
            JOIN FETCH t.bienTheSanPham bt
            JOIN FETCH bt.sanPham sp
            LEFT JOIN FETCH bt.hinhAnhSanPhams ha
            WHERE t.kho.id = :khoId
              AND t.soLuong  > 0
            ORDER BY sp.tenSanPham ASC
            """)
    List<TonKho> layDanhSachChoPos(@Param("khoId") Integer khoId);

    /**
     * Tổng tồn kho theo danh sách biến thể tại một kho cụ thể.
     *
     * @param khoId      ID kho cần tổng hợp.
     * @param bienTheIds Danh sách ID biến thể cần tổng hợp.
     * @return Mảng [bienTheSanPhamId, tongSoLuong].
     */
    @Query("""
            SELECT t.bienTheSanPham.id, COALESCE(SUM(t.soLuong), 0)
            FROM TonKho t
            WHERE t.kho.id = :khoId
              AND t.bienTheSanPham.id IN :bienTheIds
            GROUP BY t.bienTheSanPham.id
            """)
    List<Object[]> tongTonKhoTheoBienTheIds(@Param("khoId") Integer khoId,
                                            @Param("bienTheIds") List<Integer> bienTheIds);
}
