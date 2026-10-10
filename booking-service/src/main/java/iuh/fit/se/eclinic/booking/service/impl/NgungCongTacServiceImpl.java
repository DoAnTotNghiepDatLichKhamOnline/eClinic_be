package iuh.fit.se.eclinic.booking.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import iuh.fit.se.eclinic.booking.dto.response.KetQuaHuyCaCuaBacSiResponse;
import iuh.fit.se.eclinic.booking.repository.LichLamViecRepository;
import iuh.fit.se.eclinic.booking.service.NgungCongTacService;
import iuh.fit.se.eclinic.booking.service.QuanLyCaService;
import iuh.fit.se.eclinic.common.exception.LoiNghiepVu;
import iuh.fit.se.eclinic.common.exception.MaLoi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Không có transaction ở đây: mỗi ca hủy trong transaction riêng của {@link QuanLyCaService#huyKhongBaoBacSi}. */
@Slf4j
@Service
@RequiredArgsConstructor
public class NgungCongTacServiceImpl implements NgungCongTacService {

    private final LichLamViecRepository lichLamViecRepository;
    private final QuanLyCaService quanLyCaService;

    @Override
    public KetQuaHuyCaCuaBacSiResponse huyCaSapToi(Long idBacSi, Long idTaiKhoanQuanTri, String lyDo) {
        // Kiểm tra quyền 1 lần ở đây để tài khoản sai báo lỗi ngay cả khi bác sĩ không còn ca nào
        quanLyCaService.layQuanTriVien(idTaiKhoanQuanTri);
        LocalDateTime bayGio = LocalDateTime.now();
        List<Long> idCacCa = lichLamViecRepository.timIdCaSapToiCuaBacSi(idBacSi, bayGio.toLocalDate(),
                bayGio.toLocalTime());
        int soCa = 0;
        int soLichHen = 0;
        for (Long idCa : idCacCa) {
            try {
                soLichHen += quanLyCaService.huyKhongBaoBacSi(idTaiKhoanQuanTri, idCa, lyDo);
                soCa++;
            } catch (LoiNghiepVu ex) {
                // Ca vừa bắt đầu hoặc vừa bị người khác hủy trong lúc đang chạy: bỏ qua, các ca còn lại vẫn hủy
                if (ex.getMaLoi() != MaLoi.CA_KHONG_SUA_DUOC && ex.getMaLoi() != MaLoi.KHONG_TIM_THAY) {
                    throw ex;
                }
            }
        }
        log.info("Bác sĩ id={} ngừng công tác: hủy {} ca sắp tới, {} lịch hẹn cần đổi lịch", idBacSi, soCa, soLichHen);
        return new KetQuaHuyCaCuaBacSiResponse(soCa, soLichHen);
    }

}
