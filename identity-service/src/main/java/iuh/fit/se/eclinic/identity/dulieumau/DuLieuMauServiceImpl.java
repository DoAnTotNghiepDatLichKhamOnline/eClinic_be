package iuh.fit.se.eclinic.identity.dulieumau;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import iuh.fit.se.eclinic.common.entity.booking.HoSoBenhNhan;
import iuh.fit.se.eclinic.common.entity.catalog.BacSi;
import iuh.fit.se.eclinic.common.entity.catalog.ChuyenKhoa;
import iuh.fit.se.eclinic.common.entity.catalog.PhongKham;
import iuh.fit.se.eclinic.common.entity.identity.QuanTriVien;
import iuh.fit.se.eclinic.common.entity.identity.TaiKhoan;
import iuh.fit.se.eclinic.common.entity.scheduling.KhungGioKham;
import iuh.fit.se.eclinic.common.entity.scheduling.LichLamViec;
import iuh.fit.se.eclinic.common.enums.TrangThaiBacSi;
import iuh.fit.se.eclinic.common.enums.TrangThaiLichLamViec;
import iuh.fit.se.eclinic.common.enums.TrangThaiLienKet;
import iuh.fit.se.eclinic.common.enums.TrangThaiPhongKham;
import iuh.fit.se.eclinic.common.enums.TrangThaiTaiKhoan;
import iuh.fit.se.eclinic.common.enums.VaiTro;
import iuh.fit.se.eclinic.identity.dulieumau.DuLieuMau.BacSiMau;
import iuh.fit.se.eclinic.identity.dulieumau.DuLieuMau.BenhNhanMau;
import iuh.fit.se.eclinic.identity.dulieumau.DuLieuMau.Buoi;
import iuh.fit.se.eclinic.identity.dulieumau.DuLieuMau.ChuyenKhoaMau;
import iuh.fit.se.eclinic.identity.repository.TaiKhoanRepository;
import iuh.fit.se.eclinic.identity.util.ChuanHoa;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * identity-service không có repository của catalog / booking nên dùng EntityManager trực tiếp.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnBooleanProperty("app.du-lieu-mau.bat")
public class DuLieuMauServiceImpl implements DuLieuMauService {

    private final EntityManager entityManager;
    private final TaiKhoanRepository taiKhoanRepository;
    private final PasswordEncoder passwordEncoder;
    private final DuLieuMauProperties properties;

    /** Ca đang hoạt động của 1 phòng trong 1 ngày, để kiểm tra trùng giờ. */
    private record KhoangGio(LocalTime gioBatDau, LocalTime gioKetThuc) {

        boolean trung(Buoi buoi) {
            return gioBatDau.isBefore(buoi.gioKetThuc()) && gioKetThuc.isAfter(buoi.gioBatDau());
        }
    }

    private record PhongNgay(Long phongKhamId, LocalDate ngay) {
    }

    private record BacSiNgay(Long bacSiId, LocalDate ngay) {
    }

    @Override
    @Transactional
    public boolean taoDuLieuNen() {
        if (taiKhoanRepository.existsByVaiTro(VaiTro.BAC_SI)) {
            return false;
        }
        // Mã hoá 1 lần rồi dùng chung: BCrypt chậm có chủ đích, 18 lần mã hoá làm khởi động chậm thêm vài giây
        String matKhauHash = passwordEncoder.encode(properties.matKhau());

        Map<String, ChuyenKhoa> chuyenKhoaTheoTen = new HashMap<>();
        for (ChuyenKhoaMau mau : DuLieuMau.CHUYEN_KHOA) {
            ChuyenKhoa chuyenKhoa = timHoacTaoChuyenKhoa(mau);
            timHoacTaoPhongKham(mau, chuyenKhoa);
            chuyenKhoaTheoTen.put(mau.ten(), chuyenKhoa);
        }

        for (BacSiMau mau : DuLieuMau.BAC_SI) {
            TaiKhoan taiKhoan = taoTaiKhoan(mau.email(), mau.hoTen(), mau.soDienThoai(), VaiTro.BAC_SI,
                    TrangThaiTaiKhoan.DA_KICH_HOAT, matKhauHash);
            BacSi bacSi = new BacSi();
            bacSi.setTaiKhoan(taiKhoan);
            bacSi.setChuyenKhoa(chuyenKhoaTheoTen.get(mau.chuyenKhoa()));
            bacSi.setSoGiayPhep(mau.soGiayPhep());
            bacSi.setHocVi(mau.hocVi());
            bacSi.setSoNamKinhNghiem(mau.soNamKinhNghiem());
            bacSi.setTieuSu(mau.tieuSu());
            entityManager.persist(bacSi);
        }

        int soHoSo = 0;
        for (BenhNhanMau mau : DuLieuMau.BENH_NHAN) {
            TaiKhoan taiKhoan = taoTaiKhoan(mau.email(), mau.hoTen(), mau.soDienThoai(), VaiTro.BENH_NHAN,
                    mau.trangThai(), matKhauHash);
            taiKhoan.setLyDoVoHieuHoa(mau.lyDoVoHieuHoa());
            if (mau.hoSo() != null) {
                HoSoBenhNhan hoSo = new HoSoBenhNhan();
                hoSo.setTaiKhoan(taiKhoan);
                hoSo.setCccd(mau.cccd());
                hoSo.setHoTen(mau.hoTen());
                hoSo.setNgaySinh(mau.hoSo().ngaySinh());
                hoSo.setGioiTinh(mau.hoSo().gioiTinh());
                hoSo.setSoDienThoai(mau.soDienThoai());
                hoSo.setDiaChi(mau.hoSo().diaChi());
                hoSo.setTrangThaiLienKet(TrangThaiLienKet.DA_LIEN_KET);
                entityManager.persist(hoSo);
                soHoSo++;
            }
        }

        // Đẩy xuống DB ngay để lỗi trùng dữ liệu (email, SĐT, CCCD đã có người dùng) ném ra tại đây
        entityManager.flush();
        log.info("Đã tạo dữ liệu mẫu: {} chuyên khoa, {} bác sĩ, {} tài khoản bệnh nhân ({} hồ sơ bệnh nhân)",
                DuLieuMau.CHUYEN_KHOA.size(), DuLieuMau.BAC_SI.size(), DuLieuMau.BENH_NHAN.size(), soHoSo);
        return true;
    }

    @Override
    @Transactional
    public int boSungLichLamViec(LocalDate tuNgay) {
        QuanTriVien adminTao = entityManager.createQuery("select q from QuanTriVien q order by q.id", QuanTriVien.class)
                .setMaxResults(1).getResultStream().findFirst().orElse(null);
        if (adminTao == null) {
            log.warn("Chưa có quản trị viên nào: không tạo được ca làm việc mẫu");
            return 0;
        }

        Map<String, BacSiMau> mauTheoEmail = DuLieuMau.BAC_SI.stream()
                .collect(Collectors.toMap(BacSiMau::email, Function.identity()));
        // Bác sĩ bị vô hiệu hoá / ngừng công tác không được xếp thêm ca
        List<BacSi> danhSachBacSi = entityManager.createQuery("""
                select b from BacSi b join fetch b.taiKhoan t join fetch b.chuyenKhoa
                where t.email in :emails and t.trangThai = :trangThaiTaiKhoan and b.trangThai = :trangThaiBacSi
                order by b.id
                """, BacSi.class)
                .setParameter("emails", mauTheoEmail.keySet())
                .setParameter("trangThaiTaiKhoan", TrangThaiTaiKhoan.DA_KICH_HOAT)
                .setParameter("trangThaiBacSi", TrangThaiBacSi.DANG_CONG_TAC)
                .getResultList();

        // id bác sĩ -> phòng khám của bác sĩ đó
        Map<Long, PhongKham> phongCuaBacSi = new HashMap<>();
        Map<String, PhongKham> phongTheoChuyenKhoa = timPhongKhamMau();
        for (BacSi bacSi : danhSachBacSi) {
            String tenChuyenKhoa = mauTheoEmail.get(bacSi.getTaiKhoan().getEmail()).chuyenKhoa();
            PhongKham phongKham = phongTheoChuyenKhoa.get(tenChuyenKhoa);
            if (phongKham == null || phongKham.getTrangThai() != TrangThaiPhongKham.HOAT_DONG
                    || !phongKham.getChuyenKhoa().getId().equals(bacSi.getChuyenKhoa().getId())) {
                log.info("Bỏ qua bác sĩ mẫu id={}: không có phòng khám mẫu đang hoạt động cùng chuyên khoa", bacSi.getId());
                continue;
            }
            phongCuaBacSi.put(bacSi.getId(), phongKham);
        }
        if (phongCuaBacSi.isEmpty()) {
            return 0;
        }

        LocalDate denNgay = tuNgay.plusDays(properties.soNgay() - 1L);
        Set<BacSiNgay> ngayDaCoCa = timNgayDaCoCa(phongCuaBacSi.keySet(), tuNgay, denNgay);
        Set<Long> idPhong = phongCuaBacSi.values().stream().map(PhongKham::getId).collect(Collectors.toSet());
        Map<PhongNgay, List<KhoangGio>> caCuaPhong = timCaDangHoatDongCuaPhong(idPhong, tuNgay, denNgay);

        int soCa = 0;
        for (LocalDate ngay = tuNgay; !ngay.isAfter(denNgay); ngay = ngay.plusDays(1)) {
            for (BacSi bacSi : danhSachBacSi) {
                PhongKham phongKham = phongCuaBacSi.get(bacSi.getId());
                Buoi buoi = mauTheoEmail.get(bacSi.getTaiKhoan().getEmail()).nhomCa().buoi(ngay.getDayOfWeek());
                if (phongKham == null || buoi == null || ngayDaCoCa.contains(new BacSiNgay(bacSi.getId(), ngay))) {
                    continue;
                }
                List<KhoangGio> caTrongNgay = caCuaPhong.computeIfAbsent(new PhongNgay(phongKham.getId(), ngay),
                        k -> new ArrayList<>());
                if (caTrongNgay.stream().anyMatch(ca -> ca.trung(buoi))) {
                    continue;
                }
                taoCa(bacSi, phongKham, adminTao, ngay, buoi);
                caTrongNgay.add(new KhoangGio(buoi.gioBatDau(), buoi.gioKetThuc()));
                soCa++;
            }
        }
        return soCa;
    }

    private ChuyenKhoa timHoacTaoChuyenKhoa(ChuyenKhoaMau mau) {
        // DB có thể đã có chuyên khoa cùng tên do admin tạo (tên là UNIQUE): dùng lại, không sửa
        return entityManager.createQuery("select c from ChuyenKhoa c where c.tenChuyenKhoa = :ten", ChuyenKhoa.class)
                .setParameter("ten", mau.ten()).getResultStream().findFirst().orElseGet(() -> {
                    ChuyenKhoa chuyenKhoa = new ChuyenKhoa();
                    chuyenKhoa.setTenChuyenKhoa(mau.ten());
                    chuyenKhoa.setMoTa(mau.moTa());
                    entityManager.persist(chuyenKhoa);
                    return chuyenKhoa;
                });
    }

    private void timHoacTaoPhongKham(ChuyenKhoaMau mau, ChuyenKhoa chuyenKhoa) {
        if (timPhongKham(mau.tenPhong()) != null) {
            return;
        }
        PhongKham phongKham = new PhongKham();
        phongKham.setChuyenKhoa(chuyenKhoa);
        phongKham.setTenPhong(mau.tenPhong());
        phongKham.setTang(mau.tang());
        entityManager.persist(phongKham);
    }

    private PhongKham timPhongKham(String tenPhong) {
        return entityManager.createQuery("select p from PhongKham p join fetch p.chuyenKhoa where p.tenPhong = :ten",
                PhongKham.class).setParameter("ten", tenPhong).getResultStream().findFirst().orElse(null);
    }

    /** Tên chuyên khoa mẫu -> phòng khám mẫu của chuyên khoa đó (không có trong map nếu phòng đã bị xoá). */
    private Map<String, PhongKham> timPhongKhamMau() {
        Map<String, PhongKham> phongTheoChuyenKhoa = new HashMap<>();
        for (ChuyenKhoaMau mau : DuLieuMau.CHUYEN_KHOA) {
            PhongKham phongKham = timPhongKham(mau.tenPhong());
            if (phongKham != null) {
                phongTheoChuyenKhoa.put(mau.ten(), phongKham);
            }
        }
        return phongTheoChuyenKhoa;
    }

    private TaiKhoan taoTaiKhoan(String email, String hoTen, String soDienThoai, VaiTro vaiTro,
            TrangThaiTaiKhoan trangThai, String matKhauHash) {
        TaiKhoan taiKhoan = new TaiKhoan();
        taiKhoan.setEmail(ChuanHoa.email(email));
        taiKhoan.setHoTen(hoTen);
        taiKhoan.setSoDienThoai(soDienThoai);
        taiKhoan.setMatKhauHash(matKhauHash);
        taiKhoan.setVaiTro(vaiTro);
        taiKhoan.setTrangThai(trangThai);
        entityManager.persist(taiKhoan);
        return taiKhoan;
    }

    /** Ngày bác sĩ đã có ca, tính cả ca đã huỷ: ca bị huỷ (xin nghỉ) không được tạo lại. */
    private Set<BacSiNgay> timNgayDaCoCa(Set<Long> idBacSi, LocalDate tuNgay, LocalDate denNgay) {
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

    private Map<PhongNgay, List<KhoangGio>> timCaDangHoatDongCuaPhong(Set<Long> idPhong, LocalDate tuNgay,
            LocalDate denNgay) {
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

    /** 1 ca + các khung giờ 30 phút phủ kín ca (mô hình hiện tại: mỗi khung nhận 1 bệnh nhân). */
    private void taoCa(BacSi bacSi, PhongKham phongKham, QuanTriVien adminTao, LocalDate ngay, Buoi buoi) {
        List<LocalTime> gioBatDauCacKhung = new ArrayList<>();
        for (LocalTime gio = buoi.gioBatDau(); gio.isBefore(buoi.gioKetThuc());
                gio = gio.plusMinutes(DuLieuMau.SO_PHUT_MOI_KHUNG)) {
            gioBatDauCacKhung.add(gio);
        }

        LichLamViec lichLamViec = new LichLamViec();
        lichLamViec.setBacSi(bacSi);
        lichLamViec.setPhongKham(phongKham);
        lichLamViec.setAdminTao(adminTao);
        lichLamViec.setNgayLamViec(ngay);
        lichLamViec.setGioBatDau(buoi.gioBatDau());
        lichLamViec.setGioKetThuc(buoi.gioKetThuc());
        lichLamViec.setSoBenhNhanToiDa(gioBatDauCacKhung.size());
        entityManager.persist(lichLamViec);

        for (LocalTime gio : gioBatDauCacKhung) {
            KhungGioKham khungGio = new KhungGioKham();
            khungGio.setLichLamViec(lichLamViec);
            khungGio.setGioBatDau(ngay.atTime(gio));
            khungGio.setGioKetThuc(ngay.atTime(gio.plusMinutes(DuLieuMau.SO_PHUT_MOI_KHUNG)));
            entityManager.persist(khungGio);
        }
    }

}
