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

@Repository
public interface TonKhoRepository extends JpaRepository<TonKho, Integer> {

    /**
     * Tìm bản ghi tồn kho theo đối tượng Kho và BienTheSanPham.
     * Dùng để kiểm tra trước khi cộng / trừ tồn kho.
     */
    Optional<TonKho> findByKhoAndBienTheSanPham(Kho kho, BienTheSanPham bienTheSanPham);

    /**
     * Tìm bản ghi tồn kho theo ID kho và ID biến thể.
     * Dùng khi chỉ có ID, tránh phải load full Entity trước.
     */
    @Query("SELECT t FROM TonKho t WHERE t.kho.id = :khoId AND t.bienTheSanPham.id = :bienTheId")
    Optional<TonKho> findByKhoIdAndBienTheId(@Param("khoId") Integer khoId,
                                             @Param("bienTheId") Integer bienTheId);

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
     * Lấy tất cả SKU có tồn kho > 0 tại Kho Tổng, kèm đầy đủ thông tin
     * biến thể và sản phẩm cha — dùng cho màn hình POS.
     *
     * <p>JOIN FETCH tất cả liên kết trong 1 query để tránh N+1.
     * Chỉ trả về biến thể còn hàng ({@code soLuong > 0}).
     *
     * @param loaiKho Loại kho (truyền {@code "kho_tong"}).
     * @return Danh sách {@link TonKho} kèm biến thể và sản phẩm cha.
     */
    @Query("""
            SELECT t FROM TonKho t
            JOIN FETCH t.bienTheSanPham bt
            JOIN FETCH bt.sanPham sp
            LEFT JOIN FETCH bt.hinhAnhSanPhams ha
            WHERE t.kho.loai = :loaiKho
              AND t.soLuong  > 0
            ORDER BY sp.tenSanPham ASC
            """)
    List<TonKho> layDanhSachChoPos(@Param("loaiKho") String loaiKho);
}
