package iuh.fit.se.eclinic.catalog.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import iuh.fit.se.eclinic.catalog.dto.request.PhongKhamRequest;
import iuh.fit.se.eclinic.catalog.dto.response.PhongKhamQuanTriResponse;
import iuh.fit.se.eclinic.catalog.repository.ChuyenKhoaRepository;
import iuh.fit.se.eclinic.catalog.repository.LichLamViecChiDocRepository;
import iuh.fit.se.eclinic.catalog.repository.LichLamViecChiDocRepository.SoCaTheoPhong;
import iuh.fit.se.eclinic.catalog.repository.PhongKhamRepository;
import iuh.fit.se.eclinic.catalog.service.QuanLyPhongKhamService;
import iuh.fit.se.eclinic.common.dto.TrangDuLieu;
import iuh.fit.se.eclinic.common.entity.catalog.ChuyenKhoa;
import iuh.fit.se.eclinic.common.entity.catalog.PhongKham;
import iuh.fit.se.eclinic.common.enums.TrangThaiPhongKham;
import iuh.fit.se.eclinic.common.exception.LoiKhongTimThay;
import iuh.fit.se.eclinic.common.exception.LoiNghiepVu;
import iuh.fit.se.eclinic.common.exception.MaLoi;
import iuh.fit.se.eclinic.common.util.MauTimKiem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuanLyPhongKhamServiceImpl implements QuanLyPhongKhamService {

    private final PhongKhamRepository phongKhamRepository;
    private final ChuyenKhoaRepository chuyenKhoaRepository;
    private final LichLamViecChiDocRepository lichLamViecChiDocRepository;

    @Override
    public TrangDuLieu<PhongKhamQuanTriResponse> danhSach(String tuKhoa, Long idChuyenKhoa,
            TrangThaiPhongKham trangThai, int trang, int kichThuoc) {
        Page<PhongKham> ketQua = phongKhamRepository.timChoQuanTri(MauTimKiem.chua(tuKhoa), idChuyenKhoa, trangThai,
                PageRequest.of(trang, kichThuoc));
        List<Long> idCacPhong = ketQua.getContent().stream().map(PhongKham::getId).toList();
        LocalDateTime bayGio = LocalDateTime.now();
        Map<Long, Long> soCa = idCacPhong.isEmpty() ? Map.of()
                : lichLamViecChiDocRepository
                        .demCaSapToiTheoPhong(idCacPhong, bayGio.toLocalDate(), bayGio.toLocalTime()).stream()
                        .collect(Collectors.toMap(SoCaTheoPhong::getIdPhongKham, SoCaTheoPhong::getSoCa));
        return TrangDuLieu.tu(ketQua.map(phong -> toResponse(phong, soCa.getOrDefault(phong.getId(), 0L))));
    }

    @Override
    @Transactional
    public PhongKhamQuanTriResponse them(PhongKhamRequest request) {
        String tenPhong = request.tenPhong().trim();
        ChuyenKhoa chuyenKhoa = timChuyenKhoa(request.idChuyenKhoa());
        if (phongKhamRepository.existsByTenPhong(tenPhong)) {
            throw new LoiNghiepVu(MaLoi.TEN_PHONG_DA_TON_TAI);
        }
        PhongKham phong = new PhongKham();
        phong.setTenPhong(tenPhong);
        phong.setTang(rongThanhNull(request.tang()));
        phong.setChuyenKhoa(chuyenKhoa);
        phong.setTrangThai(TrangThaiPhongKham.HOAT_DONG);
        phongKhamRepository.saveAndFlush(phong);
        log.info("Thêm phòng khám id={} ({})", phong.getId(), tenPhong);
        return toResponse(phong, 0);
    }

    @Override
    @Transactional
    public PhongKhamQuanTriResponse sua(Long id, PhongKhamRequest request) {
        // Khoá dòng phòng khám TRƯỚC mọi câu đọc khác: xem khoaVaDemCa
        PhongKham phong = khoa(id);
        long soCaSapToi = demCaSapToi(id);
        String tenPhong = request.tenPhong().trim();
        // Kiểm tra trước khi sửa entity đang được quản lý (xem ChuyenKhoaServiceImpl.capNhat)
        if (phongKhamRepository.existsByTenPhongAndIdNot(tenPhong, id)) {
            throw new LoiNghiepVu(MaLoi.TEN_PHONG_DA_TON_TAI);
        }
        if (!phong.getChuyenKhoa().getId().equals(request.idChuyenKhoa())) {
            ChuyenKhoa chuyenKhoaMoi = timChuyenKhoa(request.idChuyenKhoa());
            if (soCaSapToi > 0) {
                throw new LoiNghiepVu(MaLoi.PHONG_CON_CA_LAM_VIEC, "Phòng khám còn " + soCaSapToi
                        + " ca làm việc sắp tới nên không đổi chuyên khoa được; hãy chuyển các ca đó sang phòng khác trước");
            }
            phong.setChuyenKhoa(chuyenKhoaMoi);
        }
        phong.setTenPhong(tenPhong);
        phong.setTang(rongThanhNull(request.tang()));
        phongKhamRepository.saveAndFlush(phong);
        log.info("Sửa phòng khám id={}", id);
        return toResponse(phong, soCaSapToi);
    }

    @Override
    @Transactional
    public PhongKhamQuanTriResponse ngungHoatDong(Long id) {
        PhongKham phong = khoa(id);
        long soCaSapToi = demCaSapToi(id);
        if (phong.getTrangThai() == TrangThaiPhongKham.NGUNG_HOAT_DONG) {
            return toResponse(phong, soCaSapToi);
        }
        if (soCaSapToi > 0) {
            throw new LoiNghiepVu(MaLoi.PHONG_CON_CA_LAM_VIEC, "Phòng khám còn " + soCaSapToi
                    + " ca làm việc sắp tới; hãy chuyển các ca đó sang phòng khác trước khi cho phòng ngừng hoạt động");
        }
        phong.setTrangThai(TrangThaiPhongKham.NGUNG_HOAT_DONG);
        log.info("Phòng khám id={} ngừng hoạt động", id);
        return toResponse(phong, 0);
    }

    @Override
    @Transactional
    public PhongKhamQuanTriResponse hoatDongLai(Long id) {
        PhongKham phong = khoa(id);
        long soCaSapToi = demCaSapToi(id);
        if (phong.getTrangThai() != TrangThaiPhongKham.HOAT_DONG) {
            phong.setTrangThai(TrangThaiPhongKham.HOAT_DONG);
            log.info("Phòng khám id={} hoạt động lại", id);
        }
        return toResponse(phong, soCaSapToi);
    }

    /**
     * booking-service khoá cùng dòng phòng khám này khi tạo ca / đổi phòng của ca, nên sau khi lấy được khoá thì không ca
     * nào đang được xếp dở vào phòng. Phải là câu lệnh đầu tiên của transaction: câu đọc thường đầu tiên (đếm ca) chốt
     * snapshot của transaction, đọc trước khi khoá thì không thấy ca vừa được commit trong lúc chờ khoá.
     */
    private PhongKham khoa(Long id) {
        return phongKhamRepository.findByIdForUpdate(id).orElseThrow(() -> new LoiKhongTimThay("PhongKham", id));
    }

    private long demCaSapToi(Long idPhongKham) {
        LocalDateTime bayGio = LocalDateTime.now();
        return lichLamViecChiDocRepository.demCaSapToiCuaPhong(idPhongKham, bayGio.toLocalDate(),
                bayGio.toLocalTime());
    }

    private ChuyenKhoa timChuyenKhoa(Long id) {
        return chuyenKhoaRepository.findById(id).orElseThrow(() -> new LoiKhongTimThay("ChuyenKhoa", id));
    }

    private static PhongKhamQuanTriResponse toResponse(PhongKham phong, long soCaSapToi) {
        return new PhongKhamQuanTriResponse(phong.getId(), phong.getTenPhong(), phong.getTang(),
                phong.getChuyenKhoa().getId(), phong.getChuyenKhoa().getTenChuyenKhoa(), phong.getTrangThai(),
                soCaSapToi);
    }

    private static String rongThanhNull(String chuoi) {
        return chuoi == null || chuoi.isBlank() ? null : chuoi.trim();
    }
}
