package iuh.fit.se.eclinic.booking.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import iuh.fit.se.eclinic.booking.repository.KhungGioKhamRepository;
import iuh.fit.se.eclinic.booking.service.KhungGioKhamService;
import iuh.fit.se.eclinic.common.entity.scheduling.KhungGioKham;
import iuh.fit.se.eclinic.common.enums.TrangThaiKhungGio;
import iuh.fit.se.eclinic.common.exception.LoiKhongTimThay;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class KhungGioKhamServiceImpl implements KhungGioKhamService {

    private final KhungGioKhamRepository khungGioKhamRepository;

    @Override
    public KhungGioKham layTheoId(Long id) {
        return khungGioKhamRepository.findById(id).orElseThrow(() -> new LoiKhongTimThay("KhungGioKham", id));
    }

    @Override
    public List<KhungGioKham> timTheoLichLamViec(Long lichLamViecId) {
        return khungGioKhamRepository.findByLichLamViecIdOrderByGioBatDauAsc(lichLamViecId);
    }

    @Override
    public List<KhungGioKham> timKhungGioConTrongTheoBacSi(Long bacSiId, LocalDateTime tu, LocalDateTime den) {
        return khungGioKhamRepository.findByBacSiAndTrangThaiInRange(bacSiId, TrangThaiKhungGio.CON_TRONG, tu, den);
    }

}
