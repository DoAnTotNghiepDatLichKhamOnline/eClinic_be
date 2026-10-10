package iuh.fit.se.eclinic.booking.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import iuh.fit.se.eclinic.booking.dto.request.SuaHoSoBenhNhanRequest;
import iuh.fit.se.eclinic.booking.dto.response.HoSoBenhNhanQuanTriResponse;
import iuh.fit.se.eclinic.booking.repository.HoSoBenhNhanRepository;
import iuh.fit.se.eclinic.booking.repository.NguoiGiamHoRepository;
import iuh.fit.se.eclinic.booking.service.SuaHoSoBenhNhanService;
import iuh.fit.se.eclinic.booking.util.ChuanHoaTen;
import iuh.fit.se.eclinic.booking.util.KhoaNhanDien;
import iuh.fit.se.eclinic.common.entity.booking.HoSoBenhNhan;
import iuh.fit.se.eclinic.common.exception.LoiKhongTimThay;
import iuh.fit.se.eclinic.common.exception.LoiNghiepVu;
import iuh.fit.se.eclinic.common.exception.MaLoi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SuaHoSoBenhNhanServiceImpl implements SuaHoSoBenhNhanService {

    private final HoSoBenhNhanRepository hoSoBenhNhanRepository;
    private final NguoiGiamHoRepository nguoiGiamHoRepository;

    @Override
    public HoSoBenhNhanQuanTriResponse xem(Long id) {
        return toResponse(hoSoBenhNhanRepository.findById(id)
                .orElseThrow(() -> new LoiKhongTimThay("HoSoBenhNhan", id)));
    }

    @Override
    @Transactional
    public HoSoBenhNhanQuanTriResponse sua(Long id, SuaHoSoBenhNhanRequest request, Long idNguoiSua) {
        // Khoá dòng hồ sơ như lúc đặt lịch: lần đặt đang chạy của cùng bệnh nhân đối chiếu với dữ liệu trước hoặc sau khi sửa
        HoSoBenhNhan hoSo = hoSoBenhNhanRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new LoiKhongTimThay("HoSoBenhNhan", id));
        String cccd = rongThanhNull(request.cccd());
        if (cccd != null && !cccd.equals(hoSo.getCccd())) {
            if (hoSo.getCccd() != null) {
                throw new LoiNghiepVu(MaLoi.DU_LIEU_KHONG_HOP_LE, "Không thể đổi số CCCD của hồ sơ bệnh nhân");
            }
            hoSo.setCccd(cccd);
        }
        hoSo.setHoTen(ChuanHoaTen.gon(request.hoTen()));
        hoSo.setNgaySinh(request.ngaySinh());
        hoSo.setGioiTinh(request.gioiTinh());
        hoSo.setSoDienThoai(request.soDienThoai());
        hoSo.setDiaChi(rongThanhNull(request.diaChi()));
        hoSo.setSoBaoHiemYTe(rongThanhNull(request.soBaoHiemYTe()));
        if (hoSo.getCccd() == null) {
            // Hồ sơ chưa có CCCD được tìm lại bằng khoá nhận diện: tính lại theo họ tên / ngày sinh mới và CCCD người
            // giám hộ đã khai lúc tạo hồ sơ, để lần đặt sau với thông tin đã sửa vẫn về đúng hồ sơ này
            nguoiGiamHoRepository.findFirstByHoSoBenhNhanIdOrderByIdAsc(id)
                    .ifPresent(giamHo -> hoSo.setKhoaNhanDien(
                            KhoaNhanDien.tao(hoSo.getHoTen(), hoSo.getNgaySinh(), giamHo.getCccd())));
        }
        // Flush ngay: số CCCD / khoá nhận diện trùng hồ sơ khác vi phạm UNIQUE -> 409 XUNG_DOT_DU_LIEU
        hoSoBenhNhanRepository.saveAndFlush(hoSo);
        log.info("Tài khoản id={} sửa hồ sơ bệnh nhân id={}", idNguoiSua, id);
        return toResponse(hoSo);
    }

    private static HoSoBenhNhanQuanTriResponse toResponse(HoSoBenhNhan hoSo) {
        return new HoSoBenhNhanQuanTriResponse(hoSo.getId(), hoSo.getCccd(), hoSo.getHoTen(), hoSo.getNgaySinh(),
                hoSo.getGioiTinh(), hoSo.getSoDienThoai(), hoSo.getDiaChi(), hoSo.getSoBaoHiemYTe(),
                hoSo.getTrangThaiLienKet(), hoSo.getTaiKhoan() == null ? null : hoSo.getTaiKhoan().getId(),
                hoSo.getNgayTao());
    }

    private static String rongThanhNull(String chuoi) {
        return chuoi == null || chuoi.isBlank() ? null : chuoi.trim();
    }

}
