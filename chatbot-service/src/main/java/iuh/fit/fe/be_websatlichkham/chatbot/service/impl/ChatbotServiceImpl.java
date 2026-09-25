package iuh.fit.fe.be_websatlichkham.chatbot.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import iuh.fit.fe.be_websatlichkham.chatbot.repository.PhienChatRepository;
import iuh.fit.fe.be_websatlichkham.chatbot.repository.TinNhanChatRepository;
import iuh.fit.fe.be_websatlichkham.chatbot.service.ChatbotService;
import iuh.fit.fe.be_websatlichkham.common.entity.chatbot.PhienChat;
import iuh.fit.fe.be_websatlichkham.common.entity.chatbot.TinNhanChat;
import iuh.fit.fe.be_websatlichkham.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatbotServiceImpl implements ChatbotService {

    private final PhienChatRepository phienChatRepository;
    private final TinNhanChatRepository tinNhanChatRepository;

    @Override
    public PhienChat getPhienChatById(Long phienChatId) {
        return phienChatRepository.findById(phienChatId)
                .orElseThrow(() -> new ResourceNotFoundException("PhienChat", phienChatId));
    }

    @Override
    public List<PhienChat> findPhienChatByTaiKhoan(Long taiKhoanId) {
        return phienChatRepository.findByTaiKhoanIdOrderByThoiGianBatDauDesc(taiKhoanId);
    }

    @Override
    public List<TinNhanChat> findTinNhan(Long phienChatId) {
        return tinNhanChatRepository.findByPhienChatIdOrderByNgayTaoAsc(phienChatId);
    }

}
