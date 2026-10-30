package iuh.fit.se.eclinic.identity.dulieumau;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import iuh.fit.se.eclinic.common.entity.booking.HoSoBenhNhan;
import iuh.fit.se.eclinic.common.entity.catalog.AnhBacSi;
import iuh.fit.se.eclinic.common.entity.catalog.BacSi;
import iuh.fit.se.eclinic.common.entity.catalog.ChuyenKhoa;
import iuh.fit.se.eclinic.common.entity.catalog.PhongKham;
import iuh.fit.se.eclinic.common.entity.identity.QuanTriVien;
import iuh.fit.se.eclinic.common.entity.identity.TaiKhoan;
import iuh.fit.se.eclinic.common.entity.medical.Thuoc;
import iuh.fit.se.eclinic.common.enums.LoaiAnhBacSi;
import iuh.fit.se.eclinic.common.enums.TrangThaiBacSi;
import iuh.fit.se.eclinic.common.enums.TrangThaiLienKet;
import iuh.fit.se.eclinic.common.enums.TrangThaiPhongKham;
import iuh.fit.se.eclinic.common.enums.TrangThaiTaiKhoan;
import iuh.fit.se.eclinic.common.enums.TrangThaiThuoc;
import iuh.fit.se.eclinic.common.enums.VaiTro;
import iuh.fit.se.eclinic.identity.dulieumau.DuLieuMau.BacSiMau;
import iuh.fit.se.eclinic.identity.dulieumau.DuLieuMau.BenhNhanMau;
import iuh.fit.se.eclinic.identity.dulieumau.DuLieuMau.Buoi;
import iuh.fit.se.eclinic.identity.dulieumau.DuLieuMau.ChuyenKhoaMau;
import iuh.fit.se.eclinic.identity.dulieumau.DuLieuMau.ThuocMau;
import iuh.fit.se.eclinic.identity.dulieumau.DuLieuMauTiengAnh.ChuyenKhoaEn;
import iuh.fit.se.eclinic.identity.dulieumau.DuLieuMauTiengAnh.HoSoBacSiEn;
import iuh.fit.se.eclinic.identity.dulieumau.XepCaMau.BacSiNgay;
import iuh.fit.se.eclinic.identity.dulieumau.XepCaMau.KhoangGio;
import iuh.fit.se.eclinic.identity.dulieumau.XepCaMau.PhongNgay;
import iuh.fit.se.eclinic.identity.repository.TaiKhoanRepository;
import iuh.fit.se.eclinic.identity.util.ChuanHoa;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** identity-service không có repository của catalog / booking nên dùng EntityManager trực tiếp. */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnBooleanProperty("app.du-lieu-mau.bat")
public class DuLieuMauServiceImpl implements DuLieuMauService {

    private final EntityManager entityManager;
    private final TaiKhoanRepository taiKhoanRepository;
    private final PasswordEncoder passwordEncoder;
    private final DuLieuMauProperties properties;
    private final XepCaMau xepCaMau;

    @Override
    @Transactional
    public boolean taoDuLieuNen() {
        if (taiKhoanRepository.existsByVaiTro(VaiTro.BAC_SI)) {
            return false;
        }
        // Mã hoá 1 lần rồi dùng chung: BCrypt chậm có chủ đích
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
            bacSi.setNamBatDauHanhNghe(mau.namBatDauHanhNghe());
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
    public int boSungHoSoBacSi() {
        Map<String, BacSiMau> mauTheoEmail = DuLieuMau.BAC_SI.stream()
                .collect(Collectors.toMap(BacSiMau::email, Function.identity()));
        List<BacSi> chuaCoHoSo = entityManager.createQuery("""
                select b from BacSi b join fetch b.taiKhoan t
                where t.email in :emails and b.gioiThieuNgan is null
                order by b.id
                """, BacSi.class)
                .setParameter("emails", mauTheoEmail.keySet())
                .getResultList();

        for (BacSi bacSi : chuaCoHoSo) {
            BacSiMau mau = mauTheoEmail.get(bacSi.getTaiKhoan().getEmail());
            HoSoBacSiMau hoSo = HoSoBacSiMau.THEO_SO.get(mau.so());
            bacSi.setChucVu(hoSo.chucVu());
            bacSi.setGioiThieuNgan(hoSo.gioiThieuNgan());
            bacSi.setQuaTrinhDaoTao(String.join("\n", hoSo.daoTao()));
            bacSi.setQuaTrinhCongTac(String.join("\n", hoSo.congTac()));
            bacSi.setLinhVucKhamChua(String.join("\n", hoSo.linhVuc()));
            if (bacSi.getTaiKhoan().getAnhDaiDien() == null) {
                bacSi.getTaiKhoan().setAnhDaiDien(anhGiu(400, 400, "0f766e", "ffffff", "BS " + mau.so()));
            }

            if (docAnhGioiThieu(bacSi.getId()).isEmpty()) {
                long idCuoi = ((Number) entityManager
                        .createNativeQuery("SELECT anh_id_cuoi FROM bac_si WHERE id_bac_si = :id")
                        .setParameter("id", bacSi.getId())
                        .getSingleResult()).longValue();
                ghiAnhGioiThieu(bacSi.getId(), List.of(
                        anhMau(idCuoi + 1, 1, LoaiAnhBacSi.ANH_CONG_VIEC,
                                anhGiu(1200, 800, "e0f2f1", "0f766e", "eClinic"), chuThichMau(mau, 1)),
                        anhMau(idCuoi + 2, 2, LoaiAnhBacSi.ANH_CONG_VIEC,
                                anhGiu(1200, 800, "e3f2fd", "1565c0", "eClinic"), chuThichMau(mau, 2)),
                        anhMau(idCuoi + 3, 3, LoaiAnhBacSi.CHUNG_CHI,
                                anhGiu(900, 1200, "fff8e1", "8d6e63", mau.soGiayPhep()), chuThichMau(mau, 3))));
            }
        }
        if (!chuaCoHoSo.isEmpty()) {
            log.info("Đã bổ sung hồ sơ giới thiệu mẫu cho {} bác sĩ", chuaCoHoSo.size());
        }
        return chuaCoHoSo.size();
    }

    /** Chú thích tiếng Việt của ảnh mẫu thứ {@code thuTu} (1-3). */
    private static String chuThichMau(BacSiMau mau, int thuTu) {
        return switch (thuTu) {
            case 1 -> "Bác sĩ " + mau.hoTen() + " tại phòng khám " + mau.chuyenKhoa();
            case 2 -> "Tư vấn cho người bệnh";
            default -> "Chứng chỉ hành nghề số " + mau.soGiayPhep();
        };
    }

    /** Bản tiếng Anh của {@link #chuThichMau}; họ tên bác sĩ giữ nguyên. */
    private static String chuThichMauTiengAnh(BacSiMau mau, int thuTu) {
        return switch (thuTu) {
            case 1 -> "Dr. " + mau.hoTen() + " at the "
                    + DuLieuMauTiengAnh.CHUYEN_KHOA.get(mau.chuyenKhoa()).ten() + " clinic";
            case 2 -> "Consulting a patient";
            default -> "Practising certificate no. " + mau.soGiayPhep();
        };
    }

    @Override
    @Transactional
    public int boSungTiengAnh() {
        int soDong = 0;
        for (ChuyenKhoaMau mau : DuLieuMau.CHUYEN_KHOA) {
            ChuyenKhoaEn en = DuLieuMauTiengAnh.CHUYEN_KHOA.get(mau.ten());
            ChuyenKhoa chuyenKhoa = entityManager
                    .createQuery("select c from ChuyenKhoa c where c.tenChuyenKhoa = :ten", ChuyenKhoa.class)
                    .setParameter("ten", mau.ten()).getResultStream().findFirst().orElse(null);
            // Tên tiếng Anh là UNIQUE: quản trị viên đã đặt tên đó cho chuyên khoa khác thì bỏ qua
            if (chuyenKhoa != null && chuyenKhoa.getTenChuyenKhoaEn() == null
                    && chuaCo("select count(c) from ChuyenKhoa c where c.tenChuyenKhoaEn = :ten", en.ten())) {
                chuyenKhoa.setTenChuyenKhoaEn(en.ten());
                chuyenKhoa.setMoTaEn(banTiengAnh(chuyenKhoa.getMoTa(), mau.moTa(), chuyenKhoa.getMoTaEn(), en.moTa()));
                soDong++;
            }
            PhongKham phongKham = timPhongKham(mau.tenPhong());
            if (phongKham != null && phongKham.getTenPhongEn() == null
                    && chuaCo("select count(p) from PhongKham p where p.tenPhongEn = :ten", en.tenPhong())) {
                phongKham.setTenPhongEn(en.tenPhong());
                phongKham.setTangEn(banTiengAnh(phongKham.getTang(), mau.tang(), phongKham.getTangEn(), en.tang()));
                soDong++;
            }
        }

        Map<String, BacSiMau> mauTheoEmail = DuLieuMau.BAC_SI.stream()
                .collect(Collectors.toMap(BacSiMau::email, Function.identity()));
        List<BacSi> chuaCoTiengAnh = entityManager.createQuery("""
                select b from BacSi b join fetch b.taiKhoan t
                where t.email in :emails and b.gioiThieuNgan is not null and b.gioiThieuNganEn is null
                order by b.id
                """, BacSi.class)
                .setParameter("emails", mauTheoEmail.keySet())
                .getResultList();
        for (BacSi bacSi : chuaCoTiengAnh) {
            BacSiMau mau = mauTheoEmail.get(bacSi.getTaiKhoan().getEmail());
            HoSoBacSiMau vi = HoSoBacSiMau.THEO_SO.get(mau.so());
            HoSoBacSiEn en = DuLieuMauTiengAnh.BAC_SI.get(mau.so());
            bacSi.setTieuSuEn(banTiengAnh(bacSi.getTieuSu(), mau.tieuSu(), bacSi.getTieuSuEn(), en.tieuSu()));
            bacSi.setChucVuEn(banTiengAnh(bacSi.getChucVu(), vi.chucVu(), bacSi.getChucVuEn(), en.chucVu()));
            bacSi.setGioiThieuNganEn(banTiengAnh(bacSi.getGioiThieuNgan(), vi.gioiThieuNgan(), null,
                    en.gioiThieuNgan()));
            bacSi.setQuaTrinhDaoTaoEn(banTiengAnh(bacSi.getQuaTrinhDaoTao(), String.join("\n", vi.daoTao()),
                    bacSi.getQuaTrinhDaoTaoEn(), String.join("\n", en.daoTao())));
            bacSi.setQuaTrinhCongTacEn(banTiengAnh(bacSi.getQuaTrinhCongTac(), String.join("\n", vi.congTac()),
                    bacSi.getQuaTrinhCongTacEn(), String.join("\n", en.congTac())));
            bacSi.setLinhVucKhamChuaEn(banTiengAnh(bacSi.getLinhVucKhamChua(), String.join("\n", vi.linhVuc()),
                    bacSi.getLinhVucKhamChuaEn(), String.join("\n", en.linhVuc())));

            // Ảnh mẫu: không nằm trong kho ảnh (maLuuTru null), chú thích còn đúng như lúc tạo
            List<AnhBacSi> danhSachAnh = docAnhGioiThieu(bacSi.getId());
            boolean doiAnh = false;
            for (AnhBacSi anh : danhSachAnh) {
                if (anh.getMaLuuTru() == null && anh.getChuThichEn() == null && anh.getThuTu() >= 1
                        && anh.getThuTu() <= 3) {
                    anh.setChuThichEn(banTiengAnh(anh.getChuThich(), chuThichMau(mau, anh.getThuTu()), null,
                            chuThichMauTiengAnh(mau, anh.getThuTu())));
                    doiAnh |= anh.getChuThichEn() != null;
                }
            }
            if (doiAnh) {
                ghiAnhGioiThieu(bacSi.getId(), danhSachAnh);
            }
            soDong++;
        }
        if (soDong > 0) {
            log.info("Đã bổ sung bản tiếng Anh cho {} chuyên khoa / phòng khám / bác sĩ mẫu", soDong);
        }
        return soDong;
    }

    @Override
    @Transactional
    public int boSungGiayPhepVaNgonNgu() {
        Map<String, BacSiMau> mauTheoEmail = DuLieuMau.BAC_SI.stream()
                .collect(Collectors.toMap(BacSiMau::email, Function.identity()));
        List<BacSi> chuaCo = entityManager.createQuery("""
                select b from BacSi b join fetch b.taiKhoan t
                where t.email in :emails and b.ngonNguKham is null
                order by b.id
                """, BacSi.class)
                .setParameter("emails", mauTheoEmail.keySet())
                .getResultList();
        for (BacSi bacSi : chuaCo) {
            BacSiMau mau = mauTheoEmail.get(bacSi.getTaiKhoan().getEmail());
            bacSi.setNgonNguKham(HoSoBacSiMau.KHAM_TIENG_ANH.contains(mau.so()) ? "vi,en" : "vi");
            if (bacSi.getNgayCapGiayPhep() == null && bacSi.getNoiCapGiayPhep() == null) {
                int nam = bacSi.getNamBatDauHanhNghe() != null ? bacSi.getNamBatDauHanhNghe() : mau.namBatDauHanhNghe();
                LocalDate ngayCap = LocalDate.of(nam, 9, 15);
                bacSi.setNgayCapGiayPhep(ngayCap.isAfter(LocalDate.now()) ? LocalDate.now() : ngayCap);
                bacSi.setNoiCapGiayPhep("Sở Y tế TP. Hồ Chí Minh");
                bacSi.setNoiCapGiayPhepEn(DuLieuMauTiengAnh.NOI_CAP_GIAY_PHEP);
            }
            if (bacSi.getPhamViHanhNghe() == null) {
                bacSi.setPhamViHanhNghe("Khám bệnh, chữa bệnh chuyên khoa " + mau.chuyenKhoa());
                bacSi.setPhamViHanhNgheEn("Medical examination and treatment in "
                        + DuLieuMauTiengAnh.CHUYEN_KHOA.get(mau.chuyenKhoa()).ten());
            }
            HoSoBacSiMau.GiaiThuongMau vi = HoSoBacSiMau.GIAI_THUONG.get(mau.so());
            if (vi != null && bacSi.getGiaiThuong() == null && bacSi.getCongTrinhNghienCuu() == null) {
                HoSoBacSiMau.GiaiThuongMau en = DuLieuMauTiengAnh.GIAI_THUONG.get(mau.so());
                bacSi.setGiaiThuong(String.join("\n", vi.giaiThuong()));
                bacSi.setGiaiThuongEn(String.join("\n", en.giaiThuong()));
                bacSi.setCongTrinhNghienCuu(String.join("\n", vi.nghienCuu()));
                bacSi.setCongTrinhNghienCuuEn(String.join("\n", en.nghienCuu()));
            }
        }
        if (!chuaCo.isEmpty()) {
            log.info("Đã bổ sung giấy phép, ngôn ngữ khám mẫu cho {} bác sĩ", chuaCo.size());
        }
        return chuaCo.size();
    }

    @Override
    @Transactional
    public int boSungTrieuChung() {
        int soDong = 0;
        for (ChuyenKhoaMau mau : DuLieuMau.CHUYEN_KHOA) {
            ChuyenKhoa chuyenKhoa = entityManager
                    .createQuery("select c from ChuyenKhoa c where c.tenChuyenKhoa = :ten", ChuyenKhoa.class)
                    .setParameter("ten", mau.ten()).getResultStream().findFirst().orElse(null);
            if (chuyenKhoa != null && chuyenKhoa.getTrieuChung() == null && chuyenKhoa.getTrieuChungEn() == null) {
                chuyenKhoa.setTrieuChung(DuLieuMau.TRIEU_CHUNG.get(mau.ten()));
                chuyenKhoa.setTrieuChungEn(DuLieuMauTiengAnh.TRIEU_CHUNG.get(mau.ten()));
                soDong++;
            }
        }
        return soDong;
    }

    @Override
    @Transactional
    public int boSungThuoc() {
        int soThuoc = 0;
        for (ThuocMau mau : DuLieuMau.THUOC) {
            // Cùng cách chuẩn hoá với medical-service: khoá UNIQUE là tên + hàm lượng
            String tenChuanHoa = chuanHoaThuoc(mau.ten());
            String hamLuongChuanHoa = chuanHoaThuoc(mau.hamLuong());
            boolean daCo = entityManager.createQuery("""
                    select count(t) from Thuoc t where t.tenChuanHoa = :ten and t.hamLuongChuanHoa = :hamLuong
                    """, Long.class)
                    .setParameter("ten", tenChuanHoa).setParameter("hamLuong", hamLuongChuanHoa)
                    .getSingleResult() > 0;
            if (daCo) {
                continue;
            }
            DuLieuMauTiengAnh.ThuocEn en = DuLieuMauTiengAnh.THUOC.get(mau.ten());
            Thuoc thuoc = new Thuoc();
            thuoc.setTenThuoc(mau.ten());
            thuoc.setTenChuanHoa(tenChuanHoa);
            thuoc.setHoatChat(mau.hoatChat());
            thuoc.setHamLuong(mau.hamLuong());
            thuoc.setHamLuongChuanHoa(hamLuongChuanHoa);
            thuoc.setDangBaoChe(mau.dangBaoChe());
            thuoc.setDangBaoCheEn(en.dangBaoChe());
            thuoc.setDuongDung(mau.duongDung());
            thuoc.setDonVi(mau.donVi());
            thuoc.setDonViEn(en.donVi());
            thuoc.setMoTa(mau.moTa());
            thuoc.setMoTaEn(en.moTa());
            thuoc.setDaXacMinh(true);
            thuoc.setTrangThai(TrangThaiThuoc.DANG_DUNG);
            entityManager.persist(thuoc);
            soThuoc++;
        }
        return soThuoc;
    }

    private static String chuanHoaThuoc(String chuoi) {
        return chuoi.trim().replaceAll("\s+", " ").toLowerCase(Locale.ROOT);
    }

    private boolean chuaCo(String cauDem, String ten) {
        return entityManager.createQuery(cauDem, Long.class).setParameter("ten", ten).getSingleResult() == 0;
    }

    /** Bản tiếng Anh mẫu, chỉ khi chưa có bản tiếng Anh và bản tiếng Việt vẫn đúng như dữ liệu mẫu. */
    private static String banTiengAnh(String tiengViet, String tiengVietMau, String tiengAnh, String tiengAnhMau) {
        return tiengAnh == null && tiengVietMau.equals(tiengViet) ? tiengAnhMau : tiengAnh;
    }

    /** Ảnh mẫu không nằm trong kho ảnh ({@code maLuuTru} null): tạo dữ liệu mẫu không gọi Cloudinary. */
    private static AnhBacSi anhMau(long id, int thuTu, LoaiAnhBacSi loai, String url, String chuThich) {
        AnhBacSi anh = new AnhBacSi();
        anh.setId(id);
        anh.setLoai(loai);
        anh.setUrl(url);
        anh.setChuThich(chuThich);
        anh.setThuTu(thuTu);
        anh.setNgayTao(LocalDateTime.now());
        return anh;
    }

    /** Ảnh giới thiệu của bác sĩ (mảng JSON trên bac_si, xem {@link AnhBacSi}); khoá dòng tới hết transaction. */
    private List<AnhBacSi> docAnhGioiThieu(Long idBacSi) {
        List<?> json = entityManager.createNativeQuery(
                        "SELECT CAST(anh_gioi_thieu AS CHAR) FROM bac_si WHERE id_bac_si = :id FOR UPDATE", String.class)
                .setParameter("id", idBacSi)
                .getResultList();
        return AnhBacSi.docDanhSach(json.isEmpty() ? null : (String) json.get(0));
    }

    /** Ghi lại cả danh sách ảnh (không rỗng) và nâng bộ đếm id ảnh của bác sĩ qua id lớn nhất trong đó. */
    private void ghiAnhGioiThieu(Long idBacSi, List<AnhBacSi> danhSach) {
        entityManager.createNativeQuery("UPDATE bac_si SET anh_gioi_thieu = CAST(:json AS JSON),"
                        + " anh_id_cuoi = GREATEST(anh_id_cuoi, :idLonNhat) WHERE id_bac_si = :id")
                .setParameter("json", AnhBacSi.vietDanhSach(danhSach))
                .setParameter("idLonNhat", danhSach.stream().mapToLong(AnhBacSi::getId).max().orElse(0))
                .setParameter("id", idBacSi)
                .executeUpdate();
    }

    /** Ảnh giữ chỗ (nền màu + 1 dòng chữ không dấu) của placehold.co, thay cho ảnh thật khi demo. */
    private static String anhGiu(int rong, int cao, String mauNen, String mauChu, String chu) {
        return "https://placehold.co/%dx%d/%s/%s/png?text=%s".formatted(rong, cao, mauNen, mauChu,
                chu.replace(' ', '+'));
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
        Set<BacSiNgay> ngayDaCoCa = xepCaMau.timNgayDaCoCa(phongCuaBacSi.keySet(), tuNgay, denNgay);
        Set<Long> idPhong = phongCuaBacSi.values().stream().map(PhongKham::getId).collect(Collectors.toSet());
        Map<PhongNgay, List<KhoangGio>> caCuaPhong = xepCaMau.timCaDangHoatDongCuaPhong(idPhong, tuNgay, denNgay);

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
                xepCaMau.taoCa(bacSi, phongKham, adminTao, ngay, buoi);
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

}
