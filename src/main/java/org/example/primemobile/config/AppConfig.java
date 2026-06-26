package org.example.primemobile.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Cấu hình Bean dùng chung cho ứng dụng PrimeMobile.
 * <p>
 * Hiện tại khai báo {@link RestTemplate} bean dùng cho tích hợp GHN API
 * (system_rules.md §6) và các HTTP client khác trong tương lai.
 */
@Configuration
public class AppConfig {

    /**
     * Bean {@link RestTemplate} với timeout được cấu hình hợp lý.
     * <p>
     * Timeout 5 giây đảm bảo:
     * <ul>
     *   <li>Kết nối không bị treo quá lâu khi GHN API chậm.</li>
     *   <li>{@link org.example.primemobile.service.impl.GhnServiceImpl} bắt được
     *       {@code ResourceAccessException} (timeout) và kích hoạt Fail-Safe §6.</li>
     * </ul>
     *
     * @return RestTemplate bean singleton dùng toàn ứng dụng.
     */
    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000); // Timeout kết nối TCP (5 giây)
        factory.setReadTimeout(5000);    // Timeout đọc response (5 giây)
        return new RestTemplate(factory);
    }
}
