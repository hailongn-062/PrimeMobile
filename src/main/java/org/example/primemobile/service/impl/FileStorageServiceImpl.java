package org.example.primemobile.service.impl;

import org.example.primemobile.service.IFileStorageService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageServiceImpl implements IFileStorageService {

    // Thư mục lưu trữ file (tạo tại thư mục gốc của dự án)
    private final Path fileStorageLocation;

    public FileStorageServiceImpl() {
        this.fileStorageLocation = Paths.get("uploads").toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new RuntimeException("Không thể tạo thư mục lưu trữ file: uploads/", ex);
        }
    }

    @Override
    public String storeFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new RuntimeException("Lỗi tải file: File trống.");
        }
        
        // Lấy tên file gốc và làm sạch
        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
        
        // Kiểm tra xem tên file có chứa ký tự không hợp lệ không
        if (originalFileName.contains("..")) {
            throw new RuntimeException("Lỗi tên file chứa đường dẫn không hợp lệ: " + originalFileName);
        }

        // Tạo tên file mới để tránh trùng lặp
        String fileExtension = "";
        try {
            fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
        } catch (Exception e) {
            fileExtension = ""; // Nếu không có đuôi mở rộng
        }
        
        String newFileName = UUID.randomUUID().toString() + fileExtension;

        try {
            // Copy file vào thư mục lưu trữ (thay thế nếu tồn tại trùng tên, nhưng dùng UUID nên hiếm khi xảy ra)
            Path targetLocation = this.fileStorageLocation.resolve(newFileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            // Trả về URL để truy cập file tĩnh thông qua WebMvcConfig
            return "/uploads/" + newFileName;
        } catch (IOException ex) {
            throw new RuntimeException("Không thể lưu file " + newFileName + ". Vui lòng thử lại!", ex);
        }
    }
}
