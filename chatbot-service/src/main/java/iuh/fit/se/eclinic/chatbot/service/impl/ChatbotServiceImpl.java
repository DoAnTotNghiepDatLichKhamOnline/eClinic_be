package iuh.fit.se.eclinic.chatbot.service.impl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.springframework.context.MessageSource;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import iuh.fit.se.eclinic.chatbot.client.LuotChat;
import iuh.fit.se.eclinic.chatbot.client.MoHinhNgonNgu;
import iuh.fit.se.eclinic.chatbot.client.PhanHoiMoHinh;
import iuh.fit.se.eclinic.chatbot.client.PhanHoiMoHinh.GoiCongCu;
import iuh.fit.se.eclinic.chatbot.config.ChatbotProperties;
import iuh.fit.se.eclinic.chatbot.dto.response.GoiYResponse;
import iuh.fit.se.eclinic.chatbot.dto.response.PhienChatResponse;
import iuh.fit.se.eclinic.chatbot.dto.response.PhienChatTomTatResponse;
import iuh.fit.se.eclinic.chatbot.dto.response.TinNhanChatResponse;
import iuh.fit.se.eclinic.chatbot.dto.response.TraLoiResponse;
import iuh.fit.se.eclinic.chatbot.mapper.TinNhanChatMapper;
import iuh.fit.se.eclinic.chatbot.repository.ChuyenKhoaChiDocRepository;
import iuh.fit.se.eclinic.chatbot.repository.PhienChatRepository;
import iuh.fit.se.eclinic.chatbot.repository.PhienChatRepository.ChuPhien;
import iuh.fit.se.eclinic.chatbot.repository.TinNhanChatRepository;
import iuh.fit.se.eclinic.chatbot.service.ChatbotService;
import iuh.fit.se.eclinic.chatbot.service.GioiHanChatService;
import iuh.fit.se.eclinic.chatbot.service.KiemTraKhanCap;
import iuh.fit.se.eclinic.chatbot.service.LoiNhacHeThong;
import iuh.fit.se.eclinic.chatbot.service.NguoiHoi;
import iuh.fit.se.eclinic.chatbot.service.TraCuuPhongKham;
import iuh.fit.se.eclinic.common.entity.chatbot.PhienChat;
import iuh.fit.se.eclinic.common.entity.chatbot.TinNhanChat;
import iuh.fit.se.eclinic.common.entity.identity.TaiKhoan;
import iuh.fit.se.eclinic.common.enums.LoaiTinNhanChat;
import iuh.fit.se.eclinic.common.enums.NgonNgu;
import iuh.fit.se.eclinic.common.enums.NguoiGui;
import iuh.fit.se.eclinic.common.enums.VaiTro;
import iuh.fit.se.eclinic.common.exception.LoiNghiepVu;
import iuh.fit.se.eclinic.common.exception.MaLoi;
import iuh.fit.se.eclinic.common.security.NguoiDungHienTai;
import iuh.fit.se.eclinic.common.util.TokenNgauNhien;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Chuỗi xử lý 1 tin nhắn, thứ tự cố định (docs/QUY-UOC-CODE.md mục 7):
 * chủ phiên → giới hạn của người gửi → dấu hiệu cấp cứu → giới hạn toàn hệ thống → mô hình → dấu hiệu ngoài phạm vi → lưu.
 * <p>
 * Bước "mô hình" là 1 vòng có giới hạn ({@link #hoiMoHinh}): mô hình có thể xin tra cứu bác sĩ / lịch khám
 * ({@link TraCuuPhongKham}) vài lần rồi mới trả lời. Gợi ý trả cho người dùng ({@code goiY}) dựng từ kết quả tra cứu,
 * không từ câu chữ của mô hình.
 * <p>
 * Dữ liệu cá nhân: {@link NguoiHoi} (dựng từ JWT và chủ phiên ở {@link #nguoiHoi}) quyết định mô hình có được xin 2 hàm
 * "của tôi" không. Bệnh nhân chưa đồng ý trong phiên thì lần đầu mô hình xin là dừng, trả câu xin đồng ý cố định
 * (CAN_DONG_Y), chưa đọc gì.
 * <p>
 * Không có @Transactional ở lớp: lời gọi mô hình (tới 20 giây) không được nằm trong transaction. Chỉ bước lưu chạy
 * trong transaction ({@link TransactionTemplate}).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatbotServiceImpl implements ChatbotService {

    private static final int SO_PHIEN_TOI_DA = 50;
    /** Số lời gọi công cụ được chạy trong 1 lượt mô hình xin tra cứu; lời gọi thừa nhận kết quả báo lỗi. */
    private static final int SO_TRA_CUU_MOI_LUOT = 4;
    /** Còn ít hơn chừng này thì không gọi mô hình nữa: không kịp trả lời trước khi gateway cắt. */
    private static final Duration THOI_GIAN_TOI_THIEU = Duration.ofSeconds(1);

    /**
     * Câu trả lời của mô hình kèm gợi ý dựng từ các lần tra cứu của tin nhắn này (null khi không tra cứu được gì).
     * {@code canDongY}: mô hình xin dữ liệu cá nhân khi bệnh nhân chưa đồng ý; không có câu trả lời nào.
     */
    private record TraLoiMoHinh(String noiDung, GoiYResponse goiY, boolean canDongY) {
    }

    private final PhienChatRepository phienChatRepository;
    private final TinNhanChatRepository tinNhanChatRepository;
    private final TinNhanChatMapper tinNhanChatMapper;
    private final GioiHanChatService gioiHanChatService;
    private final KiemTraKhanCap kiemTraKhanCap;
    private final LoiNhacHeThong loiNhacHeThong;
    private final MoHinhNgonNgu moHinhNgonNgu;
    private final TraCuuPhongKham traCuuPhongKham;
    private final ChuyenKhoaChiDocRepository chuyenKhoaRepository;
    private final ChatbotProperties properties;
    private final MessageSource messageSource;
    private final TransactionTemplate transactionTemplate;
    private final EntityManager entityManager;

    @Override
    public PhienChatResponse moPhien(String diaChiIp) {
        Long idTaiKhoan = idTaiKhoanHienTai();
        gioiHanChatService.ghiNhanMoPhien(chuThe(idTaiKhoan, diaChiIp));
        String maKhach = idTaiKhoan == null ? TokenNgauNhien.tao() : null;
        String loiChao = cauCoDinh("chatbot.loiChao");

        return transactionTemplate.execute(trangThai -> {
            PhienChat phien = new PhienChat();
            if (idTaiKhoan != null) {
                phien.setTaiKhoan(entityManager.getReference(TaiKhoan.class, idTaiKhoan));
            } else {
                phien.setMaDinhDanhKhach(TokenNgauNhien.bam(maKhach));
            }
            phienChatRepository.save(phien);
            TinNhanChat chao = luu(phien, NguoiGui.CHATBOT, LoaiTinNhanChat.LOI_CHAO, loiChao);
            return new PhienChatResponse(phien.getId(), maKhach, phien.getThoiGianBatDau(),
                    List.of(tinNhanChatMapper.toResponse(chao)));
        });
    }

    @Override
    public TraLoiResponse guiTinNhan(Long idPhien, String maKhach, String noiDung, String diaChiIp) {
        NguoiHoi nguoiHoi = nguoiHoi(kiemTraChuPhien(idPhien, maKhach));
        gioiHanChatService.ghiNhanTinNhan(chuThe(idTaiKhoanHienTai(), diaChiIp));
        String cauHoi = noiDung.strip();

        LoaiTinNhanChat loai;
        String traLoi;
        String goiY = null;
        if (kiemTraKhanCap.laKhanCap(cauHoi)) {
            loai = LoaiTinNhanChat.KHAN_CAP;
            traLoi = cauCoDinh("chatbot.khanCap");
        } else {
            TraLoiMoHinh cuaMoHinh = hoiMoHinh(idPhien, cauHoi, nguoiHoi);
            if (cuaMoHinh.canDongY()) {
                loai = LoaiTinNhanChat.CAN_DONG_Y;
                traLoi = cauCoDinh("chatbot.canDongY");
            } else if (laNgoaiPhamVi(cuaMoHinh.noiDung()) || loiNhacHeThong.biLo(cuaMoHinh.noiDung())) {
                // Gợi ý (nếu mô hình đã tra cứu trước khi từ chối) bỏ đi cùng câu trả lời của nó. Câu trả lời chép lại
                // chỉ dẫn hệ thống cũng bị thay bằng câu từ chối.
                loai = LoaiTinNhanChat.NGOAI_PHAM_VI;
                traLoi = cauCoDinh("chatbot.ngoaiPhamVi");
            } else {
                loai = LoaiTinNhanChat.TRA_LOI;
                traLoi = cuaMoHinh.noiDung().strip();
                goiY = tinNhanChatMapper.ghiGoiY(cuaMoHinh.goiY());
            }
        }

        String goiYDaGhi = goiY;
        return transactionTemplate.execute(trangThai -> {
            PhienChat phien = phienChatRepository.getReferenceById(idPhien);
            TinNhanChat hoi = luu(phien, NguoiGui.NGUOI_DUNG, LoaiTinNhanChat.CAU_HOI, cauHoi);
            TinNhanChat dap = luu(phien, NguoiGui.CHATBOT, loai, traLoi);
            dap.setGoiY(goiYDaGhi);
            return new TraLoiResponse(tinNhanChatMapper.toResponse(hoi), tinNhanChatMapper.toResponse(dap));
        });
    }

    /**
     * Gọi mô hình tới khi nó trả lời: mỗi lần nó xin tra cứu thì chạy các hàm tra cứu rồi gọi lại kèm kết quả. Có 2 chặn:
     * số lần gọi mô hình ({@code so-lan-goi-mo-hinh-toi-da}; lần cuối mô hình không được xin tra cứu nữa) và tổng thời
     * gian ({@code tong-thoi-gian-cho}, nhỏ hơn read-timeout của gateway). Vượt chặn nào cũng ra 503, không lưu gì.
     * Mỗi lần gọi mô hình đều tính vào giới hạn toàn hệ thống.
     */
    private TraLoiMoHinh hoiMoHinh(Long idPhien, String cauHoi, NguoiHoi nguoiHoi) {
        long hanChot = System.nanoTime() + properties.tongThoiGianCho().toNanos();
        NgonNgu ngonNgu = NgonNgu.hienTai();
        String loiNhac = loiNhacHeThong.soan(ngonNgu, chuyenKhoaRepository.findAllByOrderByIdAsc(), LocalDate.now(),
                nguoiHoi.duocTraCuuCaNhan());
        List<LuotChat> hoiThoai = hoiThoai(idPhien, cauHoi, nguoiHoi);

        GoiYResponse.ChuyenKhoa chuyenKhoa = null;
        GoiYResponse.ChuyenKhoa chuyenKhoaCuaLich = null;
        List<GoiYResponse.BacSi> bacSi = List.of();
        List<GoiYResponse.LichKham> lichKham = List.of();
        List<GoiYResponse.LichHenCuaToi> lichHenCuaToi = null;
        List<GoiYResponse.LanKham> lanKham = null;
        int soTraCuu = 0;
        for (int lan = 1; lan <= properties.soLanGoiMoHinhToiDa(); lan++) {
            Duration conLai = Duration.ofNanos(hanChot - System.nanoTime());
            if (conLai.compareTo(THOI_GIAN_TOI_THIEU) < 0) {
                log.warn("Chatbot hết thời gian cho 1 tin nhắn sau {} lần gọi mô hình", lan - 1);
                throw new LoiNghiepVu(MaLoi.CHATBOT_KHONG_KHA_DUNG);
            }
            boolean lanCuoi = lan == properties.soLanGoiMoHinhToiDa();
            gioiHanChatService.ghiNhanGoiMoHinh();
            PhanHoiMoHinh phanHoi = moHinhNgonNgu.traLoi(loiNhac, hoiThoai, traCuuPhongKham.congCu(nguoiHoi), !lanCuoi,
                    conLai.compareTo(properties.thoiGianCho()) < 0 ? conLai : properties.thoiGianCho());
            if (phanHoi.goiCongCu().isEmpty() || (lanCuoi && phanHoi.coNoiDung())) {
                GoiYResponse.ChuyenKhoa cuaGoiY = chuyenKhoa != null ? chuyenKhoa : chuyenKhoaCuaLich;
                boolean coGoiY = cuaGoiY != null || !bacSi.isEmpty() || !lichKham.isEmpty() || lichHenCuaToi != null
                        || lanKham != null;
                log.debug("Chatbot trả lời sau {} lần gọi mô hình, {} lần tra cứu", lan, soTraCuu);
                return new TraLoiMoHinh(phanHoi.noiDung(),
                        coGoiY ? new GoiYResponse(cuaGoiY, bacSi, lichKham, lichHenCuaToi, lanKham) : null, false);
            }
            if (lanCuoi) {
                break;
            }
            hoiThoai.add(LuotChat.xinTraCuu(phanHoi.tinNhanGoc()));
            int thuTu = 0;
            for (GoiCongCu goi : phanHoi.goiCongCu()) {
                // Mọi lời gọi đều phải có kết quả gửi lại, kể cả lời gọi thừa
                TraCuuPhongKham.KetQua ketQua = ++thuTu <= SO_TRA_CUU_MOI_LUOT
                        ? traCuuPhongKham.thucHien(goi.ten(), goi.thamSo(), nguoiHoi)
                        : traCuuPhongKham.thucHien(null, null, nguoiHoi);
                if (ketQua.canDongY()) {
                    // Chưa đọc gì của bệnh nhân; dừng hẳn, không gửi gì thêm cho mô hình
                    return new TraLoiMoHinh(null, null, true);
                }
                soTraCuu++;
                hoiThoai.add(LuotChat.ketQuaTraCuu(goi.id(), ketQua.choMoHinh()));
                // Gợi ý lấy kết quả cuối cùng của từng loại tra cứu
                if (ketQua.bacSi() != null) {
                    chuyenKhoa = ketQua.chuyenKhoa();
                    bacSi = ketQua.bacSi();
                }
                if (ketQua.lichKham() != null) {
                    chuyenKhoaCuaLich = ketQua.chuyenKhoa();
                    lichKham = ketQua.lichKham();
                }
                if (ketQua.lichHenCuaToi() != null) {
                    lichHenCuaToi = ketQua.lichHenCuaToi();
                }
                if (ketQua.lanKham() != null) {
                    lanKham = ketQua.lanKham();
                }
            }
        }
        log.warn("Chatbot: mô hình vẫn xin tra cứu sau {} lần gọi", properties.soLanGoiMoHinhToiDa());
        throw new LoiNghiepVu(MaLoi.CHATBOT_KHONG_KHA_DUNG);
    }

    @Override
    public List<TinNhanChatResponse> lichSu(Long idPhien, String maKhach) {
        kiemTraChuPhien(idPhien, maKhach);
        return tinNhanChatRepository.findByPhienChatIdOrderByIdAsc(idPhien).stream()
                .map(tinNhanChatMapper::toResponse)
                .toList();
    }

    @Override
    public List<PhienChatTomTatResponse> phienCuaToi() {
        return phienChatRepository
                .findByTaiKhoanIdOrderByIdDesc(NguoiDungHienTai.layIdTaiKhoan(), Limit.of(SO_PHIEN_TOI_DA)).stream()
                .map(phien -> new PhienChatTomTatResponse(phien.getId(), phien.getThoiGianBatDau(),
                        phien.getDongYDuLieuLuc() != null))
                .toList();
    }

    @Override
    public void datDongY(Long idPhien, boolean dongY) {
        // Không gửi mã khách: phiên của khách ra 404 như phiên không tồn tại, chỉ phiên của chính tài khoản mới đồng ý được
        kiemTraChuPhien(idPhien, null);
        transactionTemplate.executeWithoutResult(trangThai -> phienChatRepository.findById(idPhien)
                .ifPresent(phien -> phien.setDongYDuLieuLuc(dongY ? LocalDateTime.now() : null)));
    }

    /**
     * Người đang chat có được tra cứu lịch hẹn, lượt khám của mình không: tính năng đang bật, request có JWT vai trò bệnh
     * nhân, và phiên thuộc chính tài khoản đó ({@link #kiemTraChuPhien} đã so id). Danh tính luôn lấy từ JWT của request
     * này, không lấy từ dòng phiên chat.
     */
    private NguoiHoi nguoiHoi(ChuPhien chu) {
        boolean duoc = properties.traCuuCaNhan() && chu.idTaiKhoan() != null && NguoiDungHienTai.daDangNhap()
                && NguoiDungHienTai.layVaiTro() == VaiTro.BENH_NHAN
                && chu.idTaiKhoan().equals(NguoiDungHienTai.layIdTaiKhoan());
        return duoc ? new NguoiHoi(NguoiDungHienTai.layGiaTriToken(), true, chu.dongYDuLieuLuc() != null)
                : NguoiHoi.KHONG_CO_DU_LIEU_CA_NHAN;
    }

    /**
     * Phiên của tài khoản: người gọi phải đăng nhập bằng đúng tài khoản đó. Phiên của khách: người gọi phải gửi đúng mã
     * bí mật (so bản băm, thời gian so không phụ thuộc vị trí sai). Mọi trường hợp sai, kể cả phiên không tồn tại, ra
     * cùng 1 lỗi để không dò được id phiên nào có thật.
     */
    private ChuPhien kiemTraChuPhien(Long idPhien, String maKhach) {
        ChuPhien chu = phienChatRepository.timChuPhien(idPhien).orElseThrow(ChatbotServiceImpl::khongTimThayPhien);
        boolean laChu;
        if (chu.idTaiKhoan() != null) {
            laChu = chu.idTaiKhoan().equals(idTaiKhoanHienTai());
        } else {
            laChu = maKhach != null && !maKhach.isBlank() && chu.maDinhDanhKhach() != null
                    && MessageDigest.isEqual(TokenNgauNhien.bam(maKhach).getBytes(StandardCharsets.US_ASCII),
                            chu.maDinhDanhKhach().getBytes(StandardCharsets.US_ASCII));
        }
        if (!laChu) {
            throw khongTimThayPhien();
        }
        return chu;
    }

    private static LoiNghiepVu khongTimThayPhien() {
        return new LoiNghiepVu(MaLoi.KHONG_TIM_THAY, "loi.KHONG_TIM_THAY.phienChat");
    }

    /**
     * Các cặp hỏi - đáp gần nhất của CHÍNH phiên này rồi tới câu hỏi mới. Chỉ lấy cặp mà mô hình đã trả lời (TRA_LOI) hoặc
     * đã từ chối (gửi lại đúng dấu hiệu, không gửi câu từ chối của server); câu chào và cặp cấp cứu không gửi. Các lượt
     * tra cứu của tin nhắn cũ không được lưu nên không gửi lại; thay vào đó câu trả lời nào có gợi ý thì được gắn thêm 1
     * dòng ghi chú các id đã hiện ({@link #ghiChuDaHien}).
     * <p>
     * Cặp có câu trả lời dựng từ lịch hẹn, lượt khám của bệnh nhân chỉ gửi lại khi request này vẫn được đọc dữ liệu đó
     * (đúng bệnh nhân, còn đồng ý); rút lại đồng ý thì các câu trả lời cũ đó cũng thôi được gửi cho nhà cung cấp. Cặp
     * CAN_DONG_Y không gửi.
     */
    private List<LuotChat> hoiThoai(Long idPhien, String cauHoi, NguoiHoi nguoiHoi) {
        List<TinNhanChat> ganNhat = new ArrayList<>(
                tinNhanChatRepository.findByPhienChatIdOrderByIdDesc(idPhien, Limit.of(properties.soTinNhanLichSu())));
        Collections.reverse(ganNhat);
        List<LuotChat> hoiThoai = new ArrayList<>();
        for (int i = 0; i + 1 < ganNhat.size(); i++) {
            TinNhanChat hoi = ganNhat.get(i);
            TinNhanChat dap = ganNhat.get(i + 1);
            if (hoi.getNguoiGui() != NguoiGui.NGUOI_DUNG || dap.getNguoiGui() != NguoiGui.CHATBOT) {
                continue;
            }
            if (dap.getLoai() == LoaiTinNhanChat.TRA_LOI) {
                GoiYResponse goiY = tinNhanChatMapper.docGoiY(dap);
                if (goiY == null || !goiY.coDuLieuCaNhan() || nguoiHoi.duocDoc()) {
                    hoiThoai.add(LuotChat.nguoiDung(hoi.getNoiDung()));
                    hoiThoai.add(LuotChat.chatbot(dap.getNoiDung() + ghiChuDaHien(goiY)));
                }
            } else if (dap.getLoai() == LoaiTinNhanChat.NGOAI_PHAM_VI) {
                hoiThoai.add(LuotChat.nguoiDung(hoi.getNoiDung()));
                hoiThoai.add(LuotChat.chatbot(LoiNhacHeThong.DANH_DAU_NGOAI_PHAM_VI));
            }
            i++;
        }
        hoiThoai.add(LuotChat.nguoiDung(cauHoi));
        return hoiThoai;
    }

    /**
     * Dòng ghi chú cho mô hình (không lưu, không trả cho người dùng): chuyên khoa và bác sĩ đã hiện cùng 1 câu trả lời cũ,
     * kèm id, để câu hỏi tiếp theo ("thứ bảy thì sao?") tra cứu tiếp được mà không phải hỏi lại. Rỗng khi không có gợi ý.
     * Với lịch hẹn, lượt khám của bệnh nhân chỉ ghi là "đã hiện", không ghi dữ liệu nào: muốn nói tiếp về chúng mô hình
     * phải tra cứu lại, và lần tra cứu đó lại qua kiểm tra đồng ý.
     */
    private static String ghiChuDaHien(GoiYResponse goiY) {
        if (goiY == null) {
            return "";
        }
        StringBuilder ghiChu = new StringBuilder("\n").append(LoiNhacHeThong.GHI_CHU_DA_HIEN);
        if (goiY.chuyenKhoa() != null) {
            ghiChu.append(" specialty id ").append(goiY.chuyenKhoa().id()).append(' ').append(goiY.chuyenKhoa().ten())
                    .append(';');
        }
        if (goiY.bacSi() != null && !goiY.bacSi().isEmpty()) {
            ghiChu.append(" doctors:");
            for (GoiYResponse.BacSi bacSi : goiY.bacSi()) {
                ghiChu.append(" id ").append(bacSi.id()).append(' ').append(bacSi.hoTen()).append(',');
            }
            ghiChu.setCharAt(ghiChu.length() - 1, ';');
        }
        if (goiY.lichKham() != null && !goiY.lichKham().isEmpty()) {
            GoiYResponse.LichKham dau = goiY.lichKham().get(0);
            ghiChu.append(" schedule shown for doctor id ").append(dau.idBacSi()).append(' ').append(dau.hoTenBacSi())
                    .append(" from ").append(dau.ngay()).append(';');
        }
        if (goiY.lichHenCuaToi() != null) {
            ghiChu.append(" the user's own upcoming appointments were shown as cards;");
        }
        if (goiY.lanKham() != null) {
            ghiChu.append(" the user's own finished visits were shown as cards;");
        }
        return ghiChu.toString();
    }

    /** Mô hình có thể viết thêm chữ quanh dấu hiệu hoặc bỏ ngoặc vuông: có tên dấu hiệu là đủ. */
    private static boolean laNgoaiPhamVi(String traLoi) {
        if (traLoi == null) {
            return false;
        }
        String ten = LoiNhacHeThong.DANH_DAU_NGOAI_PHAM_VI.substring(1,
                LoiNhacHeThong.DANH_DAU_NGOAI_PHAM_VI.length() - 1);
        return traLoi.toUpperCase(Locale.ROOT).contains(ten);
    }

    private TinNhanChat luu(PhienChat phien, NguoiGui nguoiGui, LoaiTinNhanChat loai, String noiDung) {
        TinNhanChat tinNhan = new TinNhanChat();
        tinNhan.setPhienChat(phien);
        tinNhan.setNguoiGui(nguoiGui);
        tinNhan.setLoai(loai);
        tinNhan.setNoiDung(noiDung);
        return tinNhanChatRepository.save(tinNhan);
    }

    /** Câu do server viết (chào, từ chối, cấp cứu) theo ngôn ngữ của request; lưu lại đúng câu đã trả. */
    private String cauCoDinh(String khoa) {
        return messageSource.getMessage(khoa, null, NgonNgu.hienTai().locale());
    }

    private static Long idTaiKhoanHienTai() {
        return NguoiDungHienTai.daDangNhap() ? NguoiDungHienTai.layIdTaiKhoan() : null;
    }

    /** Người bị đếm giới hạn: tài khoản khi đã đăng nhập, địa chỉ IP khi là khách. */
    private static String chuThe(Long idTaiKhoan, String diaChiIp) {
        return idTaiKhoan != null ? "tk:" + idTaiKhoan : "ip:" + diaChiIp;
    }

}
