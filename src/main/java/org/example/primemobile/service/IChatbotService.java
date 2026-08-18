package org.example.primemobile.service;

import org.example.primemobile.entity.CuocHoiThoai;
import org.example.primemobile.entity.TinNhanChat;
import java.util.List;

public interface IChatbotService {
    CuocHoiThoai layHoacTaoCuocHoiThoai(Integer khachHangId, String sessionId);
    List<TinNhanChat> layLichSuTinNhan(Integer cuocHoiThoaiId);
    org.example.primemobile.dto.ChatbotResponseDto guiTinNhan(Integer cuocHoiThoaiId, String message, Integer khachHangId);
}
