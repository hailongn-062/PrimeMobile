package org.example.primemobile.repository;

import org.example.primemobile.entity.ThanhToan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ThanhToanRepository extends JpaRepository<ThanhToan, Integer> {

    Optional<ThanhToan> findByVnpTransactionNo(String vnpTransactionNo);

    List<ThanhToan> findByDonHangId(Integer donHangId);

    Optional<ThanhToan> findByDonHangIdAndTrangThai(Integer donHangId, String trangThai);

    /**
     * Tìm bản ghi thanh toán theo mã tham chiếu giao dịch VNPay (vnp_TxnRef).
     * <p>
     * Dùng để xử lý IPN và Return từ VNPay, tra cứu giao dịch khi VNPay gọi callback.
     *
     * @param vnpTxnRef Mã tham chiếu giao dịch do hệ thống tạo và gửi sang VNPay.
     * @return Bản ghi thanh toán tương ứng, hoặc empty nếu không tìm thấy.
     */
    Optional<ThanhToan> findByVnpTxnRef(String vnpTxnRef);
}