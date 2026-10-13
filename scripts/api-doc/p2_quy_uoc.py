"""Giai đoạn 2: mở rộng trang đầu, mục 1, 3, 4 cho phần đặt lịch khám phía bệnh nhân (quy ước và mô hình response).
Chưa thêm route nào. Chạy qua build.py.
"""

import kit

RONG_MO_HINH_MOI = (2150, 3100, 4388)  # cột Kiểu rộng hơn bảng 4.1-4.9: tên response của booking dài
RONG_GIOI_HAN = (4300, 5338)
RONG_ENUM = (2400, 7238)
RONG_QUY_TAC = (2000, 7638)
MUC_4 = "4. Mô hình dữ liệu (response)"


def bang_mo_hinh(dong):
    return kit.bang(RONG_MO_HINH_MOI, ("Trường", "Kiểu", "Mô tả"), [(f"`{t}`", k, m) for t, k, m in dong]) \
        + kit.khoang_trong(100)


def mo_hinh(so, ten, mo_ta, dong, con=()):
    """1 mục 4.n. con: các record lồng (tên, câu giới thiệu, dòng), mỗi record 1 bảng riêng dưới cùng heading."""
    x = kit.tieu_de(2, f"4.{so} {ten}") + (kit.doan(mo_ta) if mo_ta else "") + bang_mo_hinh(dong)
    for ten_con, gioi_thieu, dong_con in con:
        x += kit.doan(f"`{ten_con}` — {gioi_thieu}") + bang_mo_hinh(dong_con)
    return x


# (tên, mô tả, dòng, record lồng). Thứ tự ở đây là thứ tự đánh số 4.10, 4.11...
MO_HINH = [
    ("ChuyenKhoaResponse", "Chuyên khoa (catalog-service).", [
        ("id", "number", "Id chuyên khoa; dùng cho tham số `idChuyenKhoa`."),
        ("tenChuyenKhoa", "string", ""),
        ("moTa", "string", "Mô tả ngắn; `null` nếu chưa nhập."),
    ], ()),
    ("BacSiResponse",
     "Bác sĩ trong danh sách công khai, đủ để vẽ thẻ bác sĩ. Chỉ có bác sĩ đang công tác và tài khoản đã kích hoạt. "
     "Không có email, số điện thoại, số giấy phép.", [
         ("id", "number", "Id bác sĩ; dùng cho tham số `idBacSi`."),
         ("hoTen", "string", ""),
         ("anhDaiDien", "string", "URL ảnh; `null` thì dùng ảnh mặc định."),
         ("hocVi", "string", "Vd `TS.BS`, `BSCKII`."),
         ("chucVu", "string", "`null` nếu chưa nhập."),
         ("soNamKinhNghiem", "number", "`null` nếu chưa nhập."),
         ("gioiThieuNgan", "string", "1-2 câu hiện trên thẻ; `null` nếu chưa nhập."),
         ("idChuyenKhoa", "number", ""),
         ("tenChuyenKhoa", "string", ""),
         ("diemDanhGia", "number", "Điểm trung bình của bệnh nhân, 1 chữ số thập phân (vd `4.5`); `null` khi chưa có "
                                   "đánh giá nào."),
         ("soDanhGia", "number", "Số lượt đánh giá (`0` khi chưa có). Luôn hiển thị kèm điểm, vd ★ 4.5 (12)."),
     ], ()),
    ("BacSiChiTietResponse",
     "Chi tiết công khai của 1 bác sĩ: mọi trường của `BacSiResponse` và các trường dưới đây. Nhận xét của bệnh nhân "
     "không công khai.", [
         ("id", "number", "Như `BacSiResponse`."),
         ("hoTen", "string", "Như `BacSiResponse`."),
         ("anhDaiDien", "string", "Như `BacSiResponse`."),
         ("hocVi", "string", "Như `BacSiResponse`."),
         ("chucVu", "string", "Như `BacSiResponse`."),
         ("soNamKinhNghiem", "number", "Như `BacSiResponse`."),
         ("gioiThieuNgan", "string", "Như `BacSiResponse`."),
         ("idChuyenKhoa", "number", "Như `BacSiResponse`."),
         ("tenChuyenKhoa", "string", "Như `BacSiResponse`."),
         ("tieuSu", "string", "Đoạn giới thiệu dài; `null` nếu chưa nhập."),
         ("quaTrinhDaoTao", "string[]", "Mỗi phần tử 1 dòng; mảng rỗng khi chưa có."),
         ("quaTrinhCongTac", "string[]", "Mỗi phần tử 1 dòng; mảng rỗng khi chưa có."),
         ("linhVucKhamChua", "string[]", "Mỗi phần tử 1 dòng; mảng rỗng khi chưa có."),
         ("anh", "AnhBacSiResponse[]", "Ảnh giới thiệu, đã xếp đúng thứ tự hiển thị; mảng rỗng khi chưa có."),
         ("diemDanhGia", "number", "Như `BacSiResponse`."),
         ("soDanhGia", "number", "Như `BacSiResponse`."),
     ], ()),
    ("AnhBacSiResponse", "1 ảnh giới thiệu của bác sĩ.", [
        ("id", "number", ""),
        ("loai", "enum LoaiAnhBacSi", ""),
        ("url", "string", "URL ảnh."),
        ("chuThich", "string", "`null` nếu không có chú thích."),
    ], ()),
    ("BacSiTomTatResponse",
     "Bác sĩ hiển thị cạnh khung giờ, lịch hẹn, phiếu khám (booking-service). Chi tiết lấy ở catalog-service theo `id`.", [
         ("id", "number", "Id bác sĩ."),
         ("hoTen", "string", ""),
         ("hocVi", "string", ""),
         ("anhDaiDien", "string", "`null` nếu chưa có."),
     ], ()),
    ("PhongKhamTomTatResponse", "", [
        ("id", "number", ""),
        ("tenPhong", "string", "Vd `Phòng 101`."),
        ("tang", "string", "Tầng của phòng; `null` nếu chưa nhập."),
    ], ()),
    ("CaKhamResponse",
     "1 ca làm việc còn đặt lịch được trong ngày, kèm các khung 1 giờ còn kịp đặt. Khi đặt lịch với bác sĩ cụ thể, khung "
     "giờ được xác định bằng `idLichLamViec` + `khungGio[].gioBatDau`.", [
         ("idLichLamViec", "number", "Id ca làm việc; gửi lại khi đặt lịch / đổi lịch."),
         ("ngay", "date", ""),
         ("gioBatDau", "time", "Giờ bắt đầu ca."),
         ("gioKetThuc", "time", "Giờ kết thúc ca."),
         ("soLuotToiDaMoiGio", "number", "Số lượt khám tối đa của 1 khung 1 giờ."),
         ("thoiLuongLuotPhut", "number", "Số phút của 1 lượt khám."),
         ("bacSi", "BacSiTomTatResponse", ""),
         ("phongKham", "PhongKhamTomTatResponse", ""),
         ("khungGio", "KhungGioResponse[]", "Các khung 1 giờ của ca, theo giờ tăng dần."),
     ], ()),
    ("KhungGioResponse", "1 khung 1 giờ của ca làm việc. Khung cuối của ca có thể ngắn hơn 1 giờ.", [
        ("gioBatDau", "datetime", "Gửi lại làm `gioBatDauKhung` khi đặt lịch."),
        ("gioKetThuc", "datetime", ""),
        ("tongSoCho", "number", "Số lượt khám của khung."),
        ("soChoConLai", "number", "Số lượt còn trống và còn kịp đặt."),
        ("hetCho", "boolean", "`true` khi `soChoConLai` = 0: hiện \"Hết chỗ\", không cho chọn."),
    ], ()),
    ("KhungGioGopResponse",
     "1 khung giờ của chuyên khoa trong ngày, gộp các khung cùng giờ bắt đầu của mọi bác sĩ thuộc chuyên khoa; dùng khi "
     "người đặt chọn \"bác sĩ bất kỳ\". Khung của 2 ca bắt đầu lệch giờ nhau (08:00 và 08:30) là 2 dòng riêng.", [
         ("gioBatDau", "datetime", "Gửi lại làm `gioBatDauKhung` cùng `idChuyenKhoa` khi đặt lịch."),
         ("gioKetThuc", "datetime", "Giờ kết thúc muộn nhất trong các khung được gộp."),
         ("tongSoCho", "number", "Tổng số lượt của các khung được gộp."),
         ("soChoConLai", "number", "Tổng số lượt còn trống và còn kịp đặt."),
         ("hetCho", "boolean", "`true` khi `soChoConLai` = 0."),
         ("soBacSi", "number", "Số bác sĩ có khung bắt đầu vào giờ này."),
     ], ()),
    ("NgayConChoResponse", "1 ngày có ca làm việc đặt lịch được.", [
        ("ngay", "date", ""),
        ("soChoConLai", "number", "`0`: ngày có ca nhưng đã hết chỗ."),
    ], ()),
    ("NgaySomNhatResponse",
     "Ngày còn chỗ sớm nhất của 1 bác sĩ, hiện trên thẻ bác sĩ; ghép với `BacSiResponse` theo `idBacSi`.", [
         ("idBacSi", "number", ""),
         ("ngay", "date", ""),
         ("soChoConLai", "number", "Số chỗ còn lại của ngày đó."),
     ], ()),
    ("DatLichResponse",
     "Kết quả đặt lịch và đổi lịch. Không có id lịch hẹn: mọi thao tác sau đó dùng `maPhieuKham` (3.10).", [
         ("maPhieuKham", "string", "Mã phiếu khám, 43 ký tự; dùng trên đường dẫn API và trong link phiếu khám."),
         ("maTraCuu", "string", "Mã ngắn để đọc / in, vd `ECL-20261005-4198`; không mở được phiếu khám."),
         ("linkPhieuKham", "string", "Link trang phiếu khám trên frontend; cũng là nội dung mã QR."),
         ("soThuTu", "number", "Số thứ tự khám theo phòng khám và ngày."),
         ("ngay", "date", "Ngày khám."),
         ("gioKhamDuKien", "datetime", "Giờ bắt đầu lượt khám được xếp."),
         ("gioBatDauKhung", "datetime", "Khung 1 giờ đã chọn."),
         ("gioKetThucKhung", "datetime", ""),
         ("trangThai", "enum TrangThaiLichHen", "Luôn là `CHO_XAC_NHAN` ở lịch vừa đặt / vừa đổi."),
         ("bacSi", "BacSiTomTatResponse", ""),
         ("phongKham", "PhongKhamTomTatResponse", ""),
         ("hoTenBenhNhan", "string", "Đúng họ tên người đặt vừa nhập."),
         ("hoTenNguoiGiamHo", "string", "`null` nếu lượt khám không có người giám hộ."),
         ("luuVaoTaiKhoan", "boolean", "`true`: đặt khi đã đăng nhập, lịch có trong \"lịch hẹn của tôi\". `false`: "
                                       "khách đặt, chỉ xem lại được bằng link phiếu khám."),
         ("bacSiDoPhongKhamXep", "boolean", "`true`: người đặt chọn \"bác sĩ bất kỳ\", hệ thống đã xếp bác sĩ ở `bacSi`."),
     ], ()),
    ("PhieuKhamResponse",
     "Phiếu khám xem bằng `maPhieuKham`, không cần đăng nhập. Ai có link cũng xem được nên CCCD và số điện thoại đã che, "
     "không có id lịch hẹn hay id hồ sơ.", [
         ("maPhieuKham", "string", ""),
         ("maTraCuu", "string", "Mã ngắn để đọc / in."),
         ("linkPhieuKham", "string", "Link trang phiếu khám; cũng là nội dung mã QR."),
         ("trangThai", "enum TrangThaiLichHen", ""),
         ("soThuTu", "number", ""),
         ("ngay", "date", ""),
         ("gioKhamDuKien", "datetime", "Giờ bắt đầu lượt khám được xếp."),
         ("gioBatDauKhung", "datetime", ""),
         ("gioKetThucKhung", "datetime", ""),
         ("bacSi", "BacSiTomTatResponse", ""),
         ("tenChuyenKhoa", "string", ""),
         ("phongKham", "PhongKhamTomTatResponse", ""),
         ("lyDoKham", "string", ""),
         ("ngayDat", "datetime", "Thời điểm đặt lịch."),
         ("benhNhan", "BenhNhanPhieuKhamResponse", ""),
         ("nguoiGiamHo", "NguoiGiamHoPhieuKhamResponse", "`null` nếu lượt khám không có người giám hộ."),
         ("canNguoiGiamHoDiCung", "boolean", "`true`: hiện ghi chú \"người giám hộ phải đi cùng\"."),
         ("lyDoHuy", "string", "Lý do bác sĩ từ chối (`BI_TU_CHOI`) hoặc lý do hủy; `null` ở các trạng thái khác."),
         ("duocHuyDoi", "boolean", "`true`: lịch còn hủy / đổi được (3.11)."),
         ("hanHuyDoi", "datetime", "Hạn chót hủy / đổi lịch trên hệ thống."),
         ("canDoiLich", "boolean", "`true`: ca khám đã bị hủy, lịch đang chờ đổi sang khung giờ khác (3.11)."),
     ], ()),
    ("BenhNhanPhieuKhamResponse", "Người khám trên phiếu khám công khai.", [
        ("hoTen", "string", ""),
        ("namSinh", "number", "Chỉ năm sinh."),
        ("gioiTinh", "enum GioiTinh", ""),
        ("cccd", "string", "Đã che, chỉ còn 3 số cuối; `null` với trẻ chưa có CCCD."),
        ("soDienThoai", "string", "Số điện thoại liên hệ, đã che. `null` khi lượt khám có người giám hộ (số liên hệ là "
                                  "của người giám hộ)."),
    ], ()),
    ("NguoiGiamHoPhieuKhamResponse", "Người giám hộ trên phiếu khám công khai; không có CCCD.", [
        ("hoTen", "string", ""),
        ("quanHe", "enum QuanHeGiamHo", ""),
        ("soDienThoai", "string", "Đã che."),
    ], ()),
    ("LichHenCuaToiResponse",
     "1 dòng trong \"lịch hẹn của tôi\". Không có id lịch hẹn: chi tiết, hủy, đổi, QR đều dùng `maPhieuKham`.", [
         ("maPhieuKham", "string", ""),
         ("maTraCuu", "string", "Mã ngắn để đọc / in."),
         ("linkPhieuKham", "string", ""),
         ("trangThai", "enum TrangThaiLichHen", ""),
         ("soThuTu", "number", ""),
         ("ngay", "date", ""),
         ("gioKhamDuKien", "datetime", ""),
         ("gioBatDauKhung", "datetime", ""),
         ("gioKetThucKhung", "datetime", ""),
         ("bacSi", "BacSiTomTatResponse", ""),
         ("tenChuyenKhoa", "string", ""),
         ("phongKham", "PhongKhamTomTatResponse", ""),
         ("hoTenBenhNhan", "string", "`laBanThan` = `true`: họ tên trong hồ sơ của chủ tài khoản. Còn lại: họ tên người "
                                     "đặt đã nhập."),
         ("laBanThan", "boolean", "`true`: người khám là chủ tài khoản, bất kể ai đặt. `false`: lịch của người khác (đặt "
                                  "cho người thân, hoặc chủ tài khoản là người giám hộ)."),
         ("hoTenNguoiGiamHo", "string", "`null` nếu không có người giám hộ."),
         ("lyDoKham", "string", ""),
         ("ngayDat", "datetime", "Thời điểm đặt lịch."),
         ("nguoiDat", "enum NguoiDatLich", "Ai đã đặt lịch này, nhìn từ tài khoản đang xem."),
         ("thongTinKhacHoSo", "boolean", "Chỉ có nghĩa khi `laBanThan`: người đặt đã nhập họ tên / ngày sinh khác hồ sơ "
                                         "của chủ tài khoản. `true` thì hiện cảnh báo \"lịch được đặt với thông tin khác "
                                         "hồ sơ của bạn\". Luôn `false` với lịch của người khác."),
         ("lyDoHuy", "string", "Lý do bác sĩ từ chối hoặc lý do hủy; `null` ở các trạng thái khác."),
         ("duocHuyDoi", "boolean", "`true`: tài khoản đang xem còn hủy / đổi được lịch này (3.11)."),
         ("hanHuyDoi", "datetime", "Hạn chót hủy / đổi lịch trên hệ thống."),
         ("canDoiLich", "boolean", "`true`: ca khám đã bị hủy, lịch đang chờ đổi sang khung giờ khác (3.11)."),
     ], ()),
    ("LichHenChiTietCuaToiResponse", "Chi tiết 1 lịch hẹn của tôi, mở bằng `maPhieuKham`.", [
        ("lichHen", "LichHenCuaToiResponse", "Đúng như dòng trong danh sách."),
        ("ngaySinhBenhNhan", "date", "`lichHen.laBanThan`: ngày sinh trong hồ sơ của chủ tài khoản. Còn lại: ngày sinh "
                                     "người đặt đã nhập."),
        ("gioiTinhBenhNhan", "enum GioiTinh", ""),
        ("soDienThoaiLienHe", "string", "Số liên hệ của lượt khám, không che; `null` nếu lịch không do tài khoản đang xem "
                                        "đặt."),
        ("emailLienHe", "string", "`null` nếu không nhập, hoặc lịch không do tài khoản đang xem đặt."),
        ("ketQua", "KetQuaKhamResponse", "`null` nếu chưa khám xong, hoặc tài khoản không được xem kết quả. Chỉ được xem "
                                         "khi `lichHen.laBanThan` hoặc `lichHen.nguoiDat` = `TOI`."),
        ("duocDanhGia", "boolean", "`true`: lượt khám đã hoàn thành, tài khoản được xem kết quả và chưa gửi đánh giá → "
                                   "hiện form đánh giá."),
        ("danhGia", "DanhGiaCuaToiResponse", "Đánh giá đã gửi; `null` nếu chưa có hoặc không được xem kết quả."),
    ], ()),
    ("KetQuaKhamResponse", "Kết quả 1 lượt khám do bác sĩ ghi.", [
        ("chanDoan", "string", ""),
        ("ghiChu", "string", "Lời dặn của bác sĩ; `null` nếu không có."),
        ("ngayTaiKhamDeXuat", "date", "Gợi ý ngày tái khám, không phải lịch hẹn; `null` nếu không có."),
        ("donThuoc", "DonThuoc[]", "Mảng rỗng nếu không kê đơn."),
    ], (
        ("DonThuoc", "phần tử của `donThuoc`.", [
            ("tenThuoc", "string", ""),
            ("donVi", "string", "Đơn vị của thuốc trong danh mục, vd `viên`."),
            ("lieuDung", "string", ""),
            ("soLanMoiNgay", "number", "`null` nếu bác sĩ không ghi."),
            ("soNgayDung", "number", "`null` nếu bác sĩ không ghi."),
            ("ghiChuSuDung", "string", "`null` nếu không có."),
        ]),
    )),
    ("DanhGiaCuaToiResponse", "Đánh giá bệnh nhân đã gửi cho 1 lượt khám.", [
        ("soSao", "number", "1 đến 5."),
        ("nhanXet", "string", "`null` nếu không viết."),
        ("ngayTao", "datetime", "Lúc gửi lần đầu."),
        ("duocSuaDen", "datetime", "Sửa được tới thời điểm này."),
    ], ()),
    ("LanKhamCuaToiResponse", "1 lượt đã khám trong lịch sử khám.", [
        ("lichHen", "LichHenCuaToiResponse", "Như 1 dòng \"lịch hẹn của tôi\"."),
        ("ketQua", "KetQuaKhamResponse", "Chẩn đoán, lời dặn, ngày tái khám, đơn thuốc."),
    ], ()),
    ("HoSoCuaToiResponse",
     "Hồ sơ bệnh nhân của tài khoản đang đăng nhập; CCCD và số điện thoại không che. Khi `trangThaiLienKet` = "
     "`CHO_XAC_MINH` thì mọi trường khác đều là `null`.", [
         ("trangThaiLienKet", "enum TrangThaiLienKet", ""),
         ("cccd", "string", "Không đổi được sau khi đã lưu."),
         ("hoTen", "string", ""),
         ("ngaySinh", "date", ""),
         ("gioiTinh", "enum GioiTinh", ""),
         ("soDienThoai", "string", ""),
         ("diaChi", "string", ""),
         ("soBaoHiemYTe", "string", ""),
         ("tienSuBenhLy", "string", ""),
     ], ()),
    ("ThongTinDatLichResponse", "Mọi thứ để điền sẵn form đặt lịch cho bệnh nhân đã đăng nhập, trong 1 lần gọi.", [
        ("banThan", "HoSoCuaToiResponse", "Hồ sơ của tài khoản, cho \"đặt cho bản thân\"; `null` nếu tài khoản chưa có "
                                          "hồ sơ; hồ sơ đang chờ xác minh thì chỉ có `trangThaiLienKet`."),
        ("emailTaiKhoan", "string", "Email đăng nhập, gợi ý cho ô email của form."),
        ("nguoiThan", "NguoiThanDaLuuResponse[]", "Người thân đã lưu, người vừa đặt lịch gần nhất đứng trước; tối đa 10."),
        ("lanDatGanNhat", "LanDatGanNhat", "`null` nếu tài khoản chưa đặt lần nào."),
    ], (
        ("LanDatGanNhat", "chuyên khoa và bác sĩ của lịch hẹn gần nhất tài khoản đã đặt.", [
            ("idChuyenKhoa", "number", ""),
            ("tenChuyenKhoa", "string", ""),
            ("idBacSi", "number", ""),
            ("hoTenBacSi", "string", ""),
        ]),
    )),
    ("NguoiThanDaLuuResponse",
     "1 người thân đã lưu của tài khoản: đúng những gì tài khoản nhập ở lần đặt lịch gần nhất cho người này, không che. "
     "Tên trường trùng với `benhNhan` / `nguoiGiamHo` của request đặt lịch để chép thẳng vào form.", [
         ("id", "number", "Dùng để sửa / bỏ người này khỏi danh sách đã lưu."),
         ("hoTen", "string", ""),
         ("ngaySinh", "date", ""),
         ("gioiTinh", "enum GioiTinh", ""),
         ("cccd", "string", "`null` nếu người dưới 18 tuổi được đặt khi chưa có CCCD."),
         ("soDienThoai", "string", ""),
         ("email", "string", "`null` nếu không nhập."),
         ("diaChi", "string", "`null` nếu không nhập."),
         ("soBaoHiemYTe", "string", "`null` nếu không nhập."),
         ("nguoiGiamHo", "NguoiGiamHo", "`null` nếu lần đặt gần nhất không có người giám hộ."),
         ("lanDungCuoi", "datetime", "Lần đặt lịch gần nhất cho người này."),
     ], (
         ("NguoiGiamHo", "người giám hộ đã lưu kèm.", [
             ("hoTen", "string", ""),
             ("quanHe", "enum QuanHeGiamHo", ""),
             ("soDienThoai", "string", ""),
             ("cccd", "string", ""),
             ("ngaySinh", "date", "`null` nếu không nhập."),
         ]),
     )),
    ("TrangCaNhanResponse", "Mọi thứ trang cá nhân của bệnh nhân hiển thị, trong 1 lần gọi.", [
        ("hoSo", "HoSoCuaToiResponse", "`null` nếu tài khoản chưa có hồ sơ; hồ sơ đang chờ xác minh thì chỉ có "
                                       "`trangThaiLienKet`."),
        ("emailTaiKhoan", "string", "Email đăng nhập."),
        ("nguoiThan", "NguoiThanDaLuuResponse[]", "Người thân đã lưu, người vừa đặt lịch gần nhất đứng trước."),
        ("lichSapToi", "LichSapToi", "5 lịch sắp tới gần nhất của mỗi bên."),
        ("lanKhamGanDay", "LanKhamCuaToiResponse[]", "5 lượt đã khám gần nhất mà tài khoản được xem kết quả."),
        ("soLich", "SoLich", "Số đếm, để biết còn lịch ngoài các dòng trả kèm."),
    ], (
        ("LichSapToi", "lịch sắp tới, chia theo người khám.", [
            ("cuaToi", "LichHenCuaToiResponse[]", "Người khám là chủ tài khoản, bất kể ai đặt."),
            ("cuaNguoiKhac", "LichHenCuaToiResponse[]", "Lịch đặt cho người thân, hoặc chủ tài khoản là người giám hộ."),
        ]),
        ("SoLich", "tổng số lịch theo loại.", [
            ("sapToiCuaToi", "number", ""),
            ("sapToiCuaNguoiKhac", "number", ""),
            ("lichSu", "number", "Lịch đã khám, đã hủy, bị từ chối hoặc đã qua giờ, của cả hai bên."),
            ("daKham", "number", "Số lượt đã khám mà tài khoản được xem kết quả."),
        ]),
    )),
    ("ThongBaoResponse", "1 thông báo của tài khoản đang đăng nhập (notification-service).", [
        ("id", "number", ""),
        ("loai", "enum LoaiThongBao", ""),
        ("noiDung", "string", "Câu thông báo tiếng Việt đã soạn sẵn; hiển thị nguyên văn."),
        ("daDoc", "boolean", ""),
        ("ngayTao", "datetime", ""),
        ("idLichHen", "number", "Lịch hẹn liên quan; `null` nếu thông báo không gắn với lịch hẹn. Bệnh nhân không dùng "
                                "trường này."),
        ("maPhieuKham", "string", "Chỉ có khi người đọc là bệnh nhân: mã phiếu khám của lịch hẹn liên quan, để mở chi "
                                  "tiết lịch hẹn; `null` với vai trò khác."),
        ("idYeuCau", "number", "Dành cho bác sĩ / quản trị viên; `null` với bệnh nhân."),
    ], ()),
    ("SoChuaDocResponse", "", [
        ("soChuaDoc", "number", "Số thông báo chưa đọc; số trên biểu tượng chuông."),
    ], ()),
    ("DanhDauDaDocResponse", "", [
        ("soDaDanhDau", "number", "Số thông báo vừa chuyển từ chưa đọc sang đã đọc."),
    ], ()),
]


def quy_uoc_moi():
    """Các mục 3.8-3.11, chèn trước mục 4."""
    x = kit.tieu_de(2, "3.8 Service và tiền tố đường dẫn")
    x += kit.doan("Frontend chỉ gọi api-gateway (`http://localhost:8080`). Gateway chọn service theo tiền tố đường dẫn và "
                  "**giữ nguyên đường dẫn** khi chuyển tiếp.")
    x += kit.bang((3000, 2400, 900, 3338), ("Tiền tố", "Service", "Cổng", "Swagger UI"), [
        ("`/api/auth/**`, `/api/users/**`, `/api/identity/**`", "identity-service", "8081",
         "`http://localhost:8081/swagger-ui.html`"),
        ("`/api/catalog/**`", "catalog-service", "8082", "`http://localhost:8082/swagger-ui.html`"),
        ("`/api/booking/**`", "booking-service", "8084", "`http://localhost:8084/swagger-ui.html`"),
        ("`/api/notification/**`", "notification-service", "8086", "`http://localhost:8086/swagger-ui.html`"),
    ], cot_giua=(2,)) + kit.khoang_trong(60)
    x += kit.doan("Swagger UI của catalog-service và booking-service liệt kê cả route của bác sĩ và quản trị viên; tài "
                  "liệu này chỉ mô tả các route phía bệnh nhân.")

    x += kit.tieu_de(2, "3.9 Khách và bệnh nhân đã đăng nhập")
    x += kit.doan("Đặt lịch khám không bắt buộc đăng nhập. Các route sau là **công khai** (không cần access token):")
    x += "".join(kit.cham(d) for d in [
        "`GET /api/catalog/chuyen-khoa/**`, `GET /api/catalog/bac-si/**`: xem chuyên khoa và bác sĩ.",
        "`GET /api/booking/khung-gio/**`: xem khung giờ còn chỗ.",
        "`POST /api/booking/lich-hen`: đặt lịch.",
        "`GET /api/booking/phieu-kham/**` và `POST /api/booking/phieu-kham/**`: xem phiếu khám, mã QR, hủy và đổi lịch "
        "bằng link phiếu khám kèm số điện thoại liên hệ.",
    ])
    x += kit.doan("Mọi route còn lại của booking-service trong tài liệu này (đường dẫn có `/cua-toi`) cần access token của "
                  "vai trò `BENH_NHAN`; bác sĩ và quản trị viên gọi sẽ nhận 403 `KHONG_CO_QUYEN`. Các route thông báo "
                  "(`/api/notification/**`) dùng cho mọi vai trò đã đăng nhập.")
    x += kit.nhan("ĐẶT LỊCH KHI CÓ VÀ KHÔNG CÓ TOKEN")
    x += kit.bang((2900, 6738), ("Request", "Kết quả"), [
        ("Không có header `Authorization`", "Đặt như khách. `luuVaoTaiKhoan` = `false`; lịch chỉ xem lại được bằng link "
                                            "phiếu khám. Hủy / đổi bằng `maPhieuKham` kèm số điện thoại liên hệ."),
        ("Token của `BENH_NHAN`", "Lịch lưu vào tài khoản (`luuVaoTaiKhoan` = `true`), có trong \"lịch hẹn của tôi\". "
                                  "`datChoBanThan` và `luuNguoiThan` của request chỉ có nghĩa ở trường hợp này."),
        ("Token của `BAC_SI` / `QUAN_TRI_VIEN`", "403 `KHONG_CO_QUYEN`: các vai trò này không đặt lịch khám; đăng xuất để "
                                                 "đặt như khách."),
        ("Token hết hạn hoặc sai", "401 `CHUA_DANG_NHAP`, **kể cả ở route công khai**: service kiểm tra mọi token được "
                                   "gửi lên trước khi xét route có công khai hay không. Làm mới token trước, hoặc "
                                   "không gửi header `Authorization` khi gọi route công khai."),
    ]) + kit.khoang_trong(60)
    x += kit.nhan("LỊCH ĐÃ ĐẶT NHƯ KHÁCH TỰ VÀO TÀI KHOẢN")
    x += kit.doan("Frontend không phải gọi API riêng. Mỗi lần bệnh nhân đã đăng nhập gọi một trong các route đọc dưới đây, "
                  "nếu tài khoản chưa có hồ sơ bệnh nhân thì service tìm hồ sơ có số CCCD đã khai lúc đăng ký (`cccd` ở "
                  "5.1): `GET /api/booking/lich-hen/cua-toi`, `GET /api/booking/lich-hen/cua-toi/lich`, "
                  "`GET /api/booking/thong-tin-dat-lich/cua-toi`, `GET /api/booking/ho-so-benh-nhan/cua-toi`, "
                  "`GET /api/booking/trang-ca-nhan/cua-toi`, `GET /api/booking/lich-su-kham/cua-toi`.")
    x += kit.bang((3600, 6038), ("Tình huống", "Kết quả"), [
        ("Tìm thấy hồ sơ; họ tên và số điện thoại của tài khoản khớp hồ sơ",
         "Hồ sơ gắn vào tài khoản, `trangThaiLienKet` = `DA_LIEN_KET`. Các lịch đã đặt như khách bằng số CCCD đó hiện "
         "trong \"lịch hẹn của tôi\"."),
        ("Tìm thấy hồ sơ nhưng không khớp",
         "`trangThaiLienKet` = `CHO_XAC_MINH`: tài khoản chưa thấy gì của hồ sơ cho tới khi quản trị viên duyệt. Hiện "
         "\"hồ sơ đang chờ phòng khám xác minh\"; chưa đặt \"cho bản thân\" được (409 `HO_SO_CHO_XAC_MINH`), vẫn đặt cho "
         "người thân được."),
        ("Không tìm thấy, hoặc tài khoản không khai CCCD (vd đăng nhập Google)",
         "Không có gì thay đổi. Bệnh nhân nhập CCCD ở form hồ sơ bệnh nhân (`PUT /api/booking/ho-so-benh-nhan/cua-toi`), "
         "hoặc hồ sơ được tạo khi đặt lịch \"cho bản thân\" lần đầu."),
    ]) + kit.khoang_trong(60)

    x += kit.tieu_de(2, "3.10 Phiếu khám: mã, liên kết, che thông tin")
    x += kit.bang((2000, 7638), ("Giá trị", "Ý nghĩa"), [
        ("`maPhieuKham`", "Chuỗi ngẫu nhiên 43 ký tự (chữ, số, `-`, `_`). Là **khóa duy nhất** của lịch hẹn phía bệnh "
                          "nhân: nằm trên đường dẫn của mọi route xem, hủy, đổi, QR, đánh giá. Ai có mã này đều xem "
                          "được phiếu khám, nên không đưa mã vào log hay công cụ thống kê của frontend."),
        ("`maTraCuu`", "Mã ngắn dạng `ECL-<yyyyMMdd>-<4 chữ số>` (vd `ECL-20261005-4198`), in trên phiếu để đọc tại "
                       "quầy. Chỉ để hiển thị; không dùng gọi API được."),
        ("`linkPhieuKham`", "`<FRONTEND_URL>/phieu-kham/<maPhieuKham>` (mặc định `http://localhost:5173/phieu-kham/...`). "
                            "Frontend phải có trang ở đường dẫn `/phieu-kham/:maPhieuKham`, đọc mã từ URL rồi gọi "
                            "`GET /api/booking/phieu-kham/{maPhieuKham}`. Mã QR chỉ chứa link này, không chứa thông tin "
                            "bệnh nhân."),
        ("Id lịch hẹn", "Không API phía bệnh nhân nào trả hay nhận id số của lịch hẹn."),
        ("Che thông tin", "Trên phiếu khám công khai: CCCD chỉ còn 3 số cuối (`*********123`), số điện thoại còn 3 số "
                          "đầu và 3 số cuối (`090****567`), ngày sinh chỉ còn năm. Các route `/cua-toi` trả số của chính "
                          "chủ tài khoản thì không che."),
    ]) + kit.khoang_trong(60)

    x += kit.tieu_de(2, "3.11 Vòng đời lịch hẹn, hủy và đổi lịch")
    x += kit.bang((2400, 4000, 3238), ("trangThai", "Khi nào", "Bệnh nhân còn làm được"), [
        ("`CHO_XAC_NHAN`", "Vừa đặt hoặc vừa đổi lịch; chờ bác sĩ xác nhận.", "Hủy, đổi lịch (trước hạn)."),
        ("`DA_XAC_NHAN`", "Bác sĩ đã xác nhận.", "Hủy, đổi lịch (trước hạn)."),
        ("`BI_TU_CHOI`", "Bác sĩ từ chối; lý do ở `lyDoHuy`.", "Không; đặt lịch mới."),
        ("`DA_HOAN_THANH`", "Bác sĩ đã ghi kết quả khám.", "Xem kết quả, gửi / sửa đánh giá."),
        ("`DA_HUY`", "Lịch đã hủy; lý do (nếu có) ở `lyDoHuy`.", "Không."),
        ("`DA_HUY_DO_DOI_LICH`", "Lịch cũ sau khi đổi lịch; lịch mới là 1 lịch khác với phiếu khám mới.", "Không."),
    ]) + kit.khoang_trong(60)
    x += "".join(kit.cham(d) for d in [
        "**Chọn khung giờ** (đặt lịch và đổi lịch): gửi đúng một trong hai: `idLichLamViec` + `gioBatDauKhung` (bác sĩ cụ "
        "thể; lấy từ `CaKhamResponse`) hoặc `idChuyenKhoa` + `gioBatDauKhung` (bác sĩ bất kỳ; lấy từ "
        "`KhungGioGopResponse`, hệ thống xếp bác sĩ còn nhiều chỗ nhất). Gửi cả hai hoặc không gửi → 400.",
        "**Hạn hủy / đổi**: tới `hanHuyDoi` = giờ khám dự kiến trừ 2 giờ. Quá hạn → 409 `QUA_HAN_HUY_DOI_LICH`. Dùng "
        "`duocHuyDoi` để ẩn / hiện nút, không tự tính lại ở frontend.",
        "**Đổi lịch = hủy lịch cũ + tạo lịch mới**: lịch cũ thành `DA_HUY_DO_DOI_LICH`, response là `DatLichResponse` của "
        "lịch mới (`CHO_XAC_NHAN`) với `maPhieuKham` **mới**. Sau khi đổi, chuyển sang phiếu khám mới; link cũ vẫn mở "
        "được và hiện trạng thái đã đổi.",
        "**Số lần đổi**: tối đa 2 lần cho 1 lần đặt (đếm qua cả chuỗi lịch cũ). Lần thứ 3 → 409 `VUOT_SO_LAN_DOI_LICH`.",
        "**canDoiLich = true**: phòng khám đã hủy ca khám, lịch hẹn còn hiệu lực và chờ bệnh nhân đổi sang khung giờ "
        "khác (hoặc hủy). Khi đó hủy / đổi được tới giờ khám cũ và lần đổi này không tính vào số lần đổi. Bệnh nhân có "
        "tài khoản nhận thông báo `LICH_HEN_CAN_DOI`.",
        "**Lịch đã qua giờ**: lịch `CHO_XAC_NHAN` / `DA_XAC_NHAN` mà khung giờ đã kết thúc vẫn giữ nguyên `trangThai` "
        "nhưng thuộc bộ lọc `LICH_SU`, không còn là `SAP_TOI`.",
        "**Ai hủy / đổi được**: trên các route `/cua-toi`, tài khoản là người đặt hoặc là người khám (`laBanThan`). "
        "Trên các route `/phieu-kham`, bất kỳ ai gửi đúng số điện thoại liên hệ đã nhập lúc đặt (sai → 403 "
        "`SO_DIEN_THOAI_KHONG_KHOP`).",
    ])
    return x + kit.khoang_trong(100)


def sua(tl):
    # --- Trang đầu ---
    tl.thay_dau_trang("eClinic — Tài liệu API: Xác thực và Tài khoản", "eClinic — Tài liệu API: Tài khoản và Đặt lịch khám")
    tl.thay_chu("Xác thực và Quản lý tài khoản (identity-service)", "Tài khoản và Đặt lịch khám phía bệnh nhân")
    tl.thay_chu("26 route của identity-service: xác thực (AUTH), hồ sơ cá nhân, đổi mật khẩu, ảnh đại diện, đổi email, "
                "thiết bị đăng nhập, quản trị tài khoản",
                "Tài khoản (identity-service, 26 route, mọi vai trò): xác thực, hồ sơ cá nhân, đổi mật khẩu, ảnh đại diện, "
                "đổi email, thiết bị đăng nhập, quản trị tài khoản. Đặt lịch khám phía bệnh nhân (catalog-service, "
                "booking-service, notification-service): xem chuyên khoa, bác sĩ, khung giờ; đặt, xem, hủy, đổi lịch; "
                "phiếu khám; hồ sơ bệnh nhân; lịch sử khám; đánh giá; thông báo")
    tl.thay_noi_dung(" (api-gateway). Gọi thẳng identity-service: ",
                     "`http://localhost:8080` (api-gateway). Gọi thẳng từng service: identity `8081`, catalog `8082`, "
                     "booking `8084`, notification `8086` (xem 3.8)", trong_bang=True)

    # --- Mục 1 ---
    tl.thay_doan("Tài liệu mô tả route, request, response và mã lỗi của hai nhóm chức năng đã hoàn thiện trong eClinic "
                 "backend: ",
                 kit.doan("Tài liệu mô tả route, request, response và mã lỗi của hai nhóm chức năng trong eClinic backend "
                          "mà frontend cần để làm phần tài khoản và phần đặt lịch khám của bệnh nhân:")
                 + kit.cham("**Tài khoản** (mục 5-10): xác thực và quản lý tài khoản cho **mọi vai trò** (bệnh nhân, bác "
                            "sĩ, quản trị viên), do `identity-service` cung cấp.")
                 + kit.cham("**Đặt lịch khám phía bệnh nhân**: xem chuyên khoa, bác sĩ và khung giờ; đặt lịch (khách không "
                            "cần đăng nhập, hoặc bệnh nhân đã đăng nhập); xem, hủy, đổi lịch hẹn; phiếu khám và mã QR; hồ "
                            "sơ bệnh nhân và người thân đã lưu; lịch sử khám; đánh giá; thông báo. Do `catalog-service`, "
                            "`booking-service` và `notification-service` cung cấp."))
    tl.thay_doan("Chưa có trong bản này:", kit.doan(
        "**Không có trong tài liệu này:** các route đặt lịch của bác sĩ (danh sách lịch hẹn, xác nhận / từ chối, ca làm "
        "việc) và của quản trị viên (quản lý lịch hẹn, lịch làm việc, hồ sơ bệnh nhân, danh mục); API của "
        "`medical-service`, `chatbot-service`, `report-service`."))
    tl.thay_doan("Mỗi service có Swagger UI riêng, ví dụ ", kit.doan(
        "Frontend gọi mọi API qua `api-gateway` (cổng 8080); gateway chuyển tiếp theo tiền tố đường dẫn và **giữ nguyên "
        "đường dẫn**. Bảng tiền tố, cổng và Swagger UI của từng service ở mục 3.8. Quy ước riêng của phần đặt lịch ở "
        "mục 3.8-3.11."))

    # --- 3.1: trường null ---
    tl.thay_doan("Mọi API (trừ lỗi do gateway tự trả, xem 3.4) trả JSON theo một khung. Trường có giá trị ", kit.doan(
        "Mọi API (trừ lỗi do gateway tự trả, xem 3.4) trả JSON theo một khung. Các trường của khung (`maLoi`, "
        "`thongDiep`, `duLieu`, `chiTiet`) **không xuất hiện** khi không có giá trị. Bên trong `duLieu` thì ngược lại: "
        "trường không có giá trị vẫn có mặt với giá trị `null` (vd `\"anhDaiDien\": null`), mảng không có phần tử là "
        "`[]`. JSON mẫu trong tài liệu có thể lược bớt vài trường đang `null` cho gọn."))

    # --- 3.2: vai trò ở các route đặt lịch ---
    tl.chen_sau_doan("Vai trò:", kit.cham(
        "**Route đặt lịch:** một số route công khai, không cần token; các route có `/cua-toi` chỉ dành cho vai trò "
        "`BENH_NHAN`. Xem 3.9."))

    # --- 3.4 ---
    tl.thay_chu("identity-service chưa chạy hoặc không kết nối được.", "Service đích chưa chạy hoặc không kết nối được.")

    # --- 3.5: quy tắc dữ liệu dùng chung của phần đặt lịch ---
    for dong in [
        ("Số CCCD", "Đúng 12 chữ số. Bắt buộc với người khám từ đủ 18 tuổi (tính theo **ngày khám**); người dưới 18 tuổi "
                    "chưa có CCCD thì bỏ trống. Đã lưu vào hồ sơ bệnh nhân thì không đổi được."),
        ("Ngày sinh", "Dạng `2001-05-20`, không ở tương lai."),
        ("Người giám hộ", "Bắt buộc khi người khám dưới 18 tuổi tính theo ngày khám (thiếu → 400 `THIEU_NGUOI_GIAM_HO`). "
                          "Gồm họ tên, quan hệ (`QuanHeGiamHo`), số điện thoại, CCCD 12 chữ số khác CCCD người khám; "
                          "ngày sinh không bắt buộc, có gửi thì phải từ đủ 18 tuổi (sai → 400 "
                          "`NGUOI_GIAM_HO_KHONG_HOP_LE`). Người khám từ đủ 18 tuổi thì phần người giám hộ gửi kèm bị "
                          "bỏ qua."),
        ("Chuỗi tự do", "Lý do khám: bắt buộc, tối đa 500 ký tự. Lý do hủy: không bắt buộc, tối đa 500. Địa chỉ: tối đa "
                        "500. Số thẻ bảo hiểm y tế: tối đa 50. Tiền sử bệnh lý: tối đa 5000. Nhận xét đánh giá: tối đa "
                        "1000."),
        ("Trường không bắt buộc", "Ở form đặt lịch: không gửi, gửi `null` hoặc gửi chuỗi rỗng đều được. Ở các route sửa "
                                  "(`PUT`) của hồ sơ bệnh nhân và người thân đã lưu: gửi trống nghĩa là **xoá trắng** "
                                  "trường đó."),
        ("Giờ", "Dạng `08:00:00` (kiểu `time`, giờ của ca làm việc)."),
    ]:
        tl.them_dong("3.5 Quy tắc dữ liệu đầu vào", None, kit.dong_bang(RONG_QUY_TAC, dong))

    # --- 3.6: giới hạn của phần đặt lịch ---
    for dong in [
        ("Đặt lịch trước giờ khám", "Ít nhất 30 phút trước giờ bắt đầu lượt khám và trong 30 ngày tới (tính cả hôm nay); "
                                    "ngoài khoảng này khung giờ không hiện, đặt thẳng → 409 `KHUNG_GIO_KHONG_KHA_DUNG`"),
        ("Số lịch sắp tới còn hiệu lực", "Tối đa 3 cho 1 hồ sơ bệnh nhân, 5 cho 1 số điện thoại liên hệ (409 "
                                         "`VUOT_GIOI_HAN_DAT_LICH`)"),
        ("Số lần gọi đặt lịch, đổi lịch, hủy bằng link phiếu khám từ 1 địa chỉ IP (đếm chung)",
         "20 lần / 10 phút (429 `GUI_LAI_QUA_NHANH`)"),
        ("Hủy / đổi lịch", "Tới 2 giờ trước giờ khám dự kiến (409 `QUA_HAN_HUY_DOI_LICH`)"),
        ("Số lần đổi lịch của 1 lần đặt", "2 lần (409 `VUOT_SO_LAN_DOI_LICH`)"),
        ("Xem khung giờ nhiều ngày của 1 bác sĩ", "Tối đa 7 ngày liền mỗi lần gọi"),
        ("Lịch hẹn của tôi theo khoảng ngày (màn hình lịch)", "Tối đa 42 ngày mỗi lần gọi"),
        ("Người thân đã lưu", "Tối đa 10 người mỗi tài khoản (giữ 10 người đặt gần nhất)"),
        ("Sửa đánh giá đã gửi", "Trong 7 ngày kể từ lúc gửi (409 `HET_HAN_SUA_DANH_GIA`)"),
        ("Ảnh mã QR phiếu khám", "Cạnh 100-1000 pixel, mặc định 300"),
        ("Kích thước trang của các route danh sách", "`kichThuoc` từ 1 đến 100, mặc định 20"),
        ("Hỏi số thông báo chưa đọc", "Không có đẩy realtime: frontend gọi lại khoảng 30 giây một lần, tạm dừng khi tab "
                                      "bị ẩn"),
    ]:
        tl.them_dong("3.6 Giới hạn và thời hạn", None, kit.dong_bang(RONG_GIOI_HAN, dong))

    # --- 3.7: enum của phần đặt lịch ---
    for dong in [
        ("`TrangThaiLichHen`", "`CHO_XAC_NHAN`, `DA_XAC_NHAN`, `BI_TU_CHOI`, `DA_HOAN_THANH`, `DA_HUY`, "
                               "`DA_HUY_DO_DOI_LICH`. Ý nghĩa từng giá trị ở 3.11."),
        ("`LocLichHen`", "Bộ lọc \"lịch hẹn của tôi\": `TAT_CA`, `SAP_TOI` (lịch `CHO_XAC_NHAN` / `DA_XAC_NHAN` mà khung "
                         "giờ chưa kết thúc), `LICH_SU` (mọi lịch còn lại: đã khám, đã hủy, bị từ chối, đã qua giờ)"),
        ("`PhamViLichHen`", "Tham số `cuaAi`: `TAT_CA`, `BAN_THAN` (người khám là chủ tài khoản, bất kể ai đặt), "
                            "`NGUOI_KHAC` (lịch đặt cho người thân, hoặc chủ tài khoản là người giám hộ)"),
        ("`NguoiDatLich`", "`TOI` (chính tài khoản đang xem), `KHACH` (đặt khi không đăng nhập), `TAI_KHOAN_KHAC`"),
        ("`QuanHeGiamHo`", "`CHA`, `ME`, `NGUOI_GIAM_HO_HOP_PHAP`, `KHAC`"),
        ("`LoaiAnhBacSi`", "`ANH_CONG_VIEC`, `CHUNG_CHI`"),
        ("`LoaiThongBao`", "Gửi cho bệnh nhân: `LICH_HEN_DA_XAC_NHAN`, `LICH_HEN_BI_TU_CHOI`, `LICH_HEN_CAN_DOI` (ca khám "
                           "bị hủy, cần đổi lịch), `LICH_HEN_DOI_PHONG` (giữ nguyên giờ, đổi phòng khám). Chỉ gửi cho "
                           "bác sĩ / quản trị viên: `LICH_HEN_MOI`, `LICH_HEN_DA_DOI`, `LICH_HEN_DA_HUY`, "
                           "`KET_QUA_DUYET_DOI_LICH`, `CA_LAM_VIEC_THAY_DOI`, `YEU_CAU_DOI_LICH_MOI`. Đã khai báo "
                           "nhưng bản hiện tại chưa gửi: `NHAC_LICH_KHAM`, `HE_THONG`; frontend nên hiển thị được mọi "
                           "giá trị bằng `noiDung`"),
    ]:
        tl.them_dong("3.7 Giá trị enum", None, kit.dong_bang(RONG_ENUM, dong))

    # --- 3.8-3.11 ---
    tl.chen_truoc_doan(MUC_4, quy_uoc_moi())

    # --- Mục 4: lời dẫn, chữ "vắng" (trường trong duLieu là null chứ không vắng mặt) ---
    tl.thay_doan("Các đối tượng dưới đây nằm trong ", kit.doan(
        "Các đối tượng dưới đây nằm trong `duLieu`. Kiểu `datetime` là `LocalDateTime` (ISO-8601, vd "
        "`2026-10-07T08:00:00`), `date` là ngày không giờ (`2026-10-07`), `time` là giờ không ngày (`08:00:00`); kiểu "
        "`X[]` là mảng. Trường không có giá trị mang `null` (xem 3.1); mô tả \"chỉ có khi...\" nghĩa là ở các trường hợp "
        "khác trường đó là `null`. Mục 4.1-4.9 thuộc phần tài khoản, từ 4.10 thuộc phần đặt lịch."))
    tl.thay_noi_dung("Số điện thoại (có thể vắng với tài khoản Google).",
                     "Số điện thoại (`null` với tài khoản Google chưa khai số).", True)
    tl.thay_noi_dung("URL ảnh (Cloudinary hoặc Google); vắng nếu chưa có.",
                     "URL ảnh (Cloudinary hoặc Google); `null` nếu chưa có.", True)
    tl.thay_noi_dung("mọi trường khác đều vắng",
                     "Khi `trangThaiLienKet = CHO_XAC_MINH` thì **mọi trường khác đều là** `null`: hồ sơ chưa được xác "
                     "minh thì chưa được xem thông tin. Bệnh nhân xem, tạo và sửa hồ sơ này bằng các route hồ sơ bệnh "
                     "nhân của booking-service (`HoSoCuaToiResponse`).")
    tl.thay_noi_dung("User-Agent lúc đăng nhập (có thể vắng).", "User-Agent lúc đăng nhập (có thể `null`).", True)
    tl.thay_noi_dung("); vắng nếu không có.",
                     "Từ hồ sơ bệnh nhân đã liên kết (`DA_LIEN_KET`); `null` nếu không có.", True)
    tl.thay_noi_dung("Trạng thái hồ sơ bệnh nhân gắn với tài khoản; vắng nếu chưa gắn.",
                     "Trạng thái hồ sơ bệnh nhân gắn với tài khoản; `null` nếu chưa gắn.", True)

    # --- 6.1, 9.1: cùng lỗi "vắng" ở phần route; JSON mẫu 6.1 thiếu các trường null ---
    tl.thay_noi_dung("; mọi trường khác vắng mặt.",
                     "Khi hồ sơ bệnh nhân đang `CHO_XAC_MINH`, `hoSoBenhNhan` chỉ có `trangThaiLienKet`; mọi trường khác "
                     "là `null`.")
    tl.thay_noi_dung(" là nguyên chuỗi User-Agent lúc đăng nhập (có thể vắng); frontend tự rút gọn thành tên trình duyệt "
                     "/ thiết bị.",
                     "`thietBi` là nguyên chuỗi User-Agent lúc đăng nhập (có thể `null`); frontend tự rút gọn thành tên "
                     "trình duyệt / thiết bị.")
    tl.chen_sau_doan('    "ngayTao": "2026-10-03T08:40:00",', kit.dong_code('    "bacSi": null,'))
    tl.thay_chu('      "diaChi": "Củ Chi, TP. Hồ Chí Minh"', '      "diaChi": "Củ Chi, TP. Hồ Chí Minh",')
    tl.chen_sau_doan('      "diaChi": "Củ Chi, TP. Hồ Chí Minh",',
                     kit.dong_code('      "soBaoHiemYTe": null,') + kit.dong_code('      "tienSuBenhLy": null'))

    # --- Mô hình 4.10 trở đi, chèn trước mục 5 ---
    tl.chen_truoc_doan("5. Xác thực (AUTH)", "".join(
        mo_hinh(10 + i, ten, mo_ta, dong, con) for i, (ten, mo_ta, dong, con) in enumerate(MO_HINH)))
