"""Tài liệu 2, giai đoạn 2: mục 4, mô hình response chưa có ở TK. Mô hình dùng chung (BacSiTomTatResponse,
PhongKhamTomTatResponse, KetQuaKhamResponse) và record lồng giống hệt record đã có ở TK (SinhHieu, DonThuoc của
BenhAnResponse) chỉ dẫn mục TK; bsqt_check.py so trường của chúng với record của TK. Chạy qua bsqt_build.py.
"""

import kit
from bsqt_p2_khung import them_cuoi

RONG_MO_HINH = (2150, 3100, 4388)  # như mô hình 4.10 trở đi của TK


def bang_mo_hinh(dong):
    return kit.bang(RONG_MO_HINH, ("Trường", "Kiểu", "Mô tả"), [(f"`{t}`", k, m) for t, k, m in dong]) \
        + kit.khoang_trong(100)


def mo_hinh(so, ten, mo_ta, dong, con=(), ghi_chu=()):
    """1 mục 4.n. con: các record lồng (tên, câu giới thiệu, dòng), mỗi record 1 bảng riêng dưới cùng heading.
    ghi_chu: các đoạn đặt ngay sau bảng chính (không mở đầu bằng "Tên — " để bsqt_check không coi là record lồng)."""
    x = kit.tieu_de(2, f"4.{so} {ten}") + (kit.doan(mo_ta) if mo_ta else "") + bang_mo_hinh(dong)
    x += "".join(kit.doan(d) for d in ghi_chu)
    for ten_con, gioi_thieu, dong_con in con:
        x += kit.doan(f"`{ten_con}` — {gioi_thieu}") + bang_mo_hinh(dong_con)
    return x


# (tên, mô tả, dòng, record lồng, ghi chú). Thứ tự ở đây là thứ tự đánh số 4.1, 4.2...
MO_HINH = [
    ("CaLamViecResponse", "1 ca làm việc của bác sĩ. Danh sách ca theo khoảng ngày có cả ca đã hủy.", [
        ("idLichLamViec", "number", "Id ca; dùng cho tham số `idLichLamViec`."),
        ("ngay", "date", ""),
        ("gioBatDau", "time", ""),
        ("gioKetThuc", "time", ""),
        ("soLuotToiDaMoiGio", "number", "Số lượt khám tối đa của 1 khung 1 giờ."),
        ("thoiLuongLuotPhut", "number", "Số phút của 1 lượt khám."),
        ("trangThai", "enum TrangThaiLichLamViec", "`DA_HUY`: ca đã hủy (3.9)."),
        ("bacSi", "BacSiTomTatResponse", "TK 4.14."),
        ("tenChuyenKhoa", "string", "Theo `Accept-Language` (TK 3.12)."),
        ("phongKham", "PhongKhamTomTatResponse", "TK 4.15."),
        ("tongSoLuot", "number", "Số lượt khám của ca, không tính lượt đã hủy."),
        ("soLuotDaDat", "number", "Số lượt đã có người đặt."),
        ("soLuotConTrong", "number", "Số lượt còn trống, kể cả lượt đã qua giờ."),
    ], (), ()),
    ("LichHenTrongCaResponse",
     "1 lịch hẹn trong danh sách lịch hẹn của bác sĩ, trong hàng đợi và trong kết quả tra cứu theo mã. Chỉ trả cho bác "
     "sĩ của lịch hẹn và quản trị viên: số điện thoại không che (khác phiếu khám công khai, TK 3.10). Không có mã phiếu "
     "khám, không có CCCD.", [
         ("id", "number", "Id lịch hẹn, dùng trên đường dẫn các route của tài liệu này (3.3)."),
         ("maTraCuu", "string", "Mã in trên phiếu khám, vd `ECL-20261005-4198`."),
         ("idLichLamViec", "number", "Ca làm việc của lịch hẹn."),
         ("trangThai", "enum TrangThaiLichHen", "TK 3.11; bác sĩ làm được gì ở từng trạng thái: 3.4."),
         ("soThuTu", "number", "Số thứ tự khám theo phòng khám và ngày."),
         ("gioKhamDuKien", "datetime", "Giờ bắt đầu lượt khám được xếp."),
         ("gioBatDauKhung", "datetime", "Khung 1 giờ người đặt đã chọn."),
         ("gioKetThucKhung", "datetime", ""),
         ("bacSi", "BacSiTomTatResponse", "TK 4.14. `null` ở các route của bác sĩ (lịch hẹn luôn của chính bác sĩ); "
                                          "có ở route của quản trị viên."),
         ("phongKham", "PhongKhamTomTatResponse", "TK 4.15."),
         ("lyDoKham", "string", "Lý do khám người đặt ghi."),
         ("ngayDat", "datetime", "Lúc đặt lịch."),
         ("idHoSoBenhNhan", "number", "Hồ sơ bệnh nhân của lịch hẹn."),
         ("benhNhan", "BenhNhan", "Thông tin đang lưu trong hồ sơ bệnh nhân."),
         ("nguoiGiamHo", "NguoiGiamHo", "`null` nếu lượt khám không có người giám hộ."),
         ("doiChieu", "DoiChieu", "Những gì người đặt đã nhập, để đối chiếu với hồ sơ (3.6)."),
         ("lyDoHuy", "string", "Lý do bác sĩ hủy (`BI_TU_CHOI`) hoặc lý do bệnh nhân hủy (`DA_HUY`); `null` ở các "
                               "trạng thái khác."),
         ("canDoiLich", "boolean", "`true`: ca khám đã bị hủy, lịch hẹn còn hiệu lực và chờ bệnh nhân đổi sang khung "
                                   "giờ khác (TK 3.11)."),
     ], (
         ("BenhNhan", "giá trị của `benhNhan`.", [
             ("hoTen", "string", ""),
             ("ngaySinh", "date", "`null` nếu hồ sơ chưa có ngày sinh."),
             ("tuoi", "number", "Số tuổi tròn vào ngày khám; `null` nếu hồ sơ chưa có ngày sinh."),
             ("gioiTinh", "enum GioiTinh", "`null` nếu chưa nhập."),
             ("soDienThoai", "string", "Số điện thoại liên hệ của lượt khám. `null` khi lượt khám có người giám hộ "
                                       "(số liên hệ khi đó là của người giám hộ) hoặc lịch hẹn tạo trước khi lưu số "
                                       "này."),
         ]),
         ("NguoiGiamHo", "giá trị của `nguoiGiamHo`.", [
             ("hoTen", "string", ""),
             ("quanHe", "enum QuanHeGiamHo", "TK 3.7."),
             ("soDienThoai", "string", ""),
         ]),
         ("DoiChieu", "giá trị của `doiChieu` (3.6).", [
             ("canDoiChieu", "boolean", "`true`: họ tên hoặc ngày sinh đã nhập khác hồ sơ (hoặc khác người giám hộ đã "
                                        "khai) và chưa ai đánh dấu đã đối chiếu."),
             ("hoTenDaNhap", "string", "`null` ở lịch hẹn tạo trước khi lưu thông tin đã nhập; khi đó các trường đã "
                                       "nhập khác cũng `null`."),
             ("ngaySinhDaNhap", "date", ""),
             ("gioiTinhDaNhap", "enum GioiTinh", ""),
             ("hoTenGiamHoDaNhap", "string", "`null` nếu lượt khám không có người giám hộ."),
         ]),
     ), ()),
    ("HangDoiResponse", "Hàng đợi khám 1 ngày của bác sĩ (3.5), theo từng ca đang hoạt động, ca bắt đầu sớm nhất "
                        "trước.", [
        ("ngay", "date", "Ngày của hàng đợi."),
        ("idCaHienTai", "number", "Ca nên mở sẵn: hôm nay là ca đang diễn ra, không có thì ca kế tiếp trong ngày, hết "
                                  "ca thì ca cuối; ngày khác là ca đầu tiên. `null` khi ngày đó không có ca."),
        ("tinhLuc", "datetime", "Thời điểm máy chủ tính hàng đợi; phần lẻ của giây dài tới 9 chữ số."),
        ("cacCa", "CaHangDoi[]", "`[]` khi ngày đó không có ca."),
    ], (
        ("CaHangDoi", "phần tử của `cacCa`.", [
            ("idLichLamViec", "number", "Id ca; gửi cho route gọi tiếp."),
            ("buoi", "enum BuoiKham", "3.9."),
            ("gioBatDau", "time", ""),
            ("gioKetThuc", "time", ""),
            ("phongKham", "PhongKhamTomTatResponse", "TK 4.15."),
            ("soLuong", "SoLuong", ""),
            ("idDangKham", "number", "Id lịch hẹn đang `DANG_KHAM` của ca; `null` nếu không có."),
            ("idTiepTheo", "number", "Id lịch hẹn mà route gọi tiếp sẽ gọi: lượt `CHO_KHAM` có số thứ tự nhỏ nhất. "
                                     "Chỉ có ở hàng đợi hôm nay; `null` ở ngày khác hoặc khi hết lượt chờ."),
            ("cacLuot", "LuotHangDoi[]", "Theo số thứ tự."),
        ]),
        ("SoLuong", "số lượt của ca theo `trangThaiHangDoi`.", [
            ("tong", "number", "Số lượt trong hàng đợi của ca, bằng tổng 4 số dưới."),
            ("choKham", "number", ""),
            ("dangKham", "number", ""),
            ("vangMat", "number", ""),
            ("daKham", "number", ""),
        ]),
        ("LuotHangDoi", "phần tử của `cacLuot`.", [
            ("trangThaiHangDoi", "enum TrangThaiHangDoi", "3.5."),
            ("goiLuc", "datetime", "Lần gần nhất bác sĩ gọi vào khám; `null` nếu chưa gọi."),
            ("lichHen", "LichHenTrongCaResponse", "4.2, với `bacSi` = `null`."),
        ]),
    ), ()),
    ("HoSoKhamResponse",
     "Hồ sơ khám bác sĩ mở từ 1 lịch hẹn của mình: lịch hẹn, hồ sơ bệnh nhân đầy đủ và các lần khám trước. Chỉ trả cho "
     "bác sĩ của lịch hẹn: số CCCD, số điện thoại không che (bác sĩ đối chiếu giấy tờ).", [
         ("lichHen", "LichHenTrongCaResponse", "Lịch hẹn đang mở (4.2), kèm `doiChieu`."),
         ("hoSoBenhNhan", "HoSoBenhNhan", ""),
         ("ketQua", "KetQuaKhamResponse", "Kết quả khám của lịch hẹn đang mở: TK 4.27, có thêm `phienBan`, `hanSua` "
                                          "(dưới bảng). `null` nếu chưa ghi hồ sơ bệnh án."),
         ("lanKhamTruoc", "LanKham[]", "Các lịch hẹn khác của bệnh nhân (với mọi bác sĩ, mọi trạng thái) có giờ khám "
                                       "trước lịch hẹn này, mới nhất trước."),
     ], (
         ("HoSoBenhNhan", "giá trị của `hoSoBenhNhan`.", [
             ("id", "number", "Id hồ sơ bệnh nhân, bằng `lichHen.idHoSoBenhNhan`."),
             ("cccd", "string", "Không che; `null` với trẻ chưa có CCCD."),
             ("hoTen", "string", ""),
             ("ngaySinh", "date", "`null` nếu hồ sơ chưa có ngày sinh."),
             ("tuoi", "number", "Số tuổi tròn vào ngày khám; `null` nếu hồ sơ chưa có ngày sinh."),
             ("gioiTinh", "enum GioiTinh", "`null` nếu chưa nhập."),
             ("soDienThoai", "string", "Số điện thoại lưu trong hồ sơ."),
             ("diaChi", "string", "`null` nếu chưa nhập."),
             ("soBaoHiemYTe", "string", "`null` nếu chưa nhập."),
             ("tienSuBenhLy", "string", "Bệnh nhân tự khai ở hồ sơ của mình (TK 17); `null` nếu chưa có."),
             ("daLienKetTaiKhoan", "boolean", "`true`: hồ sơ đã gắn với 1 tài khoản bệnh nhân; chủ tài khoản tự sửa "
                                              "được hồ sơ."),
         ]),
         ("LanKham", "phần tử của `lanKhamTruoc`.", [
             ("idLichHen", "number", "Dùng cho route lịch sử sửa của lượt khám đó (3.7)."),
             ("maTraCuu", "string", ""),
             ("ngay", "date", "Ngày khám."),
             ("gioKhamDuKien", "datetime", ""),
             ("bacSi", "BacSiTomTatResponse", "Bác sĩ của lần khám đó (TK 4.14)."),
             ("tenChuyenKhoa", "string", "Theo `Accept-Language`."),
             ("trangThai", "enum TrangThaiLichHen", ""),
             ("lyDoKham", "string", "Lý do khám người đặt ghi."),
             ("ketQua", "KetQuaKhamResponse", "Như `ketQua` ở trên; `null` nếu lần khám đó chưa có hồ sơ bệnh án."),
         ]),
     ), (
         "**Kết quả khám khi bác sĩ xem.** `KetQuaKhamResponse` ở đây khác TK 4.27 đúng 1 điểm: `phienBan` và `hanSua` có mặt và có giá "
         "trị, ở `ketQua` và ở `lanKhamTruoc[].ketQua`. `phienBan` (number): phiên bản hồ sơ bệnh án, gửi lại khi sửa "
         "(3.7). `hanSua` (datetime): sau thời điểm này hồ sơ chỉ xem được. Chỉ bác sĩ của lịch hẹn sửa được hồ sơ, "
         "nên với lần khám trước của bác sĩ khác, 2 trường này chỉ để hiển thị.",
     )),
    ("BenhAnResponse", "Hồ sơ bệnh án của 1 lượt khám, cho bác sĩ đã khám: response của các route ghi, sửa và xem hồ "
                       "sơ bệnh án.", [
        ("id", "number", "Id hồ sơ bệnh án."),
        ("idLichHen", "number", ""),
        ("maTraCuu", "string", "Mã tra cứu của lịch hẹn."),
        ("trangThaiLichHen", "enum TrangThaiLichHen", "`DA_HOAN_THANH` sau khi ghi."),
        ("lyDoKham", "string", "`null` nếu không ghi."),
        ("sinhHieu", "SinhHieu", "Như `SinhHieu` ở TK 4.27; `null` nếu chưa đo chỉ số nào."),
        ("khamLamSang", "string", "`null` nếu không ghi."),
        ("chanDoan", "string", "Chẩn đoán chính, chữ bác sĩ gõ."),
        ("icdChinh", "Icd10Response", "Mã ICD-10 của chẩn đoán chính (4.9); `null` nếu không chọn mã."),
        ("chanDoanKemTheo", "string", "`null` nếu không có."),
        ("ghiChu", "string", "Lời dặn của bác sĩ; `null` nếu không có."),
        ("ngayTaiKhamDeXuat", "date", "`null` nếu không có."),
        ("donThuoc", "DonThuoc[]", "Như `DonThuoc` ở TK 4.27; `[]` nếu không kê đơn. Khi sửa, gửi lại `idThuoc` để "
                                   "giữ dòng thuốc."),
        ("ngayTao", "datetime", "Lúc ghi hồ sơ (phiên bản 1); hạn sửa tính từ đây."),
        ("phienBan", "number", "1 khi ghi, tăng 1 mỗi lần sửa; gửi lại khi sửa (3.7)."),
        ("ngaySuaCuoi", "datetime", "Lần sửa gần nhất; `null` = chưa sửa."),
        ("hanSua", "datetime", "Sau thời điểm này hồ sơ chỉ xem được."),
    ], (), ()),
    ("LichSuSuaBenhAnResponse", "Lịch sử sửa hồ sơ bệnh án của 1 lượt khám (3.7).", [
        ("idLichHen", "number", ""),
        ("maTraCuu", "string", ""),
        ("phienBanHienTai", "number", "`1` = chưa sửa lần nào."),
        ("ngayGhiNhan", "datetime", "Lúc ghi hồ sơ (phiên bản 1)."),
        ("cacLanSua", "LanSua[]", "Mới nhất trước; `[]` nếu chưa sửa lần nào."),
    ], (
        ("LanSua", "phần tử của `cacLanSua`.", [
            ("phienBan", "number", "Phiên bản hồ sơ sau lần sửa này (2, 3...)."),
            ("ngaySua", "datetime", ""),
            ("bacSi", "BacSiSua", "Bác sĩ đã sửa."),
            ("lyDo", "string", "Lý do sửa."),
            ("cacTruongDoi", "enum TruongBenhAn[]", "Phần đã đổi, theo thứ tự trên hồ sơ (3.9)."),
            ("truoc", "NoiDungBenhAn", "Toàn bộ nội dung trước lần sửa (4.7)."),
            ("sau", "NoiDungBenhAn", "Toàn bộ nội dung sau lần sửa."),
        ]),
        ("BacSiSua", "giá trị của `bacSi`.", [
            ("id", "number", "Id bác sĩ."),
            ("hoTen", "string", ""),
        ]),
    ), ()),
    ("NoiDungBenhAn",
     "Nội dung hồ sơ bệnh án tại 1 thời điểm, như đã lưu lúc sửa. Thuốc giữ tên và hàm lượng của lúc đó. Lần sửa ghi "
     "trước khi hồ sơ có lý do khám, sinh hiệu, khám lâm sàng, mã ICD-10, bệnh kèm theo và số lượng thuốc có các trường "
     "đó là `null`.", [
         ("lyDoKham", "string", ""),
         ("sinhHieu", "SinhHieu", "Như `SinhHieu` ở TK 4.27; `null` nếu chưa đo chỉ số nào."),
         ("khamLamSang", "string", ""),
         ("chanDoan", "string", ""),
         ("maIcdChinh", "string", "Chỉ mã ICD-10, vd `J06.9`, không kèm tên."),
         ("chanDoanKemTheo", "string", ""),
         ("ghiChu", "string", "Lời dặn."),
         ("ngayTaiKhamDeXuat", "date", ""),
         ("donThuoc", "DongThuoc[]", "Theo thứ tự bác sĩ đã gửi."),
     ], (
         ("DongThuoc", "phần tử của `donThuoc`.", [
             ("idThuoc", "number", "So cùng 1 thuốc giữa `truoc` và `sau` bằng id này."),
             ("tenThuoc", "string", ""),
             ("hamLuong", "string", ""),
             ("lieuDung", "string", ""),
             ("soLanMoiNgay", "number", ""),
             ("soNgayDung", "number", ""),
             ("soLuong", "number", "`null` ở dòng ghi trước khi bắt buộc số lượng."),
             ("ghiChuSuDung", "string", ""),
         ]),
     ), ()),
    ("ThuocGoiYResponse", "1 thuốc kê được (đang dùng, đã xác minh) trong danh mục, gợi ý khi bác sĩ gõ tên thuốc "
                          "hoặc hoạt chất (3.8).", [
        ("id", "number", "Gửi ở `donThuoc[].idThuoc`."),
        ("tenThuoc", "string", ""),
        ("hoatChat", "string", ""),
        ("hamLuong", "string", "Vd `500mg`; hiện cạnh tên thuốc."),
        ("dangBaoChe", "string", "Theo `Accept-Language` (vd viên nén / tablet); `null` nếu chưa nhập."),
        ("duongDung", "enum DuongDungThuoc", "TK 3.7; `null` nếu chưa nhập."),
        ("donVi", "string", "Đơn vị của `soLuong`, vd viên; theo `Accept-Language`."),
    ], (), ()),
    ("Icd10Response", "1 mã trong danh mục ICD-10: gợi ý khi bác sĩ gõ chẩn đoán, và `icdChinh` của hồ sơ bệnh án.", [
        ("ma", "string", "Vd `J06.9`; gửi lại ở `maIcdChinh`."),
        ("ten", "string", "Theo `Accept-Language` (TK 3.12)."),
        ("manTinh", "boolean", "`true`: bệnh mạn tính thuộc Phụ lục VII Thông tư 26/2025/TT-BYT: mỗi thuốc kê được tới "
                               "90 ngày (không thì 30)."),
    ], (), ()),
]


def sua(tl):
    x = kit.tieu_de(1, "4. Mô hình dữ liệu (response)")
    x += kit.doan("Các đối tượng dưới đây nằm trong `duLieu`. Kiểu dữ liệu viết như TK 4 (`datetime`, `date`, "
                  "`time`, `X[]`); trường không có giá trị là `null` (TK 3.1). Mô hình dùng chung đọc ở TK: "
                  "`BacSiTomTatResponse` (TK 4.14), `PhongKhamTomTatResponse` (TK 4.15), `KetQuaKhamResponse` (TK 4.27; "
                  "phần khác khi bác sĩ xem ở 4.4). `SinhHieu` và `DonThuoc` của `BenhAnResponse` có đúng các trường "
                  "của `SinhHieu`, `DonThuoc` ở TK 4.27.")
    for so, (ten, mo_ta, dong, con, ghi_chu) in enumerate(MO_HINH, 1):
        x += mo_hinh(so, ten, mo_ta, dong, con, ghi_chu)
    them_cuoi(tl, x)
