package iuh.fit.se.eclinic.identity.dulieumau;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import iuh.fit.se.eclinic.common.entity.booking.HoSoBenhNhan;
import iuh.fit.se.eclinic.common.entity.booking.LichHen;
import iuh.fit.se.eclinic.common.entity.booking.LichSuLichHen;
import iuh.fit.se.eclinic.common.entity.booking.NguoiThanDaLuu;
import iuh.fit.se.eclinic.common.entity.booking.SuKienThongBao;
import iuh.fit.se.eclinic.common.entity.catalog.BacSi;
import iuh.fit.se.eclinic.common.entity.catalog.ChuyenKhoa;
import iuh.fit.se.eclinic.common.entity.catalog.PhongKham;
import iuh.fit.se.eclinic.common.entity.chatbot.PhienChat;
import iuh.fit.se.eclinic.common.entity.chatbot.TinNhanChat;
import iuh.fit.se.eclinic.common.entity.identity.QuanTriVien;
import iuh.fit.se.eclinic.common.entity.identity.RefreshToken;
import iuh.fit.se.eclinic.common.entity.identity.TaiKhoan;
import iuh.fit.se.eclinic.common.entity.medical.ChiTietDonThuoc;
import iuh.fit.se.eclinic.common.entity.medical.HoSoBenhAn;
import iuh.fit.se.eclinic.common.entity.medical.Thuoc;
import iuh.fit.se.eclinic.common.entity.notification.EmailGui;
import iuh.fit.se.eclinic.common.entity.notification.ThongBao;
import iuh.fit.se.eclinic.common.entity.scheduling.KhungGioKham;
import iuh.fit.se.eclinic.common.entity.scheduling.LichLamViec;
import iuh.fit.se.eclinic.common.entity.scheduling.NgayNghiBenhVien;
import iuh.fit.se.eclinic.common.entity.scheduling.YeuCauDoiLich;
import iuh.fit.se.eclinic.common.enums.CapDoQuyen;
import iuh.fit.se.eclinic.common.enums.DuongDungThuoc;
import iuh.fit.se.eclinic.common.enums.GioiTinh;
import iuh.fit.se.eclinic.common.enums.KenhDatLich;
import iuh.fit.se.eclinic.common.enums.LoaiNguoiThucHien;
import iuh.fit.se.eclinic.common.enums.LoaiThongBao;
import iuh.fit.se.eclinic.common.enums.LoaiTinNhanChat;
import iuh.fit.se.eclinic.common.enums.LoaiYeuCauDoiLich;
import iuh.fit.se.eclinic.common.enums.NgonNgu;
import iuh.fit.se.eclinic.common.enums.NguoiGui;
import iuh.fit.se.eclinic.common.enums.SuKienLichHen;
import iuh.fit.se.eclinic.common.enums.TrangThaiBacSi;
import iuh.fit.se.eclinic.common.enums.TrangThaiEmail;
import iuh.fit.se.eclinic.common.enums.TrangThaiHangDoi;
import iuh.fit.se.eclinic.common.enums.TrangThaiLichHen;
import iuh.fit.se.eclinic.common.enums.TrangThaiLienKet;
import iuh.fit.se.eclinic.common.enums.TrangThaiPhienChat;
import iuh.fit.se.eclinic.common.enums.TrangThaiPhongKham;
import iuh.fit.se.eclinic.common.enums.TrangThaiTaiKhoan;
import iuh.fit.se.eclinic.common.enums.TrangThaiThuoc;
import iuh.fit.se.eclinic.common.enums.TrangThaiYeuCau;
import iuh.fit.se.eclinic.common.enums.VaiTro;
import iuh.fit.se.eclinic.common.util.DanhMucIcd10;
import iuh.fit.se.eclinic.common.util.TokenNgauNhien;
import iuh.fit.se.eclinic.identity.dulieumau.DuLieuMau.BacSiMau;
import iuh.fit.se.eclinic.identity.dulieumau.DuLieuMau.Buoi;
import iuh.fit.se.eclinic.identity.dulieumau.DuLieuMau.ChuyenKhoaMau;
import iuh.fit.se.eclinic.identity.dulieumau.DuLieuMauLon.ChuyenKhoaLon;
import iuh.fit.se.eclinic.identity.dulieumau.DuLieuMauLon.LanKhamLon;
import iuh.fit.se.eclinic.identity.dulieumau.DuLieuMauLon.NhomThuocLon;
import iuh.fit.se.eclinic.identity.dulieumau.XepCaMau.BacSiNgay;
import iuh.fit.se.eclinic.identity.dulieumau.XepCaMau.KhoangGio;
import iuh.fit.se.eclinic.identity.dulieumau.XepCaMau.PhongNgay;
import iuh.fit.se.eclinic.identity.repository.TaiKhoanRepository;
import iuh.fit.se.eclinic.identity.util.ChuanHoa;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Giống {@link DuLieuMauServiceImpl}, dùng EntityManager vì identity-service không có repository của service khác.
 * Thực thể chỉ sống trong từng bước: giữa các bước gọi {@link #xoaBoNho()} và chỉ giữ lại id, để 512MB của service
 * không bị chiếm bởi hàng chục nghìn thực thể đang quản lý.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnBooleanProperty({ "app.du-lieu-mau.bat", "app.du-lieu-mau.lon" })
public class DuLieuMauLonServiceImpl implements DuLieuMauLonService {

    private static final int SO_CA_QUA_KHU_MOI_BAC_SI = 4;
    /** Lùi xa nhất khi tìm ngày trống cho ca quá khứ: DB cũ có thể đã có ca ở những tuần gần đây. */
    private static final int SO_TUAN_LUI_TOI_DA = 40;
    private static final int SO_LICH_HEN_BAC_SI_DAU = 60;
    private static final int SO_LICH_HEN_BAC_SI_SAU = 70;
    private static final int SO_LICH_HEN_BENH_NHAN_DAU = 40;
    private static final int SO_LICH_HEN_SAP_TOI = 20;
    private static final int SO_DANH_GIA = 65;
    private static final int SO_YEU_CAU_DA_DONG = 90;
    private static final int SO_YEU_CAU_CHO_DUYET = 10;
    private static final int SO_DONG_MOI_BANG = 100;
    private static final int SO_PHIEN_CHAT_CUA_TAI_KHOAN = 70;
    private static final Random NGAU_NHIEN_CO_DINH = new Random(20);

    private final EntityManager entityManager;
    private final TaiKhoanRepository taiKhoanRepository;
    private final PasswordEncoder passwordEncoder;
    private final DuLieuMauProperties properties;
    private final XepCaMau xepCaMau;

    private record ChuyenKhoaDaCo(Long id, String ten, String tenEn) {
    }

    private record BacSiLon(Long id, int so, Long idPhong) {
    }

    private record BenhNhanLon(Long idHoSo, Long idTaiKhoan, String hoTen, LocalDate ngaySinh,
            GioiTinh gioiTinh, String soDienThoai, String email) {
    }

    private record LuotLon(Long id, Long idCa, Long idBacSi, Long idPhong, LocalDateTime gioBatDau) {
    }

    private record CaQuaKhu(Long id, Long idBacSi, LocalDate ngay) {
    }

    private record DanhGiaLon(Long idLichHen, Long idTaiKhoan, LocalDateTime gioKham) {
    }

    /** Id lớn nhất của các bảng trước khi chạy: mọi câu UPDATE cuối chỉ chạm dòng do bước này tạo. */
    private record Moc(long lichLamViec, long lichHen, long hoSoBenhAn, long lichSu, long thongBao, long yeuCau,
            long tinNhan) {
    }

    @Override
    @Transactional
    public boolean taoDuLieuLon() {
        if (taiKhoanRepository.existsByEmail(DuLieuMauLon.emailDanhDau())) {
            return false;
        }
        Long idAdmin = timIdAdmin();
        if (idAdmin == null) {
            log.warn("Chưa có quản trị viên nào: không tạo được dữ liệu mẫu lớn");
            return false;
        }
        LocalDate homNay = LocalDate.now();
        Moc moc = docMoc();
        // Mã hoá 1 lần rồi dùng chung: BCrypt chậm có chủ đích
        String matKhauHash = passwordEncoder.encode(properties.matKhau());

        List<ChuyenKhoaDaCo> chuyenKhoa = taoChuyenKhoa();
        taoPhongVaBacSi(chuyenKhoa, matKhauHash);
        List<BenhNhanLon> benhNhanMoi = taoBenhNhan(matKhauHash);
        taoNguoiThan(benhNhanMoi);
        taoQuanTri();
        taoThuoc();
        taoNgayNghi(idAdmin, homNay);
        xoaBoNho();

        int soCaToi = xepCaSapToi(idAdmin, homNay);
        int soCaTruoc = taoCaQuaKhu(idAdmin, homNay);
        List<BenhNhanLon> benhNhanDau = timBenhNhanDau();
        taoLichHenQuaKhu(moc, idAdmin, homNay, benhNhanDau, benhNhanMoi);
        taoLichHenSapToi(moc, homNay, benhNhanMoi);
        taoYeuCauDoiLich(moc, idAdmin, homNay);

        List<BenhNhanLon> tatCa = new ArrayList<>(benhNhanDau);
        tatCa.addAll(benhNhanMoi);
        taoThongBao(tatCa);
        taoHangChoGui(tatCa);
        taoPhienDangNhap(tatCa);
        taoPhienChat(tatCa);
        xoaBoNho();

        tatNhacLich(moc);
        luiThoiGian(moc, homNay);
        log.info("Dữ liệu mẫu lớn: {} ca sắp tới, {} ca đã qua, {} bệnh nhân mới", soCaToi, soCaTruoc, benhNhanMoi.size());
        return true;
    }

    @Override
    @Transactional
    public int boSungLichLamViec(LocalDate tuNgay) {
        Long idAdmin = timIdAdmin();
        return idAdmin == null ? 0 : xepCaSapToi(idAdmin, tuNgay);
    }

    private Long timIdAdmin() {
        return entityManager.createQuery("select q.id from QuanTriVien q order by q.id", Long.class)
                .setMaxResults(1).getResultStream().findFirst().orElse(null);
    }

    private Moc docMoc() {
        return new Moc(idLonNhat("lich_lam_viec", "id_lich_lam_viec"), idLonNhat("lich_hen", "id_lich_hen"),
                idLonNhat("ho_so_benh_an", "id_ho_so_benh_an"), idLonNhat("lich_su_thay_doi", "id_lich_su"),
                idLonNhat("thong_bao", "id_thong_bao"), idLonNhat("yeu_cau_doi_lich", "id_yeu_cau"),
                idLonNhat("tin_nhan_chat", "id_tin_nhan"));
    }

    private long idLonNhat(String bang, String cot) {
        return ((Number) entityManager.createNativeQuery("SELECT COALESCE(MAX(%s), 0) FROM %s".formatted(cot, bang))
                .getSingleResult()).longValue();
    }

    /** Ghi xuống DB rồi bỏ mọi thực thể đang quản lý. Sau lời gọi này chỉ dùng id + getReference. */
    private void xoaBoNho() {
        entityManager.flush();
        entityManager.clear();
    }

    /** 8 chuyên khoa mẫu đang có + 22 chuyên khoa mới; DB đã có cùng tên thì dùng lại, không sửa. */
    private List<ChuyenKhoaDaCo> taoChuyenKhoa() {
        List<ChuyenKhoaDaCo> danhSach = new ArrayList<>();
        for (ChuyenKhoaMau mau : DuLieuMau.CHUYEN_KHOA) {
            ChuyenKhoa daCo = timChuyenKhoa(mau.ten());
            if (daCo != null) {
                danhSach.add(new ChuyenKhoaDaCo(daCo.getId(), mau.ten(), DuLieuMauTiengAnh.CHUYEN_KHOA.get(mau.ten()).ten()));
            }
        }
        for (ChuyenKhoaLon mau : DuLieuMauLon.CHUYEN_KHOA) {
            ChuyenKhoa chuyenKhoa = timChuyenKhoa(mau.ten());
            if (chuyenKhoa == null) {
                chuyenKhoa = new ChuyenKhoa();
                chuyenKhoa.setTenChuyenKhoa(mau.ten());
                chuyenKhoa.setTenChuyenKhoaEn(mau.tenEn());
                chuyenKhoa.setMoTa(mau.moTa());
                chuyenKhoa.setMoTaEn(mau.moTaEn());
                chuyenKhoa.setTrieuChung(mau.trieuChung());
                chuyenKhoa.setTrieuChungEn(mau.trieuChungEn());
                entityManager.persist(chuyenKhoa);
            }
            danhSach.add(new ChuyenKhoaDaCo(chuyenKhoa.getId(), mau.ten(), mau.tenEn()));
        }
        return danhSach;
    }

    private ChuyenKhoa timChuyenKhoa(String ten) {
        return entityManager.createQuery("select c from ChuyenKhoa c where c.tenChuyenKhoa = :ten", ChuyenKhoa.class)
                .setParameter("ten", ten).getResultStream().findFirst().orElse(null);
    }

    /** Mỗi bác sĩ mới 1 phòng riêng cùng chuyên khoa, thêm vài phòng trống. */
    private void taoPhongVaBacSi(List<ChuyenKhoaDaCo> chuyenKhoa, String matKhauHash) {
        long soDaCo = entityManager.createQuery("select count(b) from BacSi b", Long.class).getSingleResult();
        int soCanThem = Math.max(0, DuLieuMauLon.SO_BAC_SI_TOI_DA - (int) soDaCo);
        int soDau = DuLieuMau.BAC_SI.size() + 1;
        for (int so = soDau; so < soDau + soCanThem; so++) {
            ChuyenKhoaDaCo ck = chuyenKhoa.get((so - 1) % chuyenKhoa.size());
            taoPhong(DuLieuMauLon.soPhong(so), ck, TrangThaiPhongKham.HOAT_DONG);
            taoBacSi(so, ck, matKhauHash);
        }
        for (int soPhong : DuLieuMauLon.PHONG_DU_PHONG) {
            taoPhong(soPhong, chuyenKhoa.get(soPhong % chuyenKhoa.size()),
                    soPhong == DuLieuMauLon.PHONG_NGUNG_HOAT_DONG ? TrangThaiPhongKham.NGUNG_HOAT_DONG
                            : TrangThaiPhongKham.HOAT_DONG);
        }
        xoaBoNho();
    }

    private void taoPhong(int soPhong, ChuyenKhoaDaCo chuyenKhoa, TrangThaiPhongKham trangThai) {
        int soTang = DuLieuMauLon.soTang(soPhong);
        PhongKham phongKham = new PhongKham();
        phongKham.setChuyenKhoa(entityManager.getReference(ChuyenKhoa.class, chuyenKhoa.id()));
        phongKham.setTenPhong(DuLieuMauLon.tenPhong(soPhong));
        phongKham.setTenPhongEn(DuLieuMauLon.tenPhongEn(soPhong));
        phongKham.setTang("Tầng " + soTang);
        phongKham.setTangEn("Floor " + soTang);
        phongKham.setTrangThai(trangThai);
        entityManager.persist(phongKham);
    }

    private void taoBacSi(int so, ChuyenKhoaDaCo ck, String matKhauHash) {
        String hoTen = DuLieuMauLon.hoTen(so);
        TaiKhoan taiKhoan = taoTaiKhoan(DuLieuMauLon.emailBacSi(so), hoTen, DuLieuMauLon.soDienThoaiBacSi(so),
                VaiTro.BAC_SI, TrangThaiTaiKhoan.DA_KICH_HOAT, matKhauHash);
        taiKhoan.setAnhDaiDien(DuLieuMauLon.anhDaiDien(so));

        int soNamKinhNghiem = 3 + so % 25;
        int namBatDau = LocalDate.now().getYear() - soNamKinhNghiem;
        BacSi bacSi = new BacSi();
        bacSi.setTaiKhoan(taiKhoan);
        bacSi.setChuyenKhoa(entityManager.getReference(ChuyenKhoa.class, ck.id()));
        bacSi.setSoGiayPhep(DuLieuMauLon.soGiayPhep(so));
        bacSi.setHocVi(DuLieuMauLon.HOC_VI.get(so % DuLieuMauLon.HOC_VI.size()));
        bacSi.setNamBatDauHanhNghe(namBatDau);
        bacSi.setNgonNguKham(so % 3 == 0 ? "vi,en" : "vi");
        bacSi.setChucVu("Bác sĩ điều trị khoa " + ck.ten());
        bacSi.setChucVuEn("Attending physician, " + ck.tenEn());
        bacSi.setGioiThieuNgan("%d năm kinh nghiệm khám và điều trị chuyên khoa %s.".formatted(soNamKinhNghiem, ck.ten()));
        bacSi.setGioiThieuNganEn("%d years of experience in %s.".formatted(soNamKinhNghiem, ck.tenEn()));
        bacSi.setTieuSu("Bác sĩ %s công tác tại eClinic từ năm %d, chuyên khám và theo dõi người bệnh khoa %s."
                .formatted(hoTen, namBatDau, ck.ten()));
        bacSi.setTieuSuEn("Dr. %s has practised since %d, focusing on outpatient care in %s."
                .formatted(hoTen, namBatDau, ck.tenEn()));
        bacSi.setQuaTrinhDaoTao("%d: Tốt nghiệp bác sĩ đa khoa, Đại học Y Dược TP. Hồ Chí Minh".formatted(namBatDau));
        bacSi.setQuaTrinhDaoTaoEn("%d: Doctor of Medicine, University of Medicine and Pharmacy at HCMC".formatted(namBatDau));
        bacSi.setQuaTrinhCongTac("%d - nay: Bác sĩ khoa %s, eClinic".formatted(namBatDau, ck.ten()));
        bacSi.setQuaTrinhCongTacEn("%d - present: Physician, %s, eClinic".formatted(namBatDau, ck.tenEn()));
        bacSi.setLinhVucKhamChua("Khám và điều trị ngoại trú chuyên khoa " + ck.ten());
        bacSi.setLinhVucKhamChuaEn("Outpatient care in " + ck.tenEn());
        bacSi.setNgayCapGiayPhep(LocalDate.of(namBatDau, 9, 15));
        bacSi.setNoiCapGiayPhep("Sở Y tế TP. Hồ Chí Minh");
        bacSi.setNoiCapGiayPhepEn(DuLieuMauTiengAnh.NOI_CAP_GIAY_PHEP);
        bacSi.setPhamViHanhNghe("Khám bệnh, chữa bệnh chuyên khoa " + ck.ten());
        bacSi.setPhamViHanhNgheEn("Medical examination and treatment in " + ck.tenEn());
        entityManager.persist(bacSi);
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

    private List<BenhNhanLon> taoBenhNhan(String matKhauHash) {
        List<BenhNhanLon> danhSach = new ArrayList<>();
        for (int so = DuLieuMauLon.SO_BENH_NHAN_DAU; so <= DuLieuMauLon.SO_BENH_NHAN_CUOI; so++) {
            String hoTen = DuLieuMauLon.hoTen(so);
            String soDienThoai = DuLieuMauLon.soDienThoaiBenhNhan(so);
            String email = DuLieuMauLon.emailBenhNhan(so);
            TaiKhoan taiKhoan = taoTaiKhoan(email, hoTen, soDienThoai, VaiTro.BENH_NHAN,
                    TrangThaiTaiKhoan.DA_KICH_HOAT, matKhauHash);
            HoSoBenhNhan hoSo = taoHoSo(so, hoTen, soDienThoai, taiKhoan);
            danhSach.add(new BenhNhanLon(hoSo.getId(), taiKhoan.getId(), hoTen, hoSo.getNgaySinh(), hoSo.getGioiTinh(),
                    soDienThoai, email));
        }
        xoaBoNho();
        return danhSach;
    }

    private HoSoBenhNhan taoHoSo(int so, String hoTen, String soDienThoai, TaiKhoan taiKhoan) {
        HoSoBenhNhan hoSo = new HoSoBenhNhan();
        hoSo.setTaiKhoan(taiKhoan);
        hoSo.setCccd(DuLieuMauLon.cccd(so));
        hoSo.setHoTen(hoTen);
        hoSo.setNgaySinh(DuLieuMauLon.ngaySinh(so));
        hoSo.setGioiTinh(DuLieuMauLon.gioiTinh(so));
        hoSo.setSoDienThoai(soDienThoai);
        hoSo.setDiaChi(DuLieuMauLon.diaChi(so));
        hoSo.setTrangThaiLienKet(taiKhoan == null ? TrangThaiLienKet.CHUA_LIEN_KET : TrangThaiLienKet.DA_LIEN_KET);
        entityManager.persist(hoSo);
        return hoSo;
    }

    /** Người thân có hồ sơ riêng, không tài khoản: gắn vào tài khoản bệnh nhân khác là sai với người dùng thật. */
    private void taoNguoiThan(List<BenhNhanLon> benhNhan) {
        int thuTu = 0;
        for (int i = 0; i < DuLieuMauLon.SO_NGUOI_THAN; i++) {
            int so = DuLieuMauLon.SO_NGUOI_THAN_DAU + i;
            String hoTen = DuLieuMauLon.hoTen(so);
            String soDienThoai = DuLieuMauLon.soDienThoaiBenhNhan(so);
            HoSoBenhNhan hoSo = taoHoSo(so, hoTen, soDienThoai, null);
            for (int k = 0; k < DuLieuMauLon.SO_TAI_KHOAN_LUU_MOI_NGUOI_THAN; k++) {
                BenhNhanLon nguoiLuu = benhNhan.get(thuTu++ % benhNhan.size());
                NguoiThanDaLuu nguoiThan = new NguoiThanDaLuu();
                nguoiThan.setTaiKhoan(entityManager.getReference(TaiKhoan.class, nguoiLuu.idTaiKhoan()));
                nguoiThan.setHoSoBenhNhan(hoSo);
                nguoiThan.setHoTen(hoTen);
                nguoiThan.setNgaySinh(hoSo.getNgaySinh());
                nguoiThan.setGioiTinh(hoSo.getGioiTinh());
                nguoiThan.setCccd(hoSo.getCccd());
                nguoiThan.setSoDienThoai(soDienThoai);
                nguoiThan.setDiaChi(hoSo.getDiaChi());
                nguoiThan.setLanDungCuoi(LocalDateTime.now());
                entityManager.persist(nguoiThan);
            }
        }
        xoaBoNho();
    }

    /** Vô hiệu hoá + mật khẩu ngẫu nhiên: admin đang hoạt động sẽ nhận mọi thông báo yêu cầu đổi lịch. */
    private void taoQuanTri() {
        String matKhauHash = passwordEncoder.encode(UUID.randomUUID().toString());
        for (int so = DuLieuMauLon.SO_QUAN_TRI_DAU; so <= DuLieuMauLon.SO_QUAN_TRI_CUOI; so++) {
            TaiKhoan taiKhoan = taoTaiKhoan(DuLieuMauLon.emailQuanTri(so), DuLieuMauLon.hoTen(so),
                    DuLieuMauLon.soDienThoaiQuanTri(so), VaiTro.QUAN_TRI_VIEN, TrangThaiTaiKhoan.VO_HIEU_HOA, matKhauHash);
            taiKhoan.setLyDoVoHieuHoa("Tài khoản mẫu để thử danh sách quản trị viên");
            QuanTriVien quanTriVien = new QuanTriVien();
            quanTriVien.setTaiKhoan(taiKhoan);
            quanTriVien.setCapDoQuyen(CapDoQuyen.NHAN_VIEN);
            entityManager.persist(quanTriVien);
        }
        xoaBoNho();
    }

    private void taoThuoc() {
        for (NhomThuocLon nhom : DuLieuMauLon.THUOC) {
            for (String hamLuong : nhom.hamLuong()) {
                String tenChuanHoa = chuanHoaThuoc(nhom.ten());
                String hamLuongChuanHoa = chuanHoaThuoc(hamLuong);
                boolean daCo = entityManager.createQuery("""
                        select count(t) from Thuoc t where t.tenChuanHoa = :ten and t.hamLuongChuanHoa = :hamLuong
                        """, Long.class)
                        .setParameter("ten", tenChuanHoa).setParameter("hamLuong", hamLuongChuanHoa)
                        .getSingleResult() > 0;
                if (daCo) {
                    continue;
                }
                Thuoc thuoc = new Thuoc();
                thuoc.setTenThuoc(nhom.ten());
                thuoc.setTenChuanHoa(tenChuanHoa);
                thuoc.setHoatChat(nhom.ten());
                thuoc.setHamLuong(hamLuong);
                thuoc.setHamLuongChuanHoa(hamLuongChuanHoa);
                thuoc.setDangBaoChe("Viên nén");
                thuoc.setDangBaoCheEn("Tablet");
                thuoc.setDuongDung(DuongDungThuoc.UONG);
                thuoc.setDonVi("viên");
                thuoc.setDonViEn("tablet");
                thuoc.setMoTa(nhom.nhom() + ", dùng theo chỉ định của bác sĩ.");
                thuoc.setMoTaEn(nhom.nhomEn() + ", used as prescribed by a doctor.");
                thuoc.setDaXacMinh(true);
                thuoc.setTrangThai(TrangThaiThuoc.DANG_DUNG);
                entityManager.persist(thuoc);
            }
        }
        xoaBoNho();
    }

    private static String chuanHoaThuoc(String chuoi) {
        return chuoi.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    /** Chủ nhật đã qua: ngày nghỉ tương lai sẽ chặn đặt lịch và huỷ ca, còn Chủ nhật không có ca mẫu nào. */
    private void taoNgayNghi(Long idAdmin, LocalDate homNay) {
        Set<LocalDate> daCo = new HashSet<>(entityManager
                .createQuery("select n.ngay from NgayNghiBenhVien n", LocalDate.class).getResultList());
        LocalDate ngay = homNay.with(TemporalAdjusters.previous(DayOfWeek.SUNDAY));
        for (int soDong = 0; soDong < SO_DONG_MOI_BANG; ngay = ngay.minusWeeks(1)) {
            if (daCo.contains(ngay)) {
                continue;
            }
            NgayNghiBenhVien ngayNghi = new NgayNghiBenhVien();
            ngayNghi.setNgay(ngay);
            ngayNghi.setLyDo("Nghỉ cuối tuần");
            ngayNghi.setLyDoEn("Weekend closure");
            ngayNghi.setQuanTriTao(entityManager.getReference(QuanTriVien.class, idAdmin));
            entityManager.persist(ngayNghi);
            soDong++;
        }
    }

    private int xepCaSapToi(Long idAdmin, LocalDate tuNgay) {
        List<BacSiLon> danhSach = timBacSi(DuLieuMau.BAC_SI.size() + 1, DuLieuMauLon.SO_BAC_SI_TOI_DA);
        if (danhSach.isEmpty()) {
            return 0;
        }
        LocalDate denNgay = tuNgay.plusDays(properties.soNgay() - 1L);
        Set<BacSiNgay> ngayDaCoCa = xepCaMau.timNgayDaCoCa(idBacSi(danhSach), tuNgay, denNgay);
        Map<PhongNgay, List<KhoangGio>> caCuaPhong = xepCaMau.timCaDangHoatDongCuaPhong(idPhong(danhSach), tuNgay, denNgay);
        int soCa = 0;
        for (BacSiLon bacSi : danhSach) {
            for (LocalDate ngay = tuNgay; !ngay.isAfter(denNgay); ngay = ngay.plusDays(1)) {
                Buoi buoi = DuLieuMauLon.buoi(bacSi.so(), ngay.getDayOfWeek());
                if (buoi != null && xepCa(bacSi, idAdmin, ngay, buoi, ngayDaCoCa, caCuaPhong)) {
                    soCa++;
                }
            }
            xoaBoNho();
        }
        return soCa;
    }

    /** Ca quá khứ của {@code SO_BAC_SI_CO_LICH_SU} bác sĩ đầu, để lịch hẹn đã qua có khung giờ gắn vào. */
    private int taoCaQuaKhu(Long idAdmin, LocalDate homNay) {
        List<BacSiLon> danhSach = timBacSi(1, DuLieuMauLon.SO_BAC_SI_CO_LICH_SU);
        if (danhSach.isEmpty()) {
            return 0;
        }
        LocalDate tuNgay = homNay.minusWeeks(SO_TUAN_LUI_TOI_DA);
        Set<BacSiNgay> ngayDaCoCa = xepCaMau.timNgayDaCoCa(idBacSi(danhSach), tuNgay, homNay);
        Map<PhongNgay, List<KhoangGio>> caCuaPhong = xepCaMau.timCaDangHoatDongCuaPhong(idPhong(danhSach), tuNgay, homNay);
        Map<Integer, BacSiMau> mauTheoSo = DuLieuMau.BAC_SI.stream()
                .collect(Collectors.toMap(BacSiMau::so, mau -> mau));
        int soCa = 0;
        for (BacSiLon bacSi : danhSach) {
            BacSiMau mau = mauTheoSo.get(bacSi.so());
            // 2 bác sĩ mẫu dùng chung 1 phòng, thứ 2 cả hai cùng làm nhưng khác buổi nên không trùng giờ
            DayOfWeek thu = mau != null ? DayOfWeek.MONDAY : DuLieuMauLon.thuDauTuan(bacSi.so());
            Buoi buoi = mau != null ? mau.nhomCa().buoi(thu) : DuLieuMauLon.buoi(bacSi.so(), thu);
            int soCaCuaBacSi = 0;
            for (LocalDate ngay = homNay.with(TemporalAdjusters.previous(thu));
                    soCaCuaBacSi < SO_CA_QUA_KHU_MOI_BAC_SI && !ngay.isBefore(tuNgay); ngay = ngay.minusWeeks(1)) {
                if (xepCa(bacSi, idAdmin, ngay, buoi, ngayDaCoCa, caCuaPhong)) {
                    soCaCuaBacSi++;
                }
            }
            soCa += soCaCuaBacSi;
            xoaBoNho();
        }
        return soCa;
    }

    private boolean xepCa(BacSiLon bacSi, Long idAdmin, LocalDate ngay, Buoi buoi, Set<BacSiNgay> ngayDaCoCa,
            Map<PhongNgay, List<KhoangGio>> caCuaPhong) {
        if (!ngayDaCoCa.add(new BacSiNgay(bacSi.id(), ngay))) {
            return false;
        }
        List<KhoangGio> caTrongNgay = caCuaPhong.computeIfAbsent(new PhongNgay(bacSi.idPhong(), ngay),
                k -> new ArrayList<>());
        if (caTrongNgay.stream().anyMatch(ca -> ca.trung(buoi))) {
            return false;
        }
        xepCaMau.taoCa(entityManager.getReference(BacSi.class, bacSi.id()),
                entityManager.getReference(PhongKham.class, bacSi.idPhong()),
                entityManager.getReference(QuanTriVien.class, idAdmin), ngay, buoi);
        caTrongNgay.add(new KhoangGio(buoi.gioBatDau(), buoi.gioKetThuc()));
        return true;
    }

    private static Set<Long> idBacSi(List<BacSiLon> danhSach) {
        return danhSach.stream().map(BacSiLon::id).collect(Collectors.toSet());
    }

    private static Set<Long> idPhong(List<BacSiLon> danhSach) {
        return danhSach.stream().map(BacSiLon::idPhong).collect(Collectors.toSet());
    }

    /** Bác sĩ số {@code tuSo..denSo} còn công tác, có phòng đang hoạt động đúng chuyên khoa; xếp theo số thứ tự. */
    private List<BacSiLon> timBacSi(int tuSo, int denSo) {
        Map<String, Integer> soTheoEmail = new HashMap<>();
        Map<Integer, String> phongTheoSo = new HashMap<>();
        for (int so = tuSo; so <= denSo; so++) {
            soTheoEmail.put(DuLieuMauLon.emailBacSi(so), so);
            phongTheoSo.put(so, tenPhongCua(so));
        }
        List<Object[]> dongPhong = entityManager.createQuery("""
                select p.tenPhong, p.id, p.chuyenKhoa.id from PhongKham p
                where p.tenPhong in :ten and p.trangThai = :trangThai
                """, Object[].class)
                .setParameter("ten", phongTheoSo.values())
                .setParameter("trangThai", TrangThaiPhongKham.HOAT_DONG)
                .getResultList();
        Map<String, Object[]> phongTheoTen = new HashMap<>();
        for (Object[] cot : dongPhong) {
            phongTheoTen.put((String) cot[0], cot);
        }

        List<Object[]> dongBacSi = entityManager.createQuery("""
                select b.id, t.email, b.chuyenKhoa.id from BacSi b join b.taiKhoan t
                where t.email in :emails and t.trangThai = :trangThaiTaiKhoan and b.trangThai = :trangThaiBacSi
                """, Object[].class)
                .setParameter("emails", soTheoEmail.keySet())
                .setParameter("trangThaiTaiKhoan", TrangThaiTaiKhoan.DA_KICH_HOAT)
                .setParameter("trangThaiBacSi", TrangThaiBacSi.DANG_CONG_TAC)
                .getResultList();
        List<BacSiLon> danhSach = new ArrayList<>();
        for (Object[] cot : dongBacSi) {
            int so = soTheoEmail.get((String) cot[1]);
            Object[] phong = phongTheoTen.get(phongTheoSo.get(so));
            if (phong != null && phong[2].equals(cot[2])) {
                danhSach.add(new BacSiLon((Long) cot[0], so, (Long) phong[1]));
            }
        }
        danhSach.sort(Comparator.comparingInt(BacSiLon::so));
        return danhSach;
    }

    private static String tenPhongCua(int soBacSi) {
        if (soBacSi > DuLieuMau.BAC_SI.size()) {
            return DuLieuMauLon.tenPhong(DuLieuMauLon.soPhong(soBacSi));
        }
        String tenChuyenKhoa = DuLieuMau.BAC_SI.get(soBacSi - 1).chuyenKhoa();
        return DuLieuMau.CHUYEN_KHOA.stream().filter(mau -> mau.ten().equals(tenChuyenKhoa)).findFirst()
                .map(ChuyenKhoaMau::tenPhong).orElseThrow();
    }

    private List<BenhNhanLon> timBenhNhanDau() {
        List<String> emails = new ArrayList<>();
        for (int so = 1; so < DuLieuMauLon.SO_BENH_NHAN_DAU; so++) {
            emails.add(DuLieuMauLon.emailBenhNhan(so));
        }
        return entityManager.createQuery("""
                select h.id, t.id, h.hoTen, h.ngaySinh, h.gioiTinh, h.soDienThoai, t.email
                from HoSoBenhNhan h join h.taiKhoan t
                where t.email in :emails and t.trangThai = :trangThai
                order by t.email
                """, Object[].class)
                .setParameter("emails", emails)
                .setParameter("trangThai", TrangThaiTaiKhoan.DA_KICH_HOAT)
                .getResultStream()
                .map(cot -> new BenhNhanLon((Long) cot[0], (Long) cot[1], (String) cot[2], (LocalDate) cot[3],
                        (GioiTinh) cot[4], (String) cot[5], (String) cot[6]))
                .toList();
    }

    /** Lượt của các ca vừa tạo trong khoảng ngày, theo bác sĩ, xếp theo giờ. */
    private Map<Long, List<LuotLon>> timLuotMoi(Moc moc, LocalDate tuNgay, LocalDate denNgay) {
        Map<Long, List<LuotLon>> luotTheoBacSi = new HashMap<>();
        entityManager.createQuery("""
                select k.id, l.id, l.bacSi.id, l.phongKham.id, k.gioBatDau
                from KhungGioKham k join k.lichLamViec l
                where l.id > :moc and l.ngayLamViec between :tuNgay and :denNgay
                order by k.gioBatDau, k.id
                """, Object[].class)
                .setParameter("moc", moc.lichLamViec())
                .setParameter("tuNgay", tuNgay)
                .setParameter("denNgay", denNgay)
                .getResultList()
                .forEach(cot -> luotTheoBacSi.computeIfAbsent((Long) cot[2], k -> new ArrayList<>())
                        .add(new LuotLon((Long) cot[0], (Long) cot[1], (Long) cot[2], (Long) cot[3],
                                (LocalDateTime) cot[4])));
        return luotTheoBacSi;
    }

    /** Lịch đã đóng trên ca đã qua: {@code DongNgayKhamJob} không còn gì để đổi, bệnh nhân không bị tính lịch đang hoạt động. */
    private void taoLichHenQuaKhu(Moc moc, Long idAdmin, LocalDate homNay, List<BenhNhanLon> benhNhanDau,
            List<BenhNhanLon> benhNhanMoi) {
        Map<Long, List<LuotLon>> luotTheoBacSi = timLuotMoi(moc, homNay.minusWeeks(SO_TUAN_LUI_TOI_DA), homNay.minusDays(1));
        Map<Long, Deque<LuotLon>> conLai = new HashMap<>();
        luotTheoBacSi.forEach((id, luot) -> {
            List<LuotLon> tronLan = new ArrayList<>(luot);
            Collections.shuffle(tronLan, NGAU_NHIEN_CO_DINH);
            conLai.put(id, new ArrayDeque<>(tronLan));
        });
        List<BacSiLon> bacSiCoLichSu = timBacSi(1, DuLieuMauLon.SO_BAC_SI_CO_LICH_SU);
        List<BacSiLon> nhomDau = bacSiCoLichSu.stream().filter(b -> b.so() <= DuLieuMau.BAC_SI.size()).toList();
        List<BacSiLon> nhomSau = bacSiCoLichSu.stream().filter(b -> b.so() > DuLieuMau.BAC_SI.size()).toList();
        List<LuotLon> luotDaChon = new ArrayList<>();
        chonLuot(luotDaChon, conLai, nhomDau, SO_LICH_HEN_BAC_SI_DAU);
        chonLuot(luotDaChon, conLai, nhomSau, SO_LICH_HEN_BAC_SI_SAU);
        luotDaChon.sort(Comparator.comparing(LuotLon::gioBatDau));

        List<TrangThaiLichHen> trangThai = new ArrayList<>();
        trangThai.addAll(Collections.nCopies(100, TrangThaiLichHen.DA_HOAN_THANH));
        trangThai.addAll(Collections.nCopies(15, TrangThaiLichHen.DA_HUY));
        trangThai.addAll(Collections.nCopies(5, TrangThaiLichHen.KHONG_DEN));
        trangThai.addAll(Collections.nCopies(5, TrangThaiLichHen.BI_TU_CHOI));
        trangThai.addAll(Collections.nCopies(5, TrangThaiLichHen.QUAN_TRI_HUY));
        Collections.shuffle(trangThai, NGAU_NHIEN_CO_DINH);

        Long idTaiKhoanAdmin = entityManager
                .createQuery("select q.taiKhoan.id from QuanTriVien q where q.id = :id", Long.class)
                .setParameter("id", idAdmin).getSingleResult();
        List<Long> idThuoc = entityManager.createQuery(
                "select t.id from Thuoc t where t.trangThai = :trangThai and t.daXacMinh = true order by t.id", Long.class)
                .setParameter("trangThai", TrangThaiThuoc.DANG_DUNG).getResultList();

        Map<PhongNgay, Integer> soThuTuTrongNgay = new HashMap<>();
        List<DanhGiaLon> danhGia = new ArrayList<>();
        int soBenhAn = 0;
        for (int i = 0; i < luotDaChon.size(); i++) {
            LuotLon luot = luotDaChon.get(i);
            boolean benhNhanDemo = i < SO_LICH_HEN_BENH_NHAN_DAU && !benhNhanDau.isEmpty();
            BenhNhanLon benhNhan = benhNhanDemo ? benhNhanDau.get(i % benhNhanDau.size())
                    : benhNhanMoi.get(i % benhNhanMoi.size());
            LanKhamLon lanKham = DuLieuMauLon.LAN_KHAM.get(i % DuLieuMauLon.LAN_KHAM.size());
            int soThuTu = soThuTuTrongNgay.merge(new PhongNgay(luot.idPhong(), luot.gioBatDau().toLocalDate()), 1,
                    Integer::sum);
            LichHen lichHen = taoLichHen(i + 1, luot, benhNhan, trangThai.get(i), soThuTu, lanKham.lyDoKham(), idTaiKhoanAdmin);
            if (lichHen.getTrangThai() == TrangThaiLichHen.DA_HOAN_THANH) {
                taoHoSoBenhAn(soBenhAn, lichHen, luot, benhNhan, lanKham, idThuoc);
                soBenhAn++;
                if (danhGia.size() < SO_DANH_GIA) {
                    danhGia.add(new DanhGiaLon(lichHen.getId(), benhNhan.idTaiKhoan(), luot.gioBatDau()));
                }
            }
        }
        xoaBoNho();
        ghiDanhGia(danhGia);
    }

    private static void chonLuot(List<LuotLon> ketQua, Map<Long, Deque<LuotLon>> conLai, List<BacSiLon> nhom, int soLuong) {
        for (int i = 0; i < soLuong && !nhom.isEmpty(); i++) {
            Deque<LuotLon> luot = conLai.get(nhom.get(i % nhom.size()).id());
            if (luot != null && !luot.isEmpty()) {
                ketQua.add(luot.poll());
            }
        }
    }

    /** Lịch sắp tới: chỉ của bệnh nhân mới, để {@code benhnhan01..04} vẫn đặt lịch được. */
    private void taoLichHenSapToi(Moc moc, LocalDate homNay, List<BenhNhanLon> benhNhanMoi) {
        Map<Long, List<LuotLon>> luotTheoBacSi = timLuotMoi(moc, homNay.plusDays(1),
                homNay.plusDays(properties.soNgay()));
        List<BacSiLon> bacSiMoi = timBacSi(DuLieuMau.BAC_SI.size() + 1, DuLieuMauLon.SO_BAC_SI_TOI_DA);
        int soDaTao = 0;
        for (BacSiLon bacSi : bacSiMoi) {
            List<LuotLon> luot = luotTheoBacSi.get(bacSi.id());
            if (soDaTao < SO_LICH_HEN_SAP_TOI && luot != null) {
                BenhNhanLon benhNhan = benhNhanMoi.get(SO_LICH_HEN_BENH_NHAN_DAU + soDaTao);
                taoLichHen(1000 + soDaTao, luot.get(0), benhNhan, TrangThaiLichHen.DA_XAC_NHAN, 1,
                        DuLieuMauLon.LAN_KHAM.get(soDaTao % DuLieuMauLon.LAN_KHAM.size()).lyDoKham(), null);
                soDaTao++;
            }
        }
        xoaBoNho();
    }

    private LichHen taoLichHen(int thuTu, LuotLon luot, BenhNhanLon benhNhan, TrangThaiLichHen trangThai, int soThuTu,
            String lyDoKham, Long idTaiKhoanAdmin) {
        KenhDatLich kenh = switch (thuTu % 5) {
            case 0 -> KenhDatLich.KHACH;
            case 4 -> KenhDatLich.HOTLINE;
            default -> KenhDatLich.TAI_KHOAN;
        };
        // Lịch sắp tới không có admin đặt hộ: chỉ lịch đã qua mới cần dòng HOTLINE
        if (idTaiKhoanAdmin == null && kenh == KenhDatLich.HOTLINE) {
            kenh = KenhDatLich.TAI_KHOAN;
        }
        TaiKhoan taiKhoanBenhNhan = entityManager.getReference(TaiKhoan.class, benhNhan.idTaiKhoan());

        LichHen lichHen = new LichHen();
        lichHen.setHoSoBenhNhan(entityManager.getReference(HoSoBenhNhan.class, benhNhan.idHoSo()));
        lichHen.setTaiKhoanDat(kenh == KenhDatLich.TAI_KHOAN ? taiKhoanBenhNhan : null);
        lichHen.setKenhDat(kenh);
        lichHen.setBacSi(entityManager.getReference(BacSi.class, luot.idBacSi()));
        lichHen.setKhungGio(entityManager.getReference(KhungGioKham.class, luot.id()));
        lichHen.setPhongKham(entityManager.getReference(PhongKham.class, luot.idPhong()));
        lichHen.setSoThuTu(soThuTu);
        lichHen.setTrangThai(trangThai);
        lichHen.setLyDoKham(lyDoKham);
        lichHen.setSoDienThoaiLienHe(benhNhan.soDienThoai());
        lichHen.setEmailLienHe(benhNhan.email());
        lichHen.setNgonNgu(NgonNgu.VI);
        lichHen.setHoTenDaNhap(benhNhan.hoTen());
        lichHen.setNgaySinhDaNhap(benhNhan.ngaySinh());
        lichHen.setGioiTinhDaNhap(benhNhan.gioiTinh());
        // 6 chữ số: mã thật dùng 4 chữ số nên không trùng
        lichHen.setMaTraCuu("ECL-%s-%06d".formatted(
                luot.gioBatDau().format(DateTimeFormatter.BASIC_ISO_DATE), thuTu));
        lichHen.setMaTokenPhieuKham(TokenNgauNhien.tao());
        switch (trangThai) {
            case DA_HOAN_THANH -> {
                lichHen.setTrangThaiHangDoi(TrangThaiHangDoi.DA_KHAM);
                lichHen.setGoiLuc(luot.gioBatDau());
            }
            case KHONG_DEN -> lichHen.setTrangThaiHangDoi(TrangThaiHangDoi.VANG_MAT);
            case DA_HUY -> lichHen.setLyDoHuy("Bệnh nhân bận việc đột xuất");
            case BI_TU_CHOI -> lichHen.setLyDoHuy("Bác sĩ có lịch công tác đột xuất");
            case QUAN_TRI_HUY -> lichHen.setLyDoHuy("Phòng khám điều chỉnh lịch làm việc");
            default -> {
            }
        }
        entityManager.persist(lichHen);

        ghiLichSu(lichHen, kenh, taiKhoanBenhNhan, idTaiKhoanAdmin);
        return lichHen;
    }

    private void ghiLichSu(LichHen lichHen, KenhDatLich kenh, TaiKhoan taiKhoanBenhNhan, Long idTaiKhoanAdmin) {
        LoaiNguoiThucHien loai = switch (kenh) {
            case KHACH -> LoaiNguoiThucHien.KHACH;
            case TAI_KHOAN -> LoaiNguoiThucHien.TAI_KHOAN;
            case HOTLINE -> LoaiNguoiThucHien.QUAN_TRI_VIEN;
        };
        TaiKhoan nguoiDat = switch (kenh) {
            case KHACH -> null;
            case TAI_KHOAN -> taiKhoanBenhNhan;
            case HOTLINE -> entityManager.getReference(TaiKhoan.class, idTaiKhoanAdmin);
        };
        entityManager.persist(LichSuLichHen.tao(lichHen, SuKienLichHen.DAT_LICH, loai, nguoiDat, null, null));
        if (lichHen.getTrangThai() == TrangThaiLichHen.DA_HUY) {
            entityManager.persist(LichSuLichHen.tao(lichHen, SuKienLichHen.HUY, loai, nguoiDat, lichHen.getLyDoHuy(), null));
        } else if (lichHen.getTrangThai() == TrangThaiLichHen.QUAN_TRI_HUY) {
            entityManager.persist(LichSuLichHen.tao(lichHen, SuKienLichHen.HUY, LoaiNguoiThucHien.QUAN_TRI_VIEN,
                    entityManager.getReference(TaiKhoan.class, idTaiKhoanAdmin), lichHen.getLyDoHuy(), null));
        }
    }

    /** Không đề xuất ngày tái khám: {@code NhacTaiKhamJob} sẽ gửi nhắc cho các hồ sơ có ngày đó. */
    private void taoHoSoBenhAn(int thuTu, LichHen lichHen, LuotLon luot, BenhNhanLon benhNhan, LanKhamLon lanKham,
            List<Long> idThuoc) {
        HoSoBenhAn hoSoBenhAn = new HoSoBenhAn();
        hoSoBenhAn.setLichHen(lichHen);
        hoSoBenhAn.setHoSoBenhNhan(entityManager.getReference(HoSoBenhNhan.class, benhNhan.idHoSo()));
        hoSoBenhAn.setBacSi(entityManager.getReference(BacSi.class, luot.idBacSi()));
        hoSoBenhAn.setLyDoKham(lanKham.lyDoKham());
        hoSoBenhAn.setMach(70 + thuTu % 20);
        hoSoBenhAn.setNhietDo(BigDecimal.valueOf(365 + thuTu % 10L, 1));
        hoSoBenhAn.setHuyetApTamThu(110 + thuTu % 30);
        hoSoBenhAn.setHuyetApTamTruong(70 + thuTu % 20);
        hoSoBenhAn.setNhipTho(16 + thuTu % 5);
        hoSoBenhAn.setCanNang(BigDecimal.valueOf(50 + thuTu % 30L));
        hoSoBenhAn.setChieuCao(BigDecimal.valueOf(155 + thuTu % 25L));
        hoSoBenhAn.setKhamLamSang(lanKham.khamLamSang());
        hoSoBenhAn.setChanDoan(lanKham.chanDoan());
        hoSoBenhAn.setIcdChinh(DanhMucIcd10.tim(lanKham.maIcd()).orElse(null));
        hoSoBenhAn.setMaTokenSoKham(TokenNgauNhien.tao());

        ChiTietDonThuoc chiTiet = new ChiTietDonThuoc();
        chiTiet.setThuoc(entityManager.getReference(Thuoc.class, idThuoc.get(thuTu % idThuoc.size())));
        chiTiet.setLieuDung("1 viên/lần");
        chiTiet.setSoLanMoiNgay(2);
        chiTiet.setSoNgayDung(5);
        chiTiet.setSoLuong(10);
        chiTiet.setGhiChuSuDung("Uống sau ăn");
        hoSoBenhAn.addChiTietDonThuoc(chiTiet);
        entityManager.persist(hoSoBenhAn);
    }

    /** Đánh giá là các cột của lịch hẹn, entity LichHen không ghi các cột này. */
    private void ghiDanhGia(List<DanhGiaLon> danhSach) {
        for (int i = 0; i < danhSach.size(); i++) {
            DanhGiaLon danhGia = danhSach.get(i);
            LocalDateTime ngayDanhGia = danhGia.gioKham().plusHours(6);
            entityManager.createNativeQuery("""
                    UPDATE lich_hen SET danh_gia_so_sao = :soSao, danh_gia_nhan_xet = :nhanXet,
                        danh_gia_id_tai_khoan = :idTaiKhoan, danh_gia_ngay_tao = :ngay, danh_gia_ngay_cap_nhat = :ngay
                    WHERE id_lich_hen = :id
                    """)
                    .setParameter("soSao", 3 + i % 3)
                    .setParameter("nhanXet", DuLieuMauLon.NHAN_XET.get(i % DuLieuMauLon.NHAN_XET.size()))
                    .setParameter("idTaiKhoan", danhGia.idTaiKhoan())
                    .setParameter("ngay", ngayDanhGia)
                    .setParameter("id", danhGia.idLichHen())
                    .executeUpdate();
        }
    }

    /** Chỉ các trạng thái đã đóng không làm đổi ca, cộng vài yêu cầu chờ duyệt trên ca sắp tới không có lịch hẹn. */
    private void taoYeuCauDoiLich(Moc moc, Long idAdmin, LocalDate homNay) {
        List<CaQuaKhu> caDaQua = entityManager.createQuery("""
                select l.id, l.bacSi.id, l.ngayLamViec from LichLamViec l
                where l.id > :moc and l.ngayLamViec < :homNay
                order by l.id
                """, Object[].class)
                .setParameter("moc", moc.lichLamViec())
                .setParameter("homNay", homNay)
                .getResultStream()
                .map(cot -> new CaQuaKhu((Long) cot[0], (Long) cot[1], (LocalDate) cot[2]))
                .toList();
        for (int i = 0; i < Math.min(SO_YEU_CAU_DA_DONG, caDaQua.size()); i++) {
            CaQuaKhu ca = caDaQua.get(i);
            YeuCauDoiLich yeuCau = yeuCauMoi(ca.id(), ca.idBacSi(), i);
            LocalDateTime ngayXuLy = ca.ngay().minusDays(1).atTime(9, 0);
            switch (i % 3) {
                case 0 -> {
                    yeuCau.setLoaiYeuCau(LoaiYeuCauDoiLich.XIN_NGHI);
                    yeuCau.setTrangThai(TrangThaiYeuCau.DA_RUT);
                }
                case 1 -> {
                    yeuCau.setLoaiYeuCau(LoaiYeuCauDoiLich.DOI_CA);
                    yeuCau.setNgayMongMuon(ca.ngay().plusDays(1));
                    yeuCau.setGioBatDauMongMuon(Buoi.SANG.gioBatDau());
                    yeuCau.setGioKetThucMongMuon(Buoi.SANG.gioKetThuc());
                    yeuCau.setTrangThai(TrangThaiYeuCau.TU_CHOI);
                    yeuCau.setAdminXuLy(entityManager.getReference(QuanTriVien.class, idAdmin));
                    yeuCau.setGhiChuXuLy("Không đủ bác sĩ thay ca trong ngày này");
                    yeuCau.setNgayXuLy(ngayXuLy);
                }
                default -> {
                    yeuCau.setLoaiYeuCau(LoaiYeuCauDoiLich.NHUONG_CA);
                    yeuCau.setBacSiNhan(entityManager.getReference(BacSi.class,
                            caDaQua.get((i + SO_CA_QUA_KHU_MOI_BAC_SI) % caDaQua.size()).idBacSi()));
                    yeuCau.setTrangThai(TrangThaiYeuCau.DONG_NGHIEP_TU_CHOI);
                    yeuCau.setGhiChuBacSiNhan("Trùng lịch cá nhân");
                    yeuCau.setNgayBacSiNhanPhanHoi(ngayXuLy);
                }
            }
            entityManager.persist(yeuCau);
        }

        Map<Long, List<LuotLon>> luotTheoBacSi = timLuotMoi(moc, homNay.plusDays(1), homNay.plusDays(properties.soNgay()));
        List<BacSiLon> bacSiMoi = timBacSi(DuLieuMau.BAC_SI.size() + 1, DuLieuMauLon.SO_BAC_SI_TOI_DA);
        // Bỏ các bác sĩ đầu: ca đầu tiên của họ đã có lịch hẹn sắp tới
        for (int i = SO_LICH_HEN_SAP_TOI; i < Math.min(SO_LICH_HEN_SAP_TOI + SO_YEU_CAU_CHO_DUYET, bacSiMoi.size()); i++) {
            List<LuotLon> luot = luotTheoBacSi.get(bacSiMoi.get(i).id());
            if (luot != null) {
                YeuCauDoiLich yeuCau = yeuCauMoi(luot.get(0).idCa(), luot.get(0).idBacSi(), i);
                yeuCau.setLoaiYeuCau(LoaiYeuCauDoiLich.XIN_NGHI);
                yeuCau.setTrangThai(TrangThaiYeuCau.CHO_DUYET);
                entityManager.persist(yeuCau);
            }
        }
        xoaBoNho();
    }

    private YeuCauDoiLich yeuCauMoi(Long idCa, Long idBacSi, int thuTu) {
        YeuCauDoiLich yeuCau = new YeuCauDoiLich();
        yeuCau.setBacSi(entityManager.getReference(BacSi.class, idBacSi));
        yeuCau.setLichLamViec(entityManager.getReference(LichLamViec.class, idCa));
        yeuCau.setLyDo(DuLieuMauLon.LY_DO_DOI_LICH.get(thuTu % DuLieuMauLon.LY_DO_DOI_LICH.size()));
        return yeuCau;
    }

    /** Không có khoa_mau: ThongBaoMapper trả thẳng noi_dung đã lưu. */
    private void taoThongBao(List<BenhNhanLon> benhNhan) {
        List<LoaiThongBao> loai = List.of(LoaiThongBao.LICH_HEN_DA_XAC_NHAN, LoaiThongBao.NHAC_LICH_KHAM,
                LoaiThongBao.HE_THONG);
        for (int i = 0; i < SO_DONG_MOI_BANG; i++) {
            ThongBao thongBao = new ThongBao();
            thongBao.setTaiKhoan(entityManager.getReference(TaiKhoan.class, benhNhan.get(i % benhNhan.size()).idTaiKhoan()));
            thongBao.setMaNguon("mau:" + i);
            thongBao.setNoiDung("Thông báo mẫu số %d từ eClinic.".formatted(i + 1));
            thongBao.setLoai(loai.get(i % loai.size()));
            thongBao.setDaDoc(i % 2 == 0);
            entityManager.persist(thongBao);
        }
    }

    /** Dòng đã gửi xong: job gửi chỉ đọc dòng chưa có ngay_gui / đang CHO_GUI nên không gửi lại gì. */
    private void taoHangChoGui(List<BenhNhanLon> benhNhan) {
        LocalDateTime bayGio = LocalDateTime.now();
        for (int i = 0; i < SO_DONG_MOI_BANG / 2; i++) {
            BenhNhanLon nguoiNhan = benhNhan.get(i % benhNhan.size());
            SuKienThongBao suKien = new SuKienThongBao();
            suKien.setLoai(LoaiThongBao.HE_THONG);
            suKien.setIdTaiKhoan(nguoiNhan.idTaiKhoan());
            suKien.setNoiDung("Sự kiện mẫu số %d.".formatted(i + 1));
            suKien.setNgayGui(bayGio);
            entityManager.persist(suKien);

            EmailGui email = new EmailGui();
            email.setMaNguon("mau:" + i);
            email.setNguoiNhan(nguoiNhan.email());
            email.setNgonNgu(NgonNgu.VI);
            email.setKhoaMau("email.datLich");
            email.setThamSo("[]");
            email.setTrangThai(TrangThaiEmail.DA_GUI);
            email.setLanThuKeTiep(bayGio);
            email.setNgayGui(bayGio);
            entityManager.persist(email);
        }
    }

    /** Hash của chuỗi ngẫu nhiên không ai giữ: phiên chỉ để hiện trong danh sách, không dùng đăng nhập được. */
    private void taoPhienDangNhap(List<BenhNhanLon> benhNhan) {
        LocalDateTime bayGio = LocalDateTime.now();
        for (int i = 0; i < SO_DONG_MOI_BANG; i++) {
            RefreshToken refreshToken = new RefreshToken();
            refreshToken.setTaiKhoan(entityManager.getReference(TaiKhoan.class, benhNhan.get(i % benhNhan.size()).idTaiKhoan()));
            refreshToken.setMaPhien(UUID.randomUUID().toString());
            refreshToken.setTokenHash(TokenNgauNhien.bam(TokenNgauNhien.tao()));
            refreshToken.setThongTinThietBi(i % 2 == 0 ? "Chrome trên Windows" : "Safari trên iPhone");
            refreshToken.setNgayDangNhap(bayGio.minusHours(i % 24L));
            refreshToken.setNgayHetHan(bayGio.plusDays(7));
            entityManager.persist(refreshToken);
        }
    }

    private void taoPhienChat(List<BenhNhanLon> benhNhan) {
        for (int i = 0; i < SO_DONG_MOI_BANG; i++) {
            LocalDateTime batDau = LocalDateTime.now().minusDays(1L + i % 30);
            PhienChat phien = new PhienChat();
            if (i < SO_PHIEN_CHAT_CUA_TAI_KHOAN) {
                phien.setTaiKhoan(entityManager.getReference(TaiKhoan.class, benhNhan.get(i % benhNhan.size()).idTaiKhoan()));
            } else {
                phien.setMaDinhDanhKhach("mau-khach-%03d".formatted(i));
            }
            phien.setThoiGianBatDau(batDau);
            phien.setThoiGianKetThuc(batDau.plusMinutes(10));
            phien.setTrangThai(TrangThaiPhienChat.DA_DONG);
            phien.setDongYDuLieuLuc(batDau);
            entityManager.persist(phien);

            int noiDung = i % DuLieuMauLon.CAU_HOI_CHAT.size();
            taoTinNhan(phien, NguoiGui.NGUOI_DUNG, LoaiTinNhanChat.CAU_HOI, DuLieuMauLon.CAU_HOI_CHAT.get(noiDung));
            taoTinNhan(phien, NguoiGui.CHATBOT, LoaiTinNhanChat.TRA_LOI, DuLieuMauLon.TRA_LOI_CHAT.get(noiDung));
        }
    }

    private void taoTinNhan(PhienChat phien, NguoiGui nguoiGui, LoaiTinNhanChat loai, String noiDung) {
        TinNhanChat tinNhan = new TinNhanChat();
        tinNhan.setPhienChat(phien);
        tinNhan.setNguoiGui(nguoiGui);
        tinNhan.setLoai(loai);
        tinNhan.setNoiDung(noiDung);
        entityManager.persist(tinNhan);
    }

    /** Mốc 0 = mọi mốc nhắc đã xử lý: NhacLichJob bỏ qua lịch mẫu, không gửi email tới địa chỉ @eclinic.local. */
    private void tatNhacLich(Moc moc) {
        chay("""
                UPDATE lich_hen SET nhac_moc_da_xu_ly = 0
                WHERE id_lich_hen > :moc AND trang_thai = 'DA_XAC_NHAN'
                """, moc.lichHen(), null);
    }

    /**
     * Auditing ghi ngày tạo là hôm nay: lịch khám tháng trước mà "tạo hôm nay" làm sai báo cáo và dashboard.
     * Sửa bằng câu UPDATE cuối cùng, cũng cập nhật trạng thái khung giờ đã có người đặt.
     */
    private void luiThoiGian(Moc moc, LocalDate homNay) {
        chay("""
                UPDATE khung_gio_kham k JOIN lich_hen l ON l.id_khung_gio = k.id_khung_gio
                SET k.trang_thai = 'DA_DAT'
                WHERE l.id_lich_hen > :moc AND l.trang_thai IN ('DA_HOAN_THANH', 'KHONG_DEN', 'DA_XAC_NHAN')
                """, moc.lichHen(), null);
        chay("""
                UPDATE lich_hen l JOIN khung_gio_kham k ON k.id_khung_gio = l.id_khung_gio
                SET l.ngay_tao = k.gio_bat_dau - INTERVAL 3 DAY, l.ngay_cap_nhat = k.gio_ket_thuc
                WHERE l.id_lich_hen > :moc AND k.gio_bat_dau < :homNay
                """, moc.lichHen(), homNay);
        chay("""
                UPDATE ho_so_benh_an h JOIN lich_hen l ON l.id_lich_hen = h.id_lich_hen
                SET h.ngay_tao = l.ngay_cap_nhat
                WHERE h.id_ho_so_benh_an > :moc
                """, moc.hoSoBenhAn(), null);
        chay("""
                UPDATE lich_su_thay_doi s JOIN lich_hen l ON l.id_lich_hen = s.id_lich_hen
                SET s.ngay_tao = CASE WHEN s.hanh_dong = 'DAT_LICH' THEN l.ngay_tao ELSE l.ngay_cap_nhat END
                WHERE s.id_lich_su > :moc AND s.loai = 'LICH_HEN' AND l.ngay_tao < :homNay
                """, moc.lichSu(), homNay);
        chay("""
                UPDATE yeu_cau_doi_lich y JOIN lich_lam_viec l ON l.id_lich_lam_viec = y.id_lich_lam_viec
                SET y.ngay_gui = l.ngay_lam_viec - INTERVAL 3 DAY
                WHERE y.id_yeu_cau > :moc AND l.ngay_lam_viec < :homNay
                """, moc.yeuCau(), homNay);
        chay("""
                UPDATE thong_bao SET ngay_tao = :homNay - INTERVAL (id_thong_bao % 30) DAY
                WHERE id_thong_bao > :moc
                """, moc.thongBao(), homNay);
        chay("""
                UPDATE tin_nhan_chat t JOIN phien_chat p ON p.id_phien_chat = t.id_phien_chat
                SET t.ngay_tao = p.thoi_gian_bat_dau
                WHERE t.id_tin_nhan > :moc
                """, moc.tinNhan(), null);
    }

    private void chay(String sql, long moc, LocalDate homNay) {
        var cauLenh = entityManager.createNativeQuery(sql).setParameter("moc", moc);
        if (homNay != null) {
            cauLenh.setParameter("homNay", homNay.atStartOfDay());
        }
        cauLenh.executeUpdate();
    }

}
