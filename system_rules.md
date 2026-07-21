# TÀI LIỆU QUY TẮC NGHIỆP VỤ HỆ THỐNG (SYSTEM RULES & BUSINESS LOGIC) - PRIMEMOBILE

## 1. TỔNG QUAN KIẾN TRÚC & PHÂN QUYỀN (AUTHENTICATION & AUTHORIZATION)
- **Môi trường phát triển:** Java 21, Spring Boot 3.x/4.x, IntelliJ IDEA 2025.2, SQL Server.
- **Cơ chế bảo mật:** TUYỆT ĐỐI KHÔNG sử dụng Spring Security hoặc JWT. Hệ thống bắt buộc quản lý phiên đăng nhập bằng `HttpSession` kết hợp với `HandlerInterceptor` (Spring Web MVC Interceptor).
- **Phân định Giao diện (UI/UX Split):**
    - **Customer Site (Giao diện khách hàng):** Áp dụng cho Khách vãng lai và Khách hàng đã đăng nhập. Phong cách thiết kế sang trọng, hiện đại với tone màu chủ đạo là **Xanh dương và Trắng bạc** (Tham khảo cấu trúc layout của HoangHaMobile).
    - **Admin Dashboard (Giao diện quản trị):** Áp dụng cho Admin và Nhân viên để thực hiện các thao tác quản lý nội bộ.
- **Cơ chế Đăng nhập / Đăng ký:**
    - **Đăng nhập:** Xác thực bằng một trong hai trường (`email` HOẶC `so_dien_thoai`) kết hợp với mật khẩu (`mat_khau`).
    - **Đăng ký:** Bắt buộc người dùng nhập đầy đủ: `ho_ten`, `email`, `so_dien_thoai`, và `mat_khau`.
- **Cơ chế phân quyền người dùng (Role Management):**
    - `KhachVangLai` (Mặc định khi vào hệ thống): Chỉ được xem sản phẩm, biến thể, thông số kỹ thuật, đánh giá, chương trình khuyến mãi, thêm sản phẩm vào giỏ hàng và sử dụng Chatbot tư vấn. KHÔNG ĐƯỢC ĐẶT HÀNG ONLINE, KHÔNG ĐƯỢC YÊU CẦU BẢO HÀNH, KHÔNG ĐƯỢC ĐÁNH GIÁ.
    - `KhachHang` (Đã đăng nhập): Kế thừa toàn bộ quyền của Khách vãng lai và được quyền thực hiện: Đặt hàng online, xem lịch sử và chi tiết đơn hàng, hủy đơn (nếu hợp lệ), quản lý địa chỉ giao hàng, viết yêu cầu bảo hành và đánh giá sản phẩm đã mua.
    - `NhanVien`: Sử dụng Admin Dashboard để quản lý tài khoản khách hàng, xử lý/xác nhận đơn hàng online, lập phiếu nhập kho, phiếu chuyển kho, quản lý IMEI máy vật lý, tiếp nhận và xử lý bảo hành, thực hiện bán hàng offline tại quầy, phản hồi hỏi đáp/đánh giá của khách hàng.
    - `Admin`: Nắm toàn quyền tối cao (`Root`). Kế thừa toàn bộ chức năng của nhân viên, đồng thời độc quyền các tính năng: Quản lý tài khoản nhân viên, cấu hình tài khoản ngân hàng nhận tiền, tạo chương trình khuyến mãi/ Flash Sale / ..., và xem báo cáo thống kê/doanh thu.

## 2. QUY TẮC PHÂN HỆ BÁN HÀNG (SALES PROCESSES)
### 2.1. Phân hệ Bán hàng Offline (Tại quầy)
- **Đối tượng thao tác:** `NhanVien` hoặc `Admin`.
- **Luồng nghiệp vụ:** 1. Chọn sản phẩm và số lượng -> Đưa vào giỏ hàng offline.
    2. Chọn chương trình khuyến mãi/áp mã giảm giá (nếu có).
    3. Xác định thông tin khách hàng:
        - *Trường hợp 1 (Khách lẻ):* Khách vãng lai không muốn để lại thông tin, hệ thống tự động gán vào tài khoản Khách lẻ mặc định có `so_dien_thoai = '0000000000'`.
        - *Trường hợp 2 (Tạo tài khoản mới):* Khách vãng lai muốn đăng ký hội viên, nhân viên nhập `email`, `so_dien_thoai`, `ho_ten` khách cung cấp để tạo trực tiếp hồ sơ khách hàng.
    4. Hệ thống hiển thị danh sách IMEI khả dụng của SKU đã chọn trong `kho_tong`. Nhân viên bắt buộc phải chọn đúng số lượng mã IMEI tương ứng với số lượng sản phẩm bán ra.
    5. Tạo đơn hàng và thanh toán: Tiền mặt/COD. Hệ thống tự động chuyển trạng thái đơn hàng thành `da_giao`, trạng thái thanh toán thành `da_thanh_toan`.
    6. **Cập nhật tồn kho:** Trừ trực tiếp số lượng trong bảng `ton_kho` của `kho_id` thuộc về **Kho Tổng**, cập nhật trạng thái IMEI trong bảng `may_dien_thoai` thành `da_ban`.

### 2.2. Phân hệ Bán hàng Online (Website Frontend)
- **Đối tượng thao tác:** `KhachHang` (Đã đăng nhập).
- **Luồng nghiệp vụ:**
    1. Khách hàng thêm sản phẩm vào giỏ hàng online -> Bấm thanh toán.
    2. Điền thông tin giao hàng: chọn địa chỉ nhận hàng từ danh sách `dia_chi_khach_hang` để lấy thông số ID địa chỉ phục vụ tính phí ship.
    3. Áp dụng mã giảm giá/khuyến mãi (nếu có).
    4. Ấn "Đặt hàng online": Đơn hàng được tạo với trạng thái mặc định là `cho_xac_nhan`, trạng thái thanh toán là `chua_thanh_toan`.
    5. **Cập nhật tồn kho:** Trừ trực tiếp số lượng tồn kho khả dụng trong bảng `ton_kho` của **Kho Online** trừ kho khi đơn hàng có trạng thái `da_xac_nhan`.
    6. **Quy tắc Hủy đơn hàng phía Khách hàng:** Khách hàng chỉ có quyền ấn HỦY đơn hàng khi trạng thái đơn hàng đang là `cho_xac_nhan`. Khi nhân viên đã duyệt chuyển trạng thái sang `da_xac_nhan` hoặc `dang_giao`, nút Hủy phía khách hàng phải bị vô hiệu hóa (Disable).
    7. **Logic hoàn kho khi hủy đơn:** Khi đơn hàng bị hủy thành công, hệ thống bắt buộc phải cộng hoàn lại số lượng sản phẩm tương ứng vào **Kho Online**.

## 3. QUY TẮC QUẢN LÝ KHO & IMEI MÁY VẬT LÝ (INVENTORY & PHYSICAL IMEI CONTROL)
### 3.1. Quy tắc Tồn kho (Stock Rule)
- **Không áp dụng mức tồn kho dự trữ tối thiểu.** Hệ thống chỉ chặn giao dịch khi tồn kho không đủ để đáp ứng số lượng yêu cầu.
- **Logic chặn xuất hàng:** Hệ thống không được phép cho nhân viên bán offline hoặc khách hàng đặt hàng trực tuyến nếu số lượng muốn mua **vượt quá tồn kho hiện có** tại kho tương ứng (tức là sau khi trừ, tồn kho không được âm — `soLuong >= 0`).
- *Ví dụ cụ thể:* Nếu biến thể iPhone 15 Pro Max đang có tồn kho là 3 sản phẩm, hệ thống cho phép mua tối đa 3 sản phẩm. Nếu mua 4, hệ thống báo lỗi "Không đủ tồn kho". Nếu tồn kho = 0, hệ thống báo "Hết hàng".
- **Chống race condition:** Tại bước trừ kho thực tế, hệ thống bắt buộc sử dụng **Pessimistic Write Lock** (`SELECT ... FOR UPDATE`) để đảm bảo khi có nhiều giao dịch đồng thời, chỉ giao dịch hợp lệ đầu tiên được thực hiện, các giao dịch sau sẽ bị chặn sau khi đọc lại tồn kho thực tế.

### 3.2. Luồng dịch chuyển hàng hóa giữa các kho
- **Nhập kho từ Nhà cung cấp (Inbound):** Khi nhân viên tạo `phieu_nhap_kho` từ nhà cung cấp, sản phẩm mặc định chui vào **Kho Tổng** (`kho_tong`). Trạng thái phiếu nhập hoàn thành sẽ cộng trực tiếp vào số lượng của `ton_kho` Kho Tổng.
- **Chuyển kho nội bộ (Transfer):** Để đưa sản phẩm lên sàn online, nhân viên bắt buộc phải lập `phieu_chuyen_kho` từ Kho Tổng sang Kho Online (hoặc ngược lại). Luồng phiếu nhập và chuyển kho trong hệ thống này được cấu hình **chốt luôn**, nghĩa là khi nhân viên ấn xác nhận tạo phiếu thì tồn kho 2 bên tự động tăng/giảm ngay lập tức, không thông qua trạng thái chờ Admin duyệt.

### 3.3. Cơ chế kiểm soát chặt chẽ mã IMEI máy vật lý
- Bảng `may_dien_thoai` dùng để quản lý chính xác từng chiếc điện thoại thương mại thông qua mã định danh `imei1`, `imei2` và `serial`.
- **Ràng buộc đối khớp số lượng (IMEI Checklist):** Mã IMEI máy vật lý là hệ quả phụ thuộc vào số lượng nhập kho trong bảng `ton_kho`.
    - Khi `ton_kho` tăng lên do có phiếu nhập, phân hệ Quản lý IMEI sẽ quét và hiển thị danh sách các SKU chưa được định danh đủ mã máy vật lý (Ví dụ: Thao tác nhập thêm 3 máy SKU_A nâng tồn kho từ 7 lên 10, hệ thống sẽ báo cáo SKU_A đang thiếu 3 mã IMEI).
    - Nhân viên bắt buộc phải nhập tay/quét mã đủ 3 cụm `imei1`, `imei2`, `serial` khác nhau vào hệ thống.
    - **Chặn thêm thừa:** Nếu số lượng bản ghi IMEI của một SKU trong bảng `may_dien_thoai` (có trạng thái `trong_kho`) đã bằng đúng số lượng ghi nhận trong bảng `ton_kho`, hệ thống phải CHẶN hoàn toàn không cho phép nhân viên thêm bất kỳ mã IMEI nào khác cho SKU đó.

## 5. CÁC QUY TẮC PHỤ TRỢ KHÁC (PROMOTIONS, REVIEWS, WARRANTY, WISHLIST)
- **Khuyến mãi:** Phân loại rõ ràng 4 loại giảm giá còn lại là 'giam_gia_truc_tiep','phan_tram','flash_sale','don_hang_toi_thieu' và cả 4 loại giảm giá này đều là giảm theo %
chỉ khác nhau là:
- 'phan_tram': không có điều kiện gì đặc biệt đến thời gian đang diễn ra là được áp dụng giảm giá %
- 'giam_gia_truc_tiep': khi chọn loại này sẽ hiện thêm chỗ chọn sản phẩm và sản phẩm đó sẽ được giảm giá % (đây chính là chức năng của bảng pham_vi_khuyen_mai) (bỏ ko giảm theo danh mục và hãng nữa)
- 'flash_sale': khi chọn loại này sẽ hiện lên các trường để nhập cho flash sale (giờ kết thúc, bắt đầu, số lượng) Nếu trong khoảng thời gian diễn ra flash sale và số lượng còn (>0) thì sẽ được áp dụng giảm giá %
- 'don_hang_toi_thieu': khi chọn loại này sẽ hiện thêm trường đơn hàng tối thiểu nhập số tiền vào đấy nếu khách hàng có tổng tiền đơn hàng >= số tiền đó sẽ được áp dụng mã giảm giá
- **Ràng buộc Đánh giá sản phẩm:** Chỉ những tài khoản khách hàng nào có lịch sử mua sản phẩm đó với trạng thái đơn hàng tương ứng là `da_giao` (đã đối khớp trong `chi_tiet_don_hang`) mới có quyền viết đánh giá sản phẩm. Bài đánh giá hỗ trợ đính kèm hình ảnh dạng mảng chuỗi JSON lưu vào database. Nhân viên có quyền viết phản hồi trả lời khách.
- **Bảo hành:** Cửa hàng đóng vai trò trung gian tiếp nhận thiết bị từ khách hàng. Nhân viên kiểm tra mã IMEI đầu vào xem có tồn tại phiếu bảo hành hợp lệ và còn hạn sử dụng không (`phieu_bao_hanh.trang_thai = 'con_hieu_luc'`). Luồng xử lý bảo hành là hoàn toàn **0 đồng** (không phát sinh chi phí thương mại), chỉ ghi nhận trạng thái luân chuyển thiết bị: Nhận từ khách -> Gửi hãng/NCC -> Nhận lại từ hãng -> Trả lại khách.
- **Yêu Thích (Wishlist):** Khi khách hàng thêm sản phẩm vào mục yêu thích, mã nguồn API Tìm kiếm sản phẩm bắt buộc phải ưu tiên đẩy các sản phẩm này lên các vị trí đầu tiên trong danh sách kết quả trả về của tài khoản đó.

## 6. QUY ĐỊNH TÍCH HỢP API GIAO HÀNG NHANH (GHN) - CHỐNG TẠO ĐƠN THẬT
- **Bối cảnh:** Dự án kết nối trực tiếp với cổng API Production của Giao Hàng Nhanh (`https://online-gateway.ghn.vn`) để đảm bảo số liệu khoảng cách, bảng giá vận chuyển và thời gian giao hàng chính xác thực tế 100% giúp bài demo đồ án thuyết phục.
- **Cơ chế nạp cấu hình (Configuration Injection):** AI Agent TUYỆT ĐỐI KHÔNG được hardcode (viết chết) các chuỗi mã Token, Shop ID, hay From-District-ID vào trong code Java. Toàn bộ các thông số này bắt buộc phải được cấu hình trong file `application.properties` và được Class `GhnService` đọc động thông qua annotation `@Value` của Spring Boot:
  - `ghn.api.url`
  - `ghn.api.token`
  - `ghn.api.shop-id`
  - `ghn.api.from-district-id`
- **QUY TẮC AN TOÀN TUYỆT ĐỐI (CRITICAL SAFETY RULE):** - AI Agent khi sinh code cho `GhnService` **CHỈ ĐƯỢC PHÉP** sử dụng các Endpoint mang tính chất Tra cứu / Đọc dữ liệu (Read-only), bao gồm:
    - API tính phí vận chuyển: `/v2/shipping-order/fee`
    - API dự đoán ngày giao hàng: `/v2/shipping-order/leadtime`
    - **CẤM HOÀN TOÀN:** Không được phép viết mã nguồn gọi sang Endpoint tạo đơn vận chuyển `/v2/shipping-order/create`. Quy định này nhằm triệt tiêu hoàn toàn rủi ro hệ thống tự động gửi yêu cầu lấy hàng thật đến tổng đài GHN, đảm bảo an toàn tài chính cho tài khoản demo của người dùng.
    - **Cơ chế phòng vệ lỗi:** Toàn bộ các khối lệnh gọi HTTP Request sang API GHN bắt buộc phải bọc trong khối `try-catch`. Nếu có sự cố mất mạng hoặc API bên thứ ba phản hồi lỗi (Timeout/Bad Request), hệ thống phải bắt được ngoại lệ và trả về giá trị phí ship mặc định bằng `0` (hoặc giá fix cứng) để luồng tạo đơn hàng nội bộ không bị sập hay gián đoạn.

## 7. CÁC TÍNH NĂNG HOÃN LẠI (POSTPONED FEATURES)
Để tập trung tối ưu hóa các phân hệ cốt lõi lấy điểm tối đa từ hội đồng chấm đồ án, các tính năng sau đây tạm thời **ĐÃ ĐƯỢC LOẠI BỎ khỏi mã nguồn Backend**, AI Agent tuyệt đối không sinh code cho các phần này cho đến khi có lệnh mới:
1. **Phân hệ Chatbot AI:** Tích hợp API Gemini để tư vấn tự động sẽ được làm sau. Hiện tại không tạo Service hay Controller cho phân hệ này.
2. **Phương thức thanh toán:** Tạm hoãn cổng thanh toán tự động bằng VNpay cũng như chuyển khoản ngân hàng. Phương thức thanh toán (PTTT) hiện tại mặc định sử dụng duy nhất một phương thức là Nhận hàng trả tiền mặt (COD).
3. **Phân hệ Bảo hành:** Tạm thời hoãn việc xây dựng luồng tiếp nhận máy lỗi, tạo phiếu bảo hành và gửi nhà cung cấp.
5. **Phân hệ Đánh giá & Hỏi đáp:** Tạm hoãn tính năng cho phép khách hàng viết đánh giá sản phẩm, chấm điểm sao, đính kèm hình ảnh dạng JSON và luồng nhân viên phản hồi lại các đánh giá/câu hỏi đó.
6. **Tính năng Yêu thích (Wishlist):** Tạm hoãn việc xây dựng tính năng lưu sản phẩm vào danh sách yêu thích (`yeu_thich`) cũng như thuật toán ưu tiên đẩy sản phẩm yêu thích lên đầu khi tìm kiếm.