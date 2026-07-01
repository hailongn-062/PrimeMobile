package org.example.primemobile.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class ThongTinUIController {

    @GetMapping("/lien-he")
    public String lienHe(Model model) {
        return page(model, "Liên hệ", "Liên hệ PrimeMobile", List.of(
                "Hotline: 1900 1234, hỗ trợ từ 8:00 đến 22:00 tất cả các ngày.",
                "Email hỗ trợ: support@primemobile.vn.",
                "Cửa hàng: 123 Đường Láng, Đống Đa, Hà Nội."));
    }

    @GetMapping({"/chinh-sach-bao-hanh", "/bao-hanh"})
    public String baoHanh(Model model) {
        return page(model, "Chính sách bảo hành", "Chính sách bảo hành", List.of(
                "Sản phẩm chính hãng được bảo hành theo chính sách của nhà sản xuất.",
                "PrimeMobile hỗ trợ tiếp nhận bảo hành trong suốt thời gian bảo hành còn hiệu lực.",
                "Máy cần còn nguyên số IMEI/serial và không thuộc trường hợp từ chối bảo hành do rơi vỡ, vào nước hoặc can thiệp trái phép."));
    }

    @GetMapping("/chinh-sach-doi-tra")
    public String doiTra(Model model) {
        return page(model, "Chính sách đổi trả", "Chính sách đổi trả", List.of(
                "Hỗ trợ đổi trả khi sản phẩm phát sinh lỗi phần cứng do nhà sản xuất trong thời gian chính sách còn hiệu lực.",
                "Sản phẩm đổi trả cần đầy đủ hộp, phụ kiện, hóa đơn và không bị hư hỏng do người dùng.",
                "PrimeMobile kiểm tra tình trạng máy trước khi xác nhận phương án đổi trả."));
    }

    @GetMapping("/huong-dan-mua-hang")
    public String huongDan(Model model) {
        return page(model, "Hướng dẫn mua hàng", "Hướng dẫn mua hàng", List.of(
                "Chọn sản phẩm, màu sắc và dung lượng phù hợp rồi thêm vào giỏ hàng.",
                "Đăng nhập, kiểm tra giỏ hàng, chọn địa chỉ nhận hàng và phương thức thanh toán.",
                "Sau khi đặt hàng, bạn có thể theo dõi trạng thái trong mục Đơn hàng của tôi."));
    }

    @GetMapping("/chinh-sach-bao-mat")
    public String baoMat(Model model) {
        return page(model, "Chính sách bảo mật", "Chính sách bảo mật", List.of(
                "PrimeMobile chỉ sử dụng thông tin khách hàng để xử lý đơn hàng, giao hàng và hỗ trợ sau bán.",
                "Thông tin liên hệ và địa chỉ giao hàng được bảo vệ trong hệ thống nội bộ.",
                "Khách hàng có thể liên hệ PrimeMobile khi cần cập nhật hoặc kiểm tra thông tin tài khoản."));
    }

    @GetMapping("/dieu-khoan-su-dung")
    public String dieuKhoan(Model model) {
        return page(model, "Điều khoản sử dụng", "Điều khoản sử dụng", List.of(
                "Khách hàng cần cung cấp thông tin chính xác khi đặt hàng.",
                "Giá bán, khuyến mãi và tồn kho có thể thay đổi theo thời điểm xác nhận đơn.",
                "PrimeMobile có quyền liên hệ xác minh đơn hàng trước khi giao."));
    }

    @GetMapping("/he-thong-cua-hang")
    public String heThongCuaHang(Model model) {
        return page(model, "Hệ thống cửa hàng", "Hệ thống cửa hàng", List.of(
                "PrimeMobile Đống Đa: 123 Đường Láng, Đống Đa, Hà Nội.",
                "Giờ mở cửa: 8:00 đến 22:00 từ Thứ 2 đến Chủ nhật.",
                "Hotline cửa hàng: 1900 1234."));
    }

    @GetMapping("/uu-dai")
    public String khuyenMai(Model model) {
        return page(model, "Khuyến mãi", "Khuyến mãi PrimeMobile", List.of(
                "Các chương trình ưu đãi được tự động áp dụng khi sản phẩm hoặc đơn hàng đủ điều kiện.",
                "Flash sale và giảm giá có thể thay đổi theo thời gian và số lượng còn lại.",
                "Bạn có thể xem giá cuối cùng tại trang sản phẩm hoặc trong giỏ hàng."));
    }

    @GetMapping("/login")
    public String loginAlias() {
        return "redirect:/dang-nhap";
    }

    private String page(Model model, String title, String heading, List<String> paragraphs) {
        model.addAttribute("pageTitle", title);
        model.addAttribute("heading", heading);
        model.addAttribute("paragraphs", paragraphs);
        return "thong-tin/index";
    }
}
