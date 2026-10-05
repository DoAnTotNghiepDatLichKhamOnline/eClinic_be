package iuh.fit.se.eclinic.booking.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import iuh.fit.se.eclinic.booking.dto.request.BenhNhanRequest;
import iuh.fit.se.eclinic.booking.dto.request.NguoiGiamHoRequest;
import iuh.fit.se.eclinic.booking.dto.response.NguoiThanDaLuuResponse;
import iuh.fit.se.eclinic.booking.event.DaDatLichChoNguoiThanEvent;
import iuh.fit.se.eclinic.booking.repository.NguoiThanDaLuuRepository;
import iuh.fit.se.eclinic.booking.service.NguoiThanDaLuuService;
import iuh.fit.se.eclinic.booking.service.TaiKhoanService;
import iuh.fit.se.eclinic.booking.util.ChuanHoaTen;
import iuh.fit.se.eclinic.common.entity.booking.HoSoBenhNhan;
import iuh.fit.se.eclinic.common.entity.booking.NguoiThanDaLuu;
import iuh.fit.se.eclinic.common.entity.identity.TaiKhoan;
import iuh.fit.se.eclinic.common.exception.LoiKhongTimThay;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NguoiThanDaLuuServiceImpl implements NguoiThanDaLuuService {

    private final NguoiThanDaLuuRepository nguoiThanDaLuuRepository;
    private final TaiKhoanService taiKhoanService;
    private final EntityManager entityManager;

    /**
     * REQUIRES_NEW: được gọi trong callback sau commit của transaction đặt lịch, lúc đó transaction cũ không còn dùng
     * được. 2 lần đặt cùng lúc cho cùng 1 người thân: lần sau vi phạm UNIQUE (tài khoản, hồ sơ) và bị bên gọi bỏ qua.
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void ghiNho(DaDatLichChoNguoiThanEvent event) {
        NguoiThanDaLuu nguoiThan = nguoiThanDaLuuRepository
                .findByTaiKhoanIdAndHoSoBenhNhanId(event.idTaiKhoan(), event.idHoSoBenhNhan())
                .orElseGet(() -> {
                    NguoiThanDaLuu moi = new NguoiThanDaLuu();
                    moi.setTaiKhoan(entityManager.getReference(TaiKhoan.class, event.idTaiKhoan()));
                    moi.setHoSoBenhNhan(entityManager.getReference(HoSoBenhNhan.class, event.idHoSoBenhNhan()));
                    return moi;
                });
        BenhNhanRequest benhNhan = event.benhNhan();
        nguoiThan.setHoTen(ChuanHoaTen.gon(benhNhan.hoTen()));
        nguoiThan.setNgaySinh(benhNhan.ngaySinh());
        nguoiThan.setGioiTinh(benhNhan.gioiTinh());
        nguoiThan.setCccd(rongThanhNull(benhNhan.cccd()));
        nguoiThan.setSoDienThoai(benhNhan.soDienThoai());
        nguoiThan.setEmail(rongThanhNull(benhNhan.email()));
        nguoiThan.setDiaChi(rongThanhNull(benhNhan.diaChi()));
        nguoiThan.setSoBaoHiemYTe(rongThanhNull(benhNhan.soBaoHiemYTe()));

        NguoiGiamHoRequest giamHo = event.nguoiGiamHo();
        nguoiThan.setGiamHoHoTen(giamHo == null ? null : ChuanHoaTen.gon(giamHo.hoTen()));
        nguoiThan.setGiamHoQuanHe(giamHo == null ? null : giamHo.quanHe());
        nguoiThan.setGiamHoSoDienThoai(giamHo == null ? null : giamHo.soDienThoai());
        nguoiThan.setGiamHoCccd(giamHo == null ? null : giamHo.cccd());
        nguoiThan.setGiamHoNgaySinh(giamHo == null ? null : giamHo.ngaySinh());
        nguoiThan.setLanDungCuoi(LocalDateTime.now());
        nguoiThanDaLuuRepository.saveAndFlush(nguoiThan);

        // Giữ SO_NGUOI_TOI_DA người đặt lịch gần nhất
        List<NguoiThanDaLuu> tatCa = nguoiThanDaLuuRepository
                .findByTaiKhoanIdOrderByLanDungCuoiDescIdDesc(event.idTaiKhoan());
        if (tatCa.size() > SO_NGUOI_TOI_DA) {
            nguoiThanDaLuuRepository.deleteAll(tatCa.subList(SO_NGUOI_TOI_DA, tatCa.size()));
        }
    }

    @Override
    public List<NguoiThanDaLuuResponse> cuaTaiKhoan(Long idTaiKhoan) {
        return nguoiThanDaLuuRepository.findByTaiKhoanIdOrderByLanDungCuoiDescIdDesc(idTaiKhoan).stream()
                .map(NguoiThanDaLuuServiceImpl::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public void xoa(Long idTaiKhoan, Long id) {
        taiKhoanService.layBenhNhanDangHoatDong(idTaiKhoan);
        NguoiThanDaLuu nguoiThan = nguoiThanDaLuuRepository.findByIdAndTaiKhoanId(id, idTaiKhoan)
                .orElseThrow(() -> new LoiKhongTimThay("NguoiThanDaLuu", id));
        nguoiThanDaLuuRepository.delete(nguoiThan);
    }

    private static NguoiThanDaLuuResponse toResponse(NguoiThanDaLuu nguoiThan) {
        NguoiThanDaLuuResponse.NguoiGiamHo giamHo = nguoiThan.getGiamHoHoTen() == null ? null
                : new NguoiThanDaLuuResponse.NguoiGiamHo(nguoiThan.getGiamHoHoTen(), nguoiThan.getGiamHoQuanHe(),
                        nguoiThan.getGiamHoSoDienThoai(), nguoiThan.getGiamHoCccd(), nguoiThan.getGiamHoNgaySinh());
        return new NguoiThanDaLuuResponse(nguoiThan.getId(), nguoiThan.getHoTen(), nguoiThan.getNgaySinh(),
                nguoiThan.getGioiTinh(), nguoiThan.getCccd(), nguoiThan.getSoDienThoai(), nguoiThan.getEmail(),
                nguoiThan.getDiaChi(), nguoiThan.getSoBaoHiemYTe(), giamHo, nguoiThan.getLanDungCuoi());
    }

    private static String rongThanhNull(String chuoi) {
        return chuoi == null || chuoi.isBlank() ? null : chuoi.trim();
    }

}
