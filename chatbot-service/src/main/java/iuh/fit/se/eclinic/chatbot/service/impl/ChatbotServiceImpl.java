package iuh.fit.se.eclinic.chatbot.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import iuh.fit.se.eclinic.chatbot.repository.PhienChatRepository;
import iuh.fit.se.eclinic.chatbot.repository.TinNhanChatRepository;
import iuh.fit.se.eclinic.chatbot.service.ChatbotService;
import iuh.fit.se.eclinic.common.entity.chatbot.PhienChat;
import iuh.fit.se.eclinic.common.entity.chatbot.TinNhanChat;
import iuh.fit.se.eclinic.common.exception.LoiKhongTimThay;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatbotServiceImpl implements ChatbotService {

    private final PhienChatRepository phienChatRepository;
    private final TinNhanChatRepository tinNhanChatRepository;

    @Override
    public PhienChat layPhienChatTheoId(Long phienChatId) {
        return phienChatRepository.findById(phienChatId)
                .orElseThrow(() -> new LoiKhongTimThay("PhienChat", phienChatId));
    }

    @Override
    public List<PhienChat> timPhienChatTheoTaiKhoan(Long taiKhoanId) {
        return phienChatRepository.findByTaiKhoanIdOrderByThoiGianBatDauDesc(taiKhoanId);
    }

    @Override
    public List<TinNhanChat> timTinNhan(Long phienChatId) {
        return tinNhanChatRepository.findByPhienChatIdOrderByNgayTaoAsc(phienChatId);
    }

}
