"""Mục 15-18 của tài liệu API: lịch hẹn của tôi, lịch sử khám và đánh giá, hồ sơ bệnh nhân, trang cá nhân, thông báo.

Giai đoạn 4 của chuỗi dựng tài liệu; chạy qua build.py. JSON mẫu lấy từ response thật do lay_mau.py ghi lại
(temp/api-doc/mau), danh sách cắt còn 1-2 phần tử, ngày giờ chỉnh cho khớp với các mẫu của mục 11-14.
"""

import kit
from p3_dat_lich import (BENH_NHAN, BS_TOM_TAT, CHON_KHUNG, DA_DAT, LINK, LOI_CHON_KHUNG, LOI_HUY_DOI, LOI_PHAN_TRANG,
                         MA_PHIEU, NGUOI_THAN_CON, PHIEU, PHONG, PHU_LUC_A, TRANG, body, dan, js, loi, mh, ok, tham_so,
                         tra, trang)

MOI_VAI_TRO = "Mọi vai trò (cần access token)"
CUA_AI = ("cuaAi", "query", "enum",
          "Mặc định `TAT_CA`. `BAN_THAN`: chỉ lịch mà người khám là chủ tài khoản; `NGUOI_KHAC`: chỉ lịch của người "
          "khác (đặt cho người thân, hoặc chủ tài khoản là người giám hộ). Xem `PhamViLichHen` ở 3.7.")
LOI_MA = (404, "KHONG_TIM_THAY",
          "Mã phiếu khám sai dạng, không tồn tại, hoặc lịch không thuộc tài khoản đang gọi (cùng một thông điệp).")
LOI_GIAM_HO = (403, "KHONG_CO_QUYEN",
               "Tài khoản chỉ thấy lịch này vì là người giám hộ (theo CCCD) của lịch do người khác đặt.")

# ----- Dữ liệu mẫu (từ response thật) -----

MA_XN = "G67nJgoOQ6KYaehGltO1ILpUa0hkXbmoJCAokUlnx-A"
MA_ME = "nObpi25wW5BD5-h8Mu_qhr47zO_f1sYsSd7RvsJ7nHE"
MA_KHAM = "gtAXM48Y89TVdPRw35K4kEVkaiQFI0f8gl9Y0p7U09c"
MA_TC = "Qro6UgLbU9tkM-ofkCsl_7oMcio4ULmklNfE1g5fY9c"
MA_MOI = "FD658Ay8JZIKjIjnwOiPVCM42M2j2UT1uzpyPQkyOuM"
LY_DO_TU_CHOI = "Bác sĩ có lịch hội chẩn đột xuất, mời bạn đặt khung giờ khác"


def lich_hen(ma, ma_tra_cuu, trang_thai, so_thu_tu, gio_kham, ly_do_kham, ngay_dat, han, duoc_huy_doi, **khac):
    """1 LichHenCuaToiResponse; thứ tự trường như record Java."""
    ngay, gio = gio_kham[:10], int(gio_kham[11:13])
    mau = {"maPhieuKham": ma, "maTraCuu": ma_tra_cuu, "linkPhieuKham": LINK + ma, "trangThai": trang_thai,
           "soThuTu": so_thu_tu, "ngay": ngay, "gioKhamDuKien": gio_kham, "gioBatDauKhung": f"{ngay}T{gio:02d}:00:00",
           "gioKetThucKhung": f"{ngay}T{gio + 1:02d}:00:00", "bacSi": BS_TOM_TAT, "tenChuyenKhoa": "Da liễu",
           "phongKham": PHONG, "hoTenBenhNhan": "Nguyễn Thị Mai", "laBanThan": True, "hoTenNguoiGiamHo": None,
           "lyDoKham": ly_do_kham, "ngayDat": ngay_dat, "nguoiDat": "TOI", "thongTinKhacHoSo": False, "lyDoHuy": None,
           "duocHuyDoi": duoc_huy_doi, "hanHuyDoi": han, "canDoiLich": False}
    assert set(khac) <= set(mau)
    return {**mau, **khac}


LH_XAC_NHAN = lich_hen(MA_XN, "ECL-20261010-3552", "DA_XAC_NHAN", 2, "2026-10-10T08:10:00", "Khám định kỳ",
                       "2026-10-09T17:02:29.412706", "2026-10-10T06:10:00", True)
LH_ME = lich_hen(MA_ME, "ECL-20261010-0451", "CHO_XAC_NHAN", 7, "2026-10-10T09:00:00", "Đau khớp gối",
                 "2026-10-09T17:02:29.853613", "2026-10-10T07:00:00", True, hoTenBenhNhan="Lê Thị Hoa",
                 laBanThan=False)
LH_TU_CHOI = lich_hen(MA_TC, "ECL-20261010-6213", "BI_TU_CHOI", 14, "2026-10-10T10:10:00", "Tái khám",
                      "2026-10-09T17:05:12.63071", "2026-10-10T08:10:00", False, lyDoHuy=LY_DO_TU_CHOI)
LH_DA_KHAM = lich_hen(MA_KHAM, "ECL-20261002-7296", "DA_HOAN_THANH", 8, "2026-10-02T09:10:00",
                      "Ho, sốt nhẹ 2 ngày nay", "2026-10-01T20:15:42.132852", "2026-10-02T07:10:00", False)
KET_QUA = {"chanDoan": "Viêm họng cấp", "ghiChu": "Uống nhiều nước ấm, nghỉ ngơi; tái khám nếu sốt trên 38,5 độ.",
           "ngayTaiKhamDeXuat": "2026-10-09",
           "donThuoc": [{"tenThuoc": "Paracetamol 500mg", "donVi": "viên", "lieuDung": "1 viên / lần",
                         "soLanMoiNgay": 3, "soNgayDung": 5, "ghiChuSuDung": "Uống sau ăn, khi sốt hoặc đau"},
                        {"tenThuoc": "Nước muối sinh lý 0,9%", "donVi": "chai", "lieuDung": "Súc họng",
                         "soLanMoiNgay": 3, "soNgayDung": 5, "ghiChuSuDung": None}]}
GUI_DANH_GIA = {"soSao": 5, "nhanXet": "Bác sĩ tư vấn kỹ, dễ hiểu."}
DANH_GIA = {**GUI_DANH_GIA, "ngayTao": "2026-10-02T19:21:13.949954", "duocSuaDen": "2026-10-09T19:21:13.949954"}
SUA_DANH_GIA = {"soSao": 4, "nhanXet": "Tư vấn kỹ nhưng phải chờ hơi lâu."}
LAN_KHAM = {"lichHen": LH_DA_KHAM, "ketQua": KET_QUA}


def chi_tiet(lh, ket_qua=None, duoc_danh_gia=False, danh_gia=None):
    return {"lichHen": lh, "ngaySinhBenhNhan": "1995-03-12", "gioiTinhBenhNhan": "NU",
            "soDienThoaiLienHe": "0902000001", "emailLienHe": None, "ketQua": ket_qua, "duocDanhGia": duoc_danh_gia,
            "danhGia": danh_gia}


PHIEU_ME_DA_HUY = {
    **PHIEU, "maPhieuKham": MA_ME, "maTraCuu": "ECL-20261010-0451", "linkPhieuKham": LINK + MA_ME,
    "trangThai": "DA_HUY", "soThuTu": 7, "gioKhamDuKien": "2026-10-10T09:00:00",
    "gioBatDauKhung": "2026-10-10T09:00:00", "gioKetThucKhung": "2026-10-10T10:00:00", "lyDoKham": "Đau khớp gối",
    "ngayDat": "2026-10-09T17:02:29.853613",
    "benhNhan": {"hoTen": "Lê Thị Hoa", "namSinh": 1960, "gioiTinh": "NU", "cccd": "*********301",
                 "soDienThoai": "091****777"},
    "lyDoHuy": "Mẹ tôi đã khám ở nơi khác", "duocHuyDoi": False, "hanHuyDoi": "2026-10-10T07:00:00"}
DA_DOI_CUA_TOI = {
    **DA_DAT, "maPhieuKham": MA_MOI, "maTraCuu": "ECL-20261010-0957", "linkPhieuKham": LINK + MA_MOI, "soThuTu": 19,
    "gioKhamDuKien": "2026-10-10T11:00:00", "gioBatDauKhung": "2026-10-10T11:00:00",
    "gioKetThucKhung": "2026-10-10T11:30:00", "hoTenBenhNhan": "Nguyễn Thị Mai", "luuVaoTaiKhoan": True}

HO_SO = {"trangThaiLienKet": "DA_LIEN_KET", "cccd": "079000000001", "hoTen": "Nguyễn Thị Mai",
         "ngaySinh": "1995-03-12", "gioiTinh": "NU", "soDienThoai": "0902000001",
         "diaChi": "12 Nguyễn Văn Bảo, Phường 4, Gò Vấp, TP. Hồ Chí Minh", "soBaoHiemYTe": None, "tienSuBenhLy": None}
SUA_HO_SO = {"hoTen": "Nguyễn Thị Mai", "ngaySinh": "1995-03-12", "gioiTinh": "NU", "soDienThoai": "0902000001",
             "diaChi": "12 Nguyễn Văn Bảo, Phường 4, Gò Vấp, TP. Hồ Chí Minh", "soBaoHiemYTe": "DN4797900000001",
             "tienSuBenhLy": "Dị ứng penicillin"}
HO_SO_DA_SUA = {**HO_SO, "soBaoHiemYTe": "DN4797900000001", "tienSuBenhLy": "Dị ứng penicillin"}
TAO_HO_SO = {"hoTen": "Đỗ Văn Xuân", "ngaySinh": "1988-11-02", "gioiTinh": "NAM", "soDienThoai": "0933000001",
             "cccd": "079188000001", "diaChi": "25 Quang Trung, Gò Vấp, TP.HCM"}
HO_SO_CHO_XAC_MINH = {**{k: None for k in HO_SO}, "trangThaiLienKet": "CHO_XAC_MINH"}
TRANG_CA_NHAN = {
    "hoSo": HO_SO_DA_SUA, "emailTaiKhoan": "benhnhan01@eclinic.local", "nguoiThan": [NGUOI_THAN_CON],
    "lichSapToi": {"cuaToi": [LH_XAC_NHAN], "cuaNguoiKhac": []}, "lanKhamGanDay": [LAN_KHAM],
    "soLich": {"sapToiCuaToi": 1, "sapToiCuaNguoiKhac": 0, "lichSu": 4, "daKham": 1}}

TB_TU_CHOI = {"id": 12, "loai": "LICH_HEN_BI_TU_CHOI",
              "noiDung": "Bác sĩ Hoàng Minh Đức đã từ chối lịch khám của Nguyễn Thị Mai lúc 10:10 ngày 10/10/2026 "
                         f"(mã ECL-20261010-6213). Lý do: {LY_DO_TU_CHOI}",
              "daDoc": False, "ngayTao": "2026-10-09T17:21:17.027433", "idLichHen": 9, "maPhieuKham": MA_TC,
              "idYeuCau": None}
TB_XAC_NHAN = {"id": 11, "loai": "LICH_HEN_DA_XAC_NHAN",
               "noiDung": "Bác sĩ Hoàng Minh Đức đã xác nhận lịch khám của Nguyễn Thị Mai lúc 08:10 ngày 10/10/2026 "
                          "(mã ECL-20261010-3552).",
               "daDoc": False, "ngayTao": "2026-10-09T17:21:17.02414", "idLichHen": 5, "maPhieuKham": MA_XN,
               "idYeuCau": None}


def muc_15(so):
    lh = "/api/booking/lich-hen/cua-toi"
    return (kit.tieu_de(1, "15. Lịch hẹn của tôi") + kit.doan(
        "Các route của bệnh nhân đã đăng nhập cho màn hình \"Lịch hẹn của tôi\": danh sách, lịch theo tháng, chi tiết, "
        "hủy và đổi lịch. Mỗi lịch hẹn được gọi bằng `maPhieuKham` (3.10), không có id số. Vòng đời và quy tắc hủy / "
        "đổi ở 3.11.")
        + kit.route(
            "15.1", "Danh sách lịch hẹn của tôi", "GET", lh, "ep_lich_hen_cua_toi", next(so), BENH_NHAN,
            ["Lịch hẹn mà tài khoản nhìn thấy, có phân trang. Gồm 3 nhóm: lịch tài khoản đã đặt khi đăng nhập (cho bản "
             "thân hoặc người thân); lịch của hồ sơ bệnh nhân đã liên kết với tài khoản, kể cả lịch đặt như khách "
             "trước khi có tài khoản (3.9); lịch của người khác mà người giám hộ khai đúng số CCCD của chủ tài khoản.",
             "Gọi lại mỗi lần mở màn hình: trạng thái đổi khi bác sĩ xác nhận, từ chối hoặc ghi kết quả khám."],
            [tham_so([
                ("loc", "query", "enum",
                 "Mặc định `TAT_CA`. `SAP_TOI`: lịch `CHO_XAC_NHAN` / `DA_XAC_NHAN` mà khung giờ chưa kết thúc; "
                 "`LICH_SU`: mọi lịch còn lại (đã khám, đã hủy, bị từ chối, đã đổi, đã qua giờ). Xem `LocLichHen` ở "
                 "3.7."),
                CUA_AI] + TRANG),
             tra("200 OK", f"`duLieu`: trang (3.3) các {mh('LichHenCuaToiResponse')}.",
                 ok(trang([LH_TU_CHOI, LH_ME], 5))),
             loi([(400, "DU_LIEU_KHONG_HOP_LE",
                   "`loc` / `cuaAi` không phải giá trị enum (không có `chiTiet`); `trang` < 0 hoặc `kichThuoc` ngoài "
                   "1..100 (có `chiTiet`).")]),
             kit.luu_y([
                 "Thứ tự: `SAP_TOI` xếp giờ khám gần nhất trước; `TAT_CA` và `LICH_SU` xếp giờ khám muộn nhất trước.",
                 "`laBanThan` cho biết người khám có phải chủ tài khoản không; `nguoiDat` cho biết ai đã đặt (`TOI`, "
                 "`KHACH`: đặt khi không đăng nhập, `TAI_KHOAN_KHAC`). Lịch đặt như khách rồi được liên kết vào tài "
                 "khoản có `laBanThan = true`, `nguoiDat = KHACH`.",
                 "Hiện nút hủy / đổi theo `duocHuyDoi`. Lịch mà tài khoản chỉ là người giám hộ theo CCCD luôn có "
                 "`duocHuyDoi = false`.",
                 "`lyDoHuy` có giá trị ở lịch `BI_TU_CHOI` (lý do của bác sĩ) và lịch `DA_HUY` có nhập lý do.",
                 "Tài khoản chưa có lịch nào: `noiDung` rỗng, `tongSoPhanTu = 0`, `tongSoTrang = 0`.",
                 "Hồ sơ của tài khoản đang `CHO_XAC_MINH` thì lịch của hồ sơ đó chưa hiện ở đây (3.9).",
             ])])
        + kit.route(
            "15.2", "Lịch hẹn của tôi theo khoảng ngày", "GET", f"{lh}/lich", "ep_lich_hen_cua_toi_lich", next(so),
            BENH_NHAN,
            "Mọi lịch hẹn (mọi trạng thái) của tài khoản có giờ khám trong khoảng ngày, xếp theo giờ khám tăng dần, "
            "không phân trang. Dùng cho màn hình lịch dạng tháng / tuần.",
            [tham_so([("tuNgay", "query", "date", "Bắt buộc. Ngày đầu của khoảng, dạng `yyyy-MM-dd`."),
                      ("denNgay", "query", "date",
                       "Bắt buộc. Ngày cuối của khoảng (tính cả ngày này), không trước `tuNgay`; khoảng dài tối đa 42 "
                       "ngày (6 tuần của 1 trang lịch tháng)."),
                      CUA_AI]),
             tra("200 OK", f"`duLieu`: mảng {mh('LichHenCuaToiResponse')}; mảng rỗng nếu khoảng ngày không có lịch.",
                 ok([LH_XAC_NHAN, LH_ME])),
             loi([(400, "DU_LIEU_KHONG_HOP_LE",
                   "Thiếu `tuNgay` / `denNgay`, ngày sai định dạng, `denNgay` trước `tuNgay`, hoặc khoảng dài hơn 42 "
                   "ngày (không có `chiTiet`; đọc `thongDiep`).")]),
             kit.luu_y([
                 "Cùng tập lịch và cùng bộ lọc `cuaAi` với 15.1; khác ở chỗ lấy theo ngày khám và trả hết trong 1 lần.",
                 "Khoảng dài nhất là `denNgay = tuNgay + 41 ngày`.",
             ])])
        + kit.route(
            "15.3", "Chi tiết lịch hẹn của tôi", "GET", f"{lh}/{{maPhieuKham}}", "ep_lich_hen_cua_toi_chi_tiet",
            next(so), BENH_NHAN,
            "Chi tiết 1 lịch hẹn trong danh sách của tài khoản: các trường của dòng danh sách, thông tin người khám, "
            "và khi đã khám xong thì kèm kết quả khám và đánh giá.",
            [tham_so([MA_PHIEU]),
             tra("200 OK", f"`duLieu`: {mh('LichHenChiTietCuaToiResponse')}. Lịch chưa khám:",
                 ok(chi_tiet(LH_XAC_NHAN))),
             dan(f"Lịch đã khám xong và đã đánh giá (`ketQua`: {mh('KetQuaKhamResponse')}, `danhGia`: "
                 f"{mh('DanhGiaCuaToiResponse')}):"),
             js(ok(chi_tiet(LH_DA_KHAM, KET_QUA, False, DANH_GIA))),
             loi([LOI_MA]),
             kit.luu_y([
                 "`ketQua` chỉ có khi lịch `DA_HOAN_THANH` và tài khoản là người khám (`lichHen.laBanThan`) hoặc "
                 "người đã đặt (`lichHen.nguoiDat = TOI`). Tài khoản chỉ là người giám hộ theo CCCD không xem được "
                 "kết quả.",
                 "`duocDanhGia = true`: hiện nút \"Đánh giá\" (16.2). Đã đánh giá thì `duocDanhGia = false` và "
                 "`danhGia` có giá trị; còn sửa được (16.3) tới `danhGia.duocSuaDen`.",
                 "`soDienThoaiLienHe`, `emailLienHe` chỉ có khi chính tài khoản này đặt lịch; lịch người khác đặt thì "
                 "là `null`.",
                 "`donThuoc` là mảng rỗng khi bác sĩ không kê đơn; `ngayTaiKhamDeXuat` chỉ là gợi ý, không phải lịch "
                 "hẹn.",
                 "Trang phiếu khám công khai (14.1) và mã QR (14.2) dùng cùng `maPhieuKham` này.",
             ])])
        + kit.route(
            "15.4", "Hủy lịch hẹn của tôi", "POST", f"{lh}/{{maPhieuKham}}/huy", "ep_lich_hen_cua_toi_huy", next(so),
            BENH_NHAN,
            "Hủy 1 lịch hẹn mà tài khoản đã đặt hoặc là người khám, trước hạn hủy / đổi (3.11). Không cần số điện "
            "thoại như route công khai 14.3.",
            [tham_so([MA_PHIEU]),
             body([("lyDo", "string", "Không", "Lý do hủy, tối đa 500 ký tự. Bác sĩ và quản trị viên đọc được.")]),
             dan("Body không bắt buộc: có thể gọi không kèm body."),
             js({"lyDo": "Mẹ tôi đã khám ở nơi khác"}),
             tra("200 OK", f"`duLieu`: {mh('PhieuKhamResponse')} sau khi hủy (`trangThai = DA_HUY`; thông tin người "
                 "khám đã che như phiếu khám công khai).", ok(PHIEU_ME_DA_HUY, "Đã hủy lịch hẹn")),
             loi([(400, "DU_LIEU_KHONG_HOP_LE", "`lyDo` dài hơn 500 ký tự (có `chiTiet`)."), LOI_GIAM_HO, LOI_MA]
                 + LOI_HUY_DOI),
             kit.luu_y([
                 "Chỗ của lịch đã hủy được trả lại cho khung giờ ngay; không hoàn tác được.",
                 "Bác sĩ của lịch nhận thông báo `LICH_HEN_DA_HUY`; tài khoản bệnh nhân không nhận thông báo cho thao "
                 "tác của chính mình.",
                 "Route này không tính vào giới hạn 20 lần / 10 phút / IP.",
                 "Sau khi hủy, tải lại danh sách (15.1): lịch chuyển sang bộ lọc `LICH_SU`.",
             ])])
        + kit.route(
            "15.5", "Đổi lịch hẹn của tôi", "POST", f"{lh}/{{maPhieuKham}}/doi-lich", "ep_lich_hen_cua_toi_doi_lich",
            next(so), BENH_NHAN,
            "Đổi 1 lịch hẹn sang khung giờ khác (cùng hoặc khác bác sĩ, chuyên khoa). Lịch cũ thành "
            "`DA_HUY_DO_DOI_LICH`, response là lịch mới với **mã phiếu khám mới** (3.11). Thông tin người khám, người "
            "giám hộ và lý do khám giữ nguyên từ lịch cũ.",
            [tham_so([MA_PHIEU]),
             body(CHON_KHUNG),
             js({"idLichLamViec": 15, "gioBatDauKhung": "2026-10-10T11:00:00"}),
             tra("201 Created", f"`duLieu`: {mh('DatLichResponse')} của lịch mới (`CHO_XAC_NHAN`).",
                 ok(DA_DOI_CUA_TOI, "Đổi lịch thành công")),
             loi([(400, "DU_LIEU_KHONG_HOP_LE",
                   "Thiếu `gioBatDauKhung` (có `chiTiet`). Không có `chiTiet`: gửi cả hai hoặc không gửi cái nào trong "
                   "`idLichLamViec`, `idChuyenKhoa`; `gioBatDauKhung` không phải khung của ca; chọn lại đúng khung "
                   "giờ đang giữ."),
                  LOI_GIAM_HO,
                  (404, "KHONG_TIM_THAY", "Như 15.4; hoặc `idLichLamViec` không tồn tại.")]
                 + LOI_HUY_DOI
                 + [(409, "VUOT_SO_LAN_DOI_LICH", "Lịch này đã được đổi 2 lần; vẫn hủy được.")]
                 + LOI_CHON_KHUNG),
             kit.luu_y([
                 "Đổi không thành công (mọi mã lỗi ở trên) thì lịch cũ giữ nguyên, không mất chỗ.",
                 "Lịch đã được bác sĩ xác nhận mà đổi thì lịch mới quay về `CHO_XAC_NHAN`, chờ xác nhận lại.",
                 "Sau khi đổi, dùng `maPhieuKham` mới cho mọi lần gọi tiếp theo; lịch cũ vẫn có trong danh sách với "
                 "trạng thái `DA_HUY_DO_DOI_LICH`.",
                 "Mỗi lần gọi qua được bước kiểm tra body đều tính vào giới hạn 20 lần / 10 phút / IP (3.6).",
                 "Không gửi `soDienThoai` (trường này chỉ dùng cho 14.4); có gửi cũng bị bỏ qua.",
             ])]))


def muc_16(so):
    dg = "/api/booking/lich-hen/cua-toi/{maPhieuKham}/danh-gia"
    than_danh_gia = body([("soSao", "number", "Có", "Số nguyên từ 1 đến 5."),
                          ("nhanXet", "string", "Không", "Tối đa 1000 ký tự. Bỏ trống hoặc chỉ có khoảng trắng thì "
                           "lưu là `null`.")])
    return (kit.tieu_de(1, "16. Lịch sử khám và đánh giá") + kit.doan(
        "Các lượt đã khám xong của bệnh nhân kèm kết quả do bác sĩ ghi (chẩn đoán, lời dặn, đơn thuốc), và việc gửi / "
        "sửa đánh giá cho 1 lượt khám.")
        + kit.route(
            "16.1", "Lịch sử khám của tôi", "GET", "/api/booking/lich-su-kham/cua-toi", "ep_lich_su_kham", next(so),
            BENH_NHAN,
            "Các lịch hẹn `DA_HOAN_THANH` mà tài khoản được xem kết quả: tài khoản là người khám, hoặc tài khoản đã "
            "đặt lịch (cho người thân). Xếp giờ khám muộn nhất trước, có phân trang.",
            [tham_so([CUA_AI] + TRANG),
             tra("200 OK", f"`duLieu`: trang (3.3) các {mh('LanKhamCuaToiResponse')}.", ok(trang([LAN_KHAM], 1))),
             loi([LOI_PHAN_TRANG]),
             kit.luu_y([
                 "Mỗi phần tử gồm `lichHen` (như 1 dòng của 15.1) và `ketQua` (như ở 15.3). Mở chi tiết và đánh giá "
                 "bằng `lichHen.maPhieuKham` (15.3).",
                 "Lịch mà tài khoản chỉ là người giám hộ theo CCCD không có trong lịch sử khám.",
                 "Response không có thông tin đã đánh giá hay chưa; lấy `duocDanhGia` / `danhGia` từ 15.3.",
             ])])
        + kit.route(
            "16.2", "Gửi đánh giá lượt khám", "POST", dg, "ep_danh_gia_gui", next(so), BENH_NHAN,
            "Đánh giá bác sĩ sau 1 lượt khám đã hoàn thành. Mỗi lịch hẹn chỉ có 1 đánh giá; người đánh giá là người "
            "khám hoặc người đã đặt lịch.",
            [tham_so([MA_PHIEU]), than_danh_gia, js(GUI_DANH_GIA),
             tra("201 Created", f"`duLieu`: {mh('DanhGiaCuaToiResponse')}.", ok(DANH_GIA, "Đã gửi đánh giá")),
             loi([(400, "DU_LIEU_KHONG_HOP_LE", "Thiếu `soSao`, `soSao` ngoài 1..5, `nhanXet` quá dài (có `chiTiet`)."),
                  (403, "KHONG_CO_QUYEN", LOI_GIAM_HO[2]), LOI_MA,
                  (409, "LICH_HEN_CHUA_KHAM_XONG", "Lịch chưa ở trạng thái `DA_HOAN_THANH`."),
                  (409, "DA_DANH_GIA", "Lịch này đã có đánh giá; dùng 16.3 để sửa.")]),
             kit.luu_y([
                 "Chỉ hiện nút đánh giá khi `duocDanhGia = true` ở 15.3.",
                 "Điểm trung bình và số đánh giá của bác sĩ (`diemDanhGia`, `soDanhGia` ở 11.3, 11.4) tính từ các đánh "
                 "giá này.",
             ])])
        + kit.route(
            "16.3", "Sửa đánh giá đã gửi", "PUT", dg, "ep_danh_gia_sua", next(so), BENH_NHAN,
            "Sửa số sao và nhận xét của đánh giá đã gửi, trong 7 ngày kể từ lúc gửi (tới `duocSuaDen`). Gửi đủ cả hai "
            "trường: `nhanXet` bỏ trống là xoá nhận xét cũ.",
            [tham_so([MA_PHIEU]), than_danh_gia, js(SUA_DANH_GIA),
             tra("200 OK", f"`duLieu`: {mh('DanhGiaCuaToiResponse')} sau khi sửa; `ngayTao` và `duocSuaDen` không đổi.",
                 ok({**DANH_GIA, **SUA_DANH_GIA}, "Đã cập nhật đánh giá")),
             loi([(400, "DU_LIEU_KHONG_HOP_LE", "Như 16.2."), (403, "KHONG_CO_QUYEN", LOI_GIAM_HO[2]),
                  (404, "KHONG_TIM_THAY",
                   "Như 15.3; hoặc lịch hẹn này chưa có đánh giá (`thongDiep`: \"Lịch hẹn này chưa có đánh giá\")."),
                  (409, "HET_HAN_SUA_DANH_GIA", "Đã quá `duocSuaDen`.")]),
             kit.luu_y(["Không có route xoá đánh giá."])]))


def muc_17(so):
    hs = "/api/booking/ho-so-benh-nhan/cua-toi"
    return (kit.tieu_de(1, "17. Hồ sơ bệnh nhân và trang cá nhân") + kit.doan(
        "Hồ sơ bệnh nhân là thông tin khám bệnh của chủ tài khoản (CCCD, ngày sinh, bảo hiểm y tế, tiền sử bệnh), "
        "tách khỏi hồ sơ tài khoản ở mục 6 (họ tên, số điện thoại, ảnh đại diện). Mỗi tài khoản bệnh nhân có nhiều "
        "nhất 1 hồ sơ; số CCCD là khóa của hồ sơ. Cách hồ sơ được gắn vào tài khoản ở 3.9.")
        + kit.route(
            "17.1", "Xem hồ sơ bệnh nhân của tôi", "GET", hs, "ep_ho_so_benh_nhan", next(so), BENH_NHAN,
            "Hồ sơ bệnh nhân của tài khoản đang đăng nhập; CCCD và số điện thoại không che.",
            [tra("200 OK", f"`duLieu`: {mh('HoSoCuaToiResponse')}.", ok(HO_SO)),
             loi([(404, "KHONG_TIM_THAY",
                   "Tài khoản chưa có hồ sơ bệnh nhân (`thongDiep`: \"Tài khoản chưa có hồ sơ bệnh nhân\").")]),
             kit.luu_y([
                 "404 ở đây là trạng thái bình thường của tài khoản mới: hiện form tạo hồ sơ (17.2), không coi là lỗi.",
                 "`trangThaiLienKet = CHO_XAC_MINH`: mọi trường khác là `null`; hiện \"hồ sơ đang chờ phòng khám xác "
                 "minh\" và không cho sửa.",
                 "Cùng dữ liệu này có trong `hoSoBenhNhan` của `GET /api/users/me` (6.1), `banThan` của 13.2 và "
                 "`hoSo` của 17.3.",
             ])])
        + kit.route(
            "17.2", "Tạo hoặc sửa hồ sơ bệnh nhân của tôi", "PUT", hs, "ep_ho_so_benh_nhan_luu", next(so), BENH_NHAN,
            ["Tài khoản chưa có hồ sơ: tạo hồ sơ (bắt buộc có `cccd`). Đã có hồ sơ: sửa thông tin. Gửi đủ mọi trường "
             "(ghi đè toàn bộ): trường không bắt buộc bỏ trống là xoá trắng trường đó.",
             "Khi tạo mà số CCCD đã có hồ sơ từ lần đặt lịch như khách, hệ thống thử gắn hồ sơ đó vào tài khoản (xem "
             "LƯU Ý)."],
            [body([
                ("hoTen", "string", "Có", "Không rỗng, tối đa 150 ký tự."),
                ("ngaySinh", "date", "Có", "Không ở tương lai."),
                ("gioiTinh", "enum", "Không", "`NAM`, `NU`, `KHAC`."),
                ("soDienThoai", "string", "Có", "10 chữ số, bắt đầu bằng 0."),
                ("cccd", "string", "Khi tạo",
                 "Đúng 12 chữ số. Bắt buộc khi tài khoản chưa có hồ sơ. Đã có hồ sơ: bỏ trống hoặc gửi đúng số đang "
                 "lưu, vì số CCCD không đổi được."),
                ("diaChi", "string", "Không", "Tối đa 500 ký tự."),
                ("soBaoHiemYTe", "string", "Không", "Tối đa 50 ký tự."),
                ("tienSuBenhLy", "string", "Không", "Tiền sử bệnh, dị ứng; tối đa 5000 ký tự."),
            ]),
             dan("Tạo hồ sơ lần đầu:"), js(TAO_HO_SO),
             dan("Sửa hồ sơ đã có:"), js(SUA_HO_SO),
             tra("200 OK", f"`duLieu`: {mh('HoSoCuaToiResponse')} sau khi lưu.",
                 ok(HO_SO_DA_SUA, "Đã lưu hồ sơ bệnh nhân")),
             dan("Số CCCD đã có hồ sơ nhưng thông tin không khớp: vẫn trả 200, hồ sơ chờ xác minh và chưa có gì được "
                 "ghi:"),
             js(ok(HO_SO_CHO_XAC_MINH, "Đã lưu hồ sơ bệnh nhân")),
             loi([(400, "DU_LIEU_KHONG_HOP_LE",
                   "Body sai quy tắc (có `chiTiet`). Không có `chiTiet`: tạo hồ sơ mà thiếu `cccd`; gửi `cccd` khác "
                   "số đang lưu."),
                  (409, "CCCD_DA_CO_HO_SO",
                   "Số CCCD thuộc hồ sơ đã gắn với tài khoản khác, hoặc quản trị viên đã từ chối gắn hồ sơ này vào "
                   "tài khoản đang gọi. Hướng dẫn bệnh nhân liên hệ phòng khám."),
                  (409, "HO_SO_CHO_XAC_MINH", "Hồ sơ của tài khoản đang `CHO_XAC_MINH`: chưa sửa được."),
                  (409, "XUNG_DOT_DU_LIEU", "Hai request tạo hồ sơ cùng lúc (3.4): tải lại 17.1.")]),
             kit.luu_y([
                 "Tạo với số CCCD chưa có hồ sơ: hồ sơ mới, `trangThaiLienKet = DA_LIEN_KET`.",
                 "Tạo với số CCCD đã có hồ sơ (chưa gắn tài khoản nào): **khớp** khi họ tên giống hồ sơ (không xét "
                 "hoa thường, khoảng trắng thừa) và ngày sinh **hoặc** số điện thoại trùng hồ sơ. Khớp thì hồ sơ gắn "
                 "ngay vào tài khoản (`DA_LIEN_KET`), thông tin trong form ghi đè lên hồ sơ, và các lịch đã đặt như "
                 "khách bằng số CCCD đó hiện trong 15.1 với `nguoiDat = KHACH`.",
                 "Không khớp: response 200 có `trangThaiLienKet = CHO_XAC_MINH` và mọi trường khác `null`. Phải kiểm "
                 "tra `trangThaiLienKet` trong response, không chỉ dựa vào HTTP 200. Quản trị viên duyệt thì hồ sơ "
                 "thành `DA_LIEN_KET`; từ chối thì tài khoản trở lại chưa có hồ sơ.",
                 "Sửa hồ sơ không thay đổi các lịch hẹn đã đặt và không đổi `hoTen`, `soDienThoai` của tài khoản "
                 "(mục 6).",
                 "Hồ sơ cũng được tạo tự động khi đặt lịch \"cho bản thân\" lần đầu (13.1), nên form này không phải "
                 "bước bắt buộc trước khi đặt lịch.",
             ])])
        + kit.route(
            "17.3", "Trang cá nhân của bệnh nhân", "GET", "/api/booking/trang-ca-nhan/cua-toi", "ep_trang_ca_nhan",
            next(so), BENH_NHAN,
            "Mọi thứ trang cá nhân của bệnh nhân cần, trong 1 lần gọi: hồ sơ bệnh nhân, người thân đã lưu, các lịch "
            "sắp tới, các lượt khám gần đây và số đếm từng loại.",
            [tra("200 OK", f"`duLieu`: {mh('TrangCaNhanResponse')}.", ok(TRANG_CA_NHAN)),
             kit.luu_y([
                 "`lichSapToi.cuaToi`, `lichSapToi.cuaNguoiKhac` và `lanKhamGanDay` mỗi mảng có tối đa 5 phần tử. So "
                 "với `soLich` để biết còn nữa không và dẫn sang 15.1 (`loc=SAP_TOI`, `cuaAi=...`) hoặc 16.1.",
                 "`soLich.lichSu` là tổng của bộ lọc `LICH_SU` ở 15.1; `soLich.daKham` là tổng của 16.1.",
                 "Tài khoản chưa có hồ sơ: `hoSo = null`, các mảng rỗng, các số đếm bằng 0. Hồ sơ `CHO_XAC_MINH`: "
                 "`hoSo` chỉ có `trangThaiLienKet`.",
                 "`nguoiThan` như ở 13.2; sửa / bỏ bằng 13.3, 13.4.",
                 "Tên, số điện thoại và ảnh đại diện của tài khoản không có ở đây: lấy từ `GET /api/users/me` (6.1).",
             ])]))


def muc_18(so):
    tb = "/api/notification/thong-bao"
    return (kit.tieu_de(1, "18. Thông báo") + kit.doan(
        "Chuông thông báo trong ứng dụng của tài khoản đang đăng nhập. Dùng chung cho mọi vai trò; tài khoản nào "
        "cũng chỉ thấy thông báo của mình. Khách không đăng nhập không có thông báo.")
        + kit.doan(
            "Bệnh nhân nhận 4 loại (`LoaiThongBao` ở 3.7): `LICH_HEN_DA_XAC_NHAN` và `LICH_HEN_BI_TU_CHOI` khi bác sĩ "
            "xác nhận / từ chối; `LICH_HEN_CAN_DOI` khi ca khám bị hủy và lịch cần đổi (3.11); `LICH_HEN_DOI_PHONG` "
            "khi ca khám đổi phòng. Người nhận là tài khoản đã đặt lịch và tài khoản của hồ sơ người khám. Bệnh nhân "
            "không nhận thông báo khi tự đặt, tự hủy, tự đổi lịch hoặc khi bác sĩ ghi kết quả khám.")
        + kit.route(
            "18.1", "Danh sách thông báo của tôi", "GET", tb, "ep_thong_bao", next(so), MOI_VAI_TRO,
            "Thông báo của tài khoản, mới nhất trước, có phân trang.",
            [tham_so([("chuaDoc", "query", "boolean", "Mặc định `false`. `true`: chỉ lấy thông báo chưa đọc.")]
                     + TRANG),
             tra("200 OK", f"`duLieu`: trang (3.3) các {mh('ThongBaoResponse')}.",
                 ok(trang([TB_TU_CHOI, TB_XAC_NHAN], 2))),
             loi([LOI_PHAN_TRANG]),
             kit.luu_y([
                 "`noiDung` là câu tiếng Việt đã soạn sẵn: hiển thị nguyên văn, không tự ghép lại từ `loai`.",
                 "Bấm vào thông báo của bệnh nhân: mở chi tiết lịch hẹn bằng `maPhieuKham` (15.3), rồi gọi 18.3. "
                 "`maPhieuKham` chỉ có khi người gọi là bệnh nhân; với bác sĩ và quản trị viên là `null` (dùng "
                 "`idLichHen`, `idYeuCau`).",
                 "Thông báo xuất hiện trễ tối đa khoảng 5 giây sau sự kiện.",
             ])])
        + kit.route(
            "18.2", "Số thông báo chưa đọc", "GET", f"{tb}/so-chua-doc", "ep_thong_bao_so_chua_doc", next(so),
            MOI_VAI_TRO, "Số hiện trên biểu tượng chuông.",
            [tra("200 OK", f"`duLieu`: {mh('SoChuaDocResponse')}.", ok({"soChuaDoc": 2})),
             kit.luu_y([
                 "Không có đẩy realtime (WebSocket / SSE): gọi lại route này định kỳ, khoảng 30 giây một lần, và tạm "
                 "dừng khi tab trình duyệt bị ẩn.",
                 "Khi số tăng so với lần trước thì tải lại 18.1 nếu danh sách đang mở.",
             ])])
        + kit.route(
            "18.3", "Đánh dấu 1 thông báo đã đọc", "POST", f"{tb}/{{id}}/da-doc", "ep_thong_bao_da_doc", next(so),
            MOI_VAI_TRO, "Đánh dấu 1 thông báo của tài khoản là đã đọc. Không có body.",
            [tham_so([("id", "path", "number", "`id` của thông báo (18.1).")]),
             tra("200 OK", f"`duLieu`: {mh('ThongBaoResponse')} với `daDoc = true`.",
                 ok({**TB_TU_CHOI, "daDoc": True})),
             loi([(404, "KHONG_TIM_THAY_THONG_BAO",
                   "Không có thông báo với id này, hoặc thông báo của tài khoản khác (cùng một lỗi).")]),
             kit.luu_y(["Gọi lại trên thông báo đã đọc vẫn trả 200.",
                        "Không có route đánh dấu chưa đọc và không có route xoá thông báo."])])
        + kit.route(
            "18.4", "Đánh dấu tất cả đã đọc", "POST", f"{tb}/da-doc-tat-ca", "ep_thong_bao_da_doc_tat_ca", next(so),
            MOI_VAI_TRO, "Đánh dấu mọi thông báo chưa đọc của tài khoản là đã đọc. Không có body.",
            [tra("200 OK", f"`duLieu`: {mh('DanhDauDaDocResponse')}: số thông báo vừa chuyển sang đã đọc (0 nếu "
                 "không còn thông báo chưa đọc).", ok({"soDaDanhDau": 2}))]))


ROUTE = [
    ("15.1", "GET", "/api/booking/lich-hen/cua-toi", "ep_lich_hen_cua_toi", "Bệnh nhân", "Danh sách lịch hẹn của tôi"),
    ("15.2", "GET", "/api/booking/lich-hen/cua-toi/lich", "ep_lich_hen_cua_toi_lich", "Bệnh nhân",
     "Lịch hẹn của tôi theo khoảng ngày"),
    ("15.3", "GET", "/api/booking/lich-hen/cua-toi/{maPhieuKham}", "ep_lich_hen_cua_toi_chi_tiet", "Bệnh nhân",
     "Chi tiết lịch hẹn của tôi"),
    ("15.4", "POST", "/api/booking/lich-hen/cua-toi/{maPhieuKham}/huy", "ep_lich_hen_cua_toi_huy", "Bệnh nhân",
     "Hủy lịch hẹn của tôi"),
    ("15.5", "POST", "/api/booking/lich-hen/cua-toi/{maPhieuKham}/doi-lich", "ep_lich_hen_cua_toi_doi_lich",
     "Bệnh nhân", "Đổi lịch hẹn của tôi"),
    ("16.1", "GET", "/api/booking/lich-su-kham/cua-toi", "ep_lich_su_kham", "Bệnh nhân", "Lịch sử khám của tôi"),
    ("16.2", "POST", "/api/booking/lich-hen/cua-toi/{maPhieuKham}/danh-gia", "ep_danh_gia_gui", "Bệnh nhân",
     "Gửi đánh giá lượt khám"),
    ("16.3", "PUT", "/api/booking/lich-hen/cua-toi/{maPhieuKham}/danh-gia", "ep_danh_gia_sua", "Bệnh nhân",
     "Sửa đánh giá đã gửi"),
    ("17.1", "GET", "/api/booking/ho-so-benh-nhan/cua-toi", "ep_ho_so_benh_nhan", "Bệnh nhân",
     "Xem hồ sơ bệnh nhân của tôi"),
    ("17.2", "PUT", "/api/booking/ho-so-benh-nhan/cua-toi", "ep_ho_so_benh_nhan_luu", "Bệnh nhân",
     "Tạo hoặc sửa hồ sơ bệnh nhân của tôi"),
    ("17.3", "GET", "/api/booking/trang-ca-nhan/cua-toi", "ep_trang_ca_nhan", "Bệnh nhân",
     "Trang cá nhân của bệnh nhân"),
    ("18.1", "GET", "/api/notification/thong-bao", "ep_thong_bao", "Đã đăng nhập", "Danh sách thông báo của tôi"),
    ("18.2", "GET", "/api/notification/thong-bao/so-chua-doc", "ep_thong_bao_so_chua_doc", "Đã đăng nhập",
     "Số thông báo chưa đọc"),
    ("18.3", "POST", "/api/notification/thong-bao/{id}/da-doc", "ep_thong_bao_da_doc", "Đã đăng nhập",
     "Đánh dấu 1 thông báo đã đọc"),
    ("18.4", "POST", "/api/notification/thong-bao/da-doc-tat-ca", "ep_thong_bao_da_doc_tat_ca", "Đã đăng nhập",
     "Đánh dấu tất cả đã đọc"),
]


def sua(tl):
    # 6.1: hồ sơ bệnh nhân không còn "chỉ xem", bệnh nhân tự tạo / sửa bằng mục 17
    tl.thay_chu(" (chỉ xem), quản trị viên không có hồ sơ riêng.",
                " (xem và sửa bằng các route ở mục 17), quản trị viên không có hồ sơ riêng.")
    for dong in ROUTE:
        tl.them_dong("2. Danh sách route", None, kit.dong_route(*dong))
    # Id bookmark tạm; build.py đánh số lại toàn bộ ở cuối chuỗi
    so = iter(range(tl.id_bookmark_moi(), 10 ** 6))
    tl.chen_truoc_doan(PHU_LUC_A, muc_15(so) + muc_16(so) + muc_17(so) + muc_18(so))
