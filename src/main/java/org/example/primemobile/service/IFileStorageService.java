package org.example.primemobile.service;

import org.springframework.web.multipart.MultipartFile;

public interface IFileStorageService {
    /**
     * Lưu file tải lên và trả về đường dẫn URL của file.
     * @param file File được tải lên
     * @return URL tĩnh để truy cập file (vd: /uploads/abc-xyz.jpg)
     */
    String storeFile(MultipartFile file);
}
