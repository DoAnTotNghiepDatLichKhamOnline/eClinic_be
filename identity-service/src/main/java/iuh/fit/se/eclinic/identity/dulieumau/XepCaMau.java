package iuh.fit.se.eclinic.identity.dulieumau;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import iuh.fit.se.eclinic.common.entity.catalog.BacSi;
import iuh.fit.se.eclinic.common.entity.catalog.PhongKham;
import iuh.fit.se.eclinic.common.entity.identity.QuanTriVien;
import iuh.fit.se.eclinic.common.entity.scheduling.KhungGioKham;
import iuh.fit.se.eclinic.common.entity.scheduling.LichLamViec;
import iuh.fit.se.eclinic.common.entity.scheduling.LichSuCaLamViec;
import iuh.fit.se.eclinic.common.enums.HanhDongCa;
import iuh.fit.se.eclinic.common.enums.NguonThayDoiCa;
import iuh.fit.se.eclinic.common.enums.TrangThaiLichLamViec;
import iuh.fit.se.eclinic.common.util.ChiaCaLamViec;
import iuh.fit.se.eclinic.identity.dulieumau.DuLieuMau.Buoi;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

/** Xếp ca mẫu: dùng chung cho dữ liệu mẫu nhỏ và dữ liệu mẫu lớn. */
@Component
@RequiredArgsConstructor
class XepCaMau {

    private final EntityManager entityManager;

    /** Ca đang hoạt động của 1 phòng trong 1 ngày, để kiểm tra trùng giờ. */
    record KhoangGio(LocalTime gioBatDau, LocalTime gioKetThuc) {

        boolean trung(Buoi buoi) {
            return gioBatDau.isBefore(buoi.gioKetThuc()) && gioKetThuc.isAfter(buoi.gioBatDau());
        }
    }

    record PhongNgay(Long phongKhamId, LocalDate ngay) {
    }

    record BacSiNgay(Long bacSiId, LocalDate ngay) {
    }

    /** Ngày bác sĩ đã có ca, tính cả ca đã huỷ: ca bị huỷ (xin nghỉ) không được tạo lại. */
    Set<BacSiNgay> timNgayDaCoCa(Set<Long> idBacSi, LocalDate tuNgay, LocalDate denNgay) {
        List<Object[]> dong = entityManager.createQuery("""
                select l.bacSi.id, l.ngayLamViec from LichLamViec l
                where l.bacSi.id in :idBacSi and l.ngayLamViec between :tuNgay and :denNgay
                """, Object[].class)
                .setParameter("idBacSi", idBacSi)
                .setParameter("tuNgay", tuNgay)
                .setParameter("denNgay", denNgay)
                .getResultList();
        Set<BacSiNgay> ngayDaCoCa = new HashSet<>();
        for (Object[] cot : dong) {
            ngayDaCoCa.add(new BacSiNgay((Long) cot[0], (LocalDate) cot[1]));
        }
        return ngayDaCoCa;
    }

    Map<PhongNgay, List<KhoangGio>> timCaDangHoatDongCuaPhong(Set<Long> idPhong, LocalDate tuNgay, LocalDate denNgay) {
        List<Object[]> dong = entityManager.createQuery("""
                select l.phongKham.id, l.ngayLamViec, l.gioBatDau, l.gioKetThuc from LichLamViec l
                where l.phongKham.id in :idPhong and l.ngayLamViec between :tuNgay and :denNgay
                  and l.trangThai = :trangThai
                """, Object[].class)
                .setParameter("idPhong", idPhong)
                .setParameter("tuNgay", tuNgay)
                .setParameter("denNgay", denNgay)
                .setParameter("trangThai", TrangThaiLichLamViec.HOAT_DONG)
                .getResultList();
        Map<PhongNgay, List<KhoangGio>> caCuaPhong = new HashMap<>();
        for (Object[] cot : dong) {
            caCuaPhong.computeIfAbsent(new PhongNgay((Long) cot[0], (LocalDate) cot[1]), k -> new ArrayList<>())
                    .add(new KhoangGio((LocalTime) cot[2], (LocalTime) cot[3]));
        }
        return caCuaPhong;
    }

    /** 1 ca + các lượt khám của ca: mỗi khung 1 giờ N lượt, mỗi lượt t phút, mỗi lượt là 1 dòng khung giờ. */
    void taoCa(BacSi bacSi, PhongKham phongKham, QuanTriVien adminTao, LocalDate ngay, Buoi buoi) {
        List<LocalTime> gioBatDauCacLuot = ChiaCaLamViec.gioBatDauCacLuot(buoi.gioBatDau(), buoi.gioKetThuc(),
                DuLieuMau.SO_LUOT_MOI_GIO, DuLieuMau.SO_PHUT_MOI_LUOT);

        LichLamViec lichLamViec = new LichLamViec();
        lichLamViec.setBacSi(bacSi);
        lichLamViec.setPhongKham(phongKham);
        lichLamViec.setAdminTao(adminTao);
        lichLamViec.setNgayLamViec(ngay);
        lichLamViec.setGioBatDau(buoi.gioBatDau());
        lichLamViec.setGioKetThuc(buoi.gioKetThuc());
        lichLamViec.setSoBenhNhanToiDa(gioBatDauCacLuot.size());
        lichLamViec.setSoLuotToiDaMoiGio(DuLieuMau.SO_LUOT_MOI_GIO);
        lichLamViec.setThoiLuongLuotPhut(DuLieuMau.SO_PHUT_MOI_LUOT);
        entityManager.persist(lichLamViec);
        // Dòng đầu của lịch sử ca: ca nào cũng bắt đầu bằng TAO
        entityManager.persist(
                LichSuCaLamViec.anhCua(lichLamViec, HanhDongCa.TAO, NguonThayDoiCa.DU_LIEU_MAU, adminTao));

        for (LocalTime gio : gioBatDauCacLuot) {
            KhungGioKham khungGio = new KhungGioKham();
            khungGio.setLichLamViec(lichLamViec);
            khungGio.setGioBatDau(ngay.atTime(gio));
            khungGio.setGioKetThuc(ngay.atTime(gio.plusMinutes(DuLieuMau.SO_PHUT_MOI_LUOT)));
            entityManager.persist(khungGio);
        }
    }

}
