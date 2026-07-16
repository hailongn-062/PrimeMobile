Bản tổng kết toàn bộ các thay đổi cơ sở dữ liệu, được chia theo từng phân hệ để dễ dàng đối chiếu với nghiệp vụ:
1. Phân hệ Khuyến Mãi (Module 9)
- Mục tiêu: Tối giản từ 4 loại rườm rà xuống còn 2 loại (giảm theo % Đơn hàng và giảm theo % Sản phẩm), lược bỏ các điều kiện thừa để hệ thống chạy nhẹ và mượt nhất.
- Xóa bảng: chi_tiet_flash_sale.
- Bảng chuong_trinh_khuyen_mai:
+> Xóa 5 cột không còn dùng: gio_flash_bat_dau, gio_flash_ket_thuc, so_luong_toi_da, so_lan_da_dung, giam_toi_da.
- Cập nhật ràng buộc chk_ctkm_loai: Giờ chỉ còn nhận 2 giá trị là 'theo_don_hang' và 'theo_san_pham'.
- (Giữ lại trường don_hang_toi_thieu để xét điều kiện cho mã theo đơn).

2. Phân hệ Đơn Hàng & Hoàn Tiền (Module 7)
- Mục tiêu: Xử lý triệt để luồng Online (Hoàn tiền VNPay) và luồng Offline (Lưu đơn hàng chờ, Hủy đơn mềm).
- Bảng don_hang (Cập nhật các ràng buộc trạng thái):
+> Ràng buộc chk_dh_trang_thai: Bổ sung 'cho_hoan_tien' (Online) và 'don_hang_cho' (Offline).
+> Đổi tên 'da_giao' thành 'da_hoan_thanh' để dùng chung cho cả 2 luồng.
+> Ràng buộc chk_dh_trang_thai_tt: Bổ sung trạng thái 'da_hoan_tien'.
- Bảng thanh_toan:
+> Ràng buộc chk_tt_trang_thai: Bổ sung trạng thái 'da_hoan_tien' để kế toán đối soát giao dịch VNPay bị hủy.

3. Phân hệ Kho Hàng & IMEI (Module 3 & 2)
- Mục tiêu: Bỏ chia kho, dọn đường cho việc Import Excel linh hoạt và khóa chặt tính toàn vẹn dữ liệu cho luồng Đơn hàng chờ.
- Bảng kho:Xóa cột loai (Cùng với 2 constraint uq_kho_loai, chk_kho_loai). 
+> Kho giờ chỉ đơn thuần lưu Tên và Địa chỉ.
- Bảng điều chuyển:Xóa 2 bảng phieu_chuyen_kho và chi_tiet_chuyen_kho (Do không chia kho online/tổng nữa nên không cần luồng này).
- Bảng may_dien_thoai (Quản lý IMEI):
+> Xóa cột serial và ràng buộc Unique cũ của nó.
+> Xóa ràng buộc Unique cũ của imei2. Thay thế bằng Filtered Index (WHERE imei2 IS NOT NULL) để cho phép nhập nhiều giá trị trống (NULL) nhưng hễ nhập số là phải duy nhất.
+> Khóa ngoại Đơn hàng: Xóa bỏ hành vi ON DELETE SET NULL tại khóa ngoại fk_may_dh. Nhằm chặn đứng việc xóa cứng đơn hàng, bắt buộc code Java phải "Hủy mềm" (chuyển trạng thái) và tự viết logic nhả IMEI về kho.

4. Phân hệ Bảo Hành (Module 8)
- Mục tiêu: Chuyển luồng gửi máy từ Nhà cung cấp (nhập hàng) sang Trung tâm bảo hành ủy quyền (bên thứ 3).
- Tạo bảng mới: trung_tam_bao_hanh (Cấu trúc tương tự NCC: id, tên, sđt, địa chỉ, người liên hệ, trạng thái).
- Bảng yeu_cau_bao_hanh:
+> Thay thế khóa ngoại nha_cung_cap_id thành trung_tam_bao_hanh_id.
+> Đổi tên 3 cột ghi nhận: ngay_gui_ncc -> ngay_gui_ttbh, ngay_nhan_lai_ncc -> ngay_nhan_lai_ttbh, ket_qua_ncc -> ket_qua_ttbh.
+> Cập nhật ràng buộc chk_ycbh_trang_thai: Đổi các từ khóa chứa chữ ncc thành ttbh ('da_gui_ttbh', 'ttbh_dang_xu_ly', 'da_nhan_lai_ttbh').