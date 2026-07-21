package org.example.primemobile.controller;

import jakarta.servlet.http.HttpSession;
import org.example.primemobile.dto.auth.SessionUser;
import org.example.primemobile.interceptor.AuthInterceptor;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice(basePackages = "org.example.primemobile.controller")
public class GlobalAdminControllerAdvice {

    /**
     * Tự động đẩy thông tin currentUser (SessionUser) vào Model cho mọi request
     * thuộc package controller. Giúp tránh việc phải khai báo lặp đi lặp lại
     * model.addAttribute("currentUser", currentUser) trong từng phương thức.
     */
    @ModelAttribute("currentUser")
    public SessionUser getCurrentUser(HttpSession session) {
        if (session != null) {
            Object sessionAttr = session.getAttribute(AuthInterceptor.SESSION_KEY);
            if (sessionAttr instanceof SessionUser) {
                return (SessionUser) sessionAttr;
            }
        }
        return null;
    }
}
