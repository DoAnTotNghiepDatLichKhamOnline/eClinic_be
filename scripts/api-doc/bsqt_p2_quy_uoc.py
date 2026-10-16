"""Tài liệu 2, giai đoạn 2: mục 3, quy ước riêng của phía bác sĩ và quản trị viên. Chỉ ghi những gì TK chưa có; phần
dùng chung thì dẫn mục của TK (3.1). Chạy qua bsqt_build.py.

Nguồn của từng mục: 3.2 api-gateway application.yml, GuiThongBaoServiceImpl, NhacTaiKhamServiceImpl; 3.3 các controller,
LichHenServiceImpl.timTheoMa; 3.4 BacSiHuyLichHenServiceImpl, KhoangNgay; 3.5 HangDoiServiceImpl, LichHen.hangDoiHienThi;
3.6 DatLichServiceImpl, HoSoKhamServiceImpl, SuaHoSoBenhNhanServiceImpl; 3.7 KhamBenhServiceImpl,
LichSuSuaBenhAnServiceImpl, BenhAnProperties; 3.8 SinhHieuRequest, DonThuocRequest, BenhAnRequest, ThuocServiceImpl,
Icd10ServiceImpl; 3.9 common/.../enums.
"""

import kit
from bsqt_p2_khung import them_cuoi

RONG_2 = (2500, 7138)
RONG_SERVICE = (2700, 2500, 4438)
RONG_DOC_O_TK = (5338, 4300)


def bang(rong, cot, dong):
    return kit.bang(rong, cot, dong) + kit.khoang_trong(60)


def muc_3_1():
    x = kit.tieu_de(2, "3.1 Quy ước dùng chung với TK")
    x += kit.doan("Các quy ước dưới đây áp dụng nguyên văn cho mọi route của tài liệu này. Đọc ở TK, không nhắc lại.")
    x += bang(RONG_DOC_O_TK, ("Nội dung", "Đọc ở"), [
        ("Khung phản hồi `thanhCong`, `maLoi`, `thongDiep`, `duLieu`, `chiTiet`; trường không có giá trị là `null`",
         "TK 3.1"),
        ("Access token, header `Authorization`, làm mới phiên khi nhận 401",
         "TK 3.2. Đăng nhập: TK 5.4; bác sĩ đăng nhập lần đầu bằng mật khẩu mặc định: TK 5.11"),
        ("Lỗi chung (400 `DU_LIEU_KHONG_HOP_LE`, 401, 403, 404, 409, 500) và lỗi do api-gateway tự trả",
         "TK 3.4. Mục chi tiết của từng route chỉ ghi lỗi riêng của route"),
        ("Ngày giờ ISO-8601 không kèm offset, số điện thoại, họ tên, CCCD", "TK 3.5"),
        ("Enum `VaiTro`, `GioiTinh`, `TrangThaiLichHen`, `QuanHeGiamHo`, `DuongDungThuoc`",
         "TK 3.7. Enum chỉ có ở tài liệu này: 3.9"),
        ("Mã tra cứu, mã phiếu khám, link phiếu khám", "TK 3.10. Cách tra cứu bằng các mã này: 3.3"),
        ("Ý nghĩa từng trạng thái lịch hẹn, `canDoiLich`, lịch thành `KHONG_DEN`",
         "TK 3.11. Bác sĩ làm được gì ở từng trạng thái: 3.4"),
        ("Câu chữ theo `Accept-Language`; chẩn đoán, lý do, tên thuốc, họ tên không dịch", "TK 3.12"),
        ("Email cho bệnh nhân khi bác sĩ hủy lịch và khi bác sĩ hẹn tái khám", "TK 3.13"),
        ("Mô hình `BacSiTomTatResponse`, `PhongKhamTomTatResponse`", "TK 4.14, TK 4.15"),
        ("Mô hình `KetQuaKhamResponse` (kết quả khám, kèm `SinhHieu`, `IcdChinh`, `DonThuoc`)",
         "TK 4.27. Phần khác khi bác sĩ xem: 4.4"),
        ("Thông điệp tiếng Việt của mã lỗi", "TK Phụ lục A"),
        ("Tài khoản bác sĩ, quản trị viên mẫu; dữ liệu mẫu (ca làm việc, bệnh nhân); biến môi trường",
         "TK Phụ lục C"),
    ])
    x += kit.doan("Không route nào trong tài liệu này phân trang (TK 3.3 không áp dụng): danh sách trả mảng đầy đủ, "
                  "`[]` khi không có phần tử nào.")
    return x


def muc_3_2():
    x = kit.tieu_de(2, "3.2 Service cần chạy")
    x += kit.doan("Frontend chỉ gọi api-gateway (`http://localhost:8080`); gateway chọn service theo tiền tố đường dẫn "
                  "và giữ nguyên đường dẫn (TK 3.8). Tài liệu này dùng 2 tiền tố: `/api/booking/**` của booking-service "
                  "(cổng, Swagger UI ở TK 3.8) và `/api/medical/**` của medical-service, TK 3.8 chưa có: cổng 8085, "
                  "Swagger UI `http://localhost:8085/swagger-ui.html`.")
    x += kit.doan("Mỗi route chỉ trả lời khi các service ở dòng tương ứng đang chạy:")
    x += bang(RONG_SERVICE, ("Nhóm route", "Service phải chạy", "Ghi chú"), [
        ("Mọi route", "mysql, redis, api-gateway, identity-service",
         "identity-service cấp access token khi đăng nhập (TK 5.4). Sau đó booking-service và medical-service tự "
         "kiểm token, không gọi identity-service ở mỗi request."),
        ("Ca làm việc, lịch hẹn, tra cứu, hủy lịch, hàng đợi, hồ sơ khám, đối chiếu giấy tờ (`/api/booking/**`)",
         "thêm booking-service", "Hủy lịch còn gửi thông báo và email cho bệnh nhân: dòng cuối."),
        ("Hồ sơ bệnh án, lịch sử sửa, gợi ý thuốc và ICD-10 (`/api/medical/**`)", "thêm medical-service",
         "medical-service đọc lịch hẹn thẳng trong CSDL, không gọi booking-service: tắt booking-service thì các route "
         "này vẫn chạy."),
        ("Thông báo và email cho bệnh nhân khi bác sĩ hủy lịch; email hẹn tái khám sau khi ghi / sửa "
         "`ngayTaiKhamDeXuat`", "notification-service (không bắt buộc), booking-service",
         "Route vẫn thành công khi notification-service tắt. booking-service ghi việc cần gửi vào hàng chờ trong CSDL "
         "rồi chuyển sang notification-service theo chu kỳ (TK Phụ lục C), nên thông báo và email được gửi khi "
         "service chạy lại, không mất. Email hẹn tái khám do booking-service tìm định kỳ trong hồ sơ bệnh án (TK 3.13)."),
    ])
    x += kit.doan("Service cần cho route chưa chạy: api-gateway trả 503 `DICH_VU_KHONG_KHA_DUNG` (TK 3.4). Cách chạy "
                  "các service (Docker Compose, hoặc từng service trong IntelliJ): README.md của mã nguồn backend.")
    return x


def muc_3_3():
    x = kit.tieu_de(2, "3.3 Vai trò, quyền và id lịch hẹn")
    x += bang(RONG_2, ("Việc", "Quy tắc"), [
        ("Route `/bac-si/toi/**` (booking và medical)",
         "Chỉ vai trò `BAC_SI`; vai trò khác nhận 403 `KHONG_CO_QUYEN`. Bác sĩ lấy từ access token, request không gửi "
         "id bác sĩ. Mọi thao tác chỉ trên lịch hẹn của chính bác sĩ đó: id lịch hẹn không có và id lịch hẹn của bác sĩ "
         "khác cùng trả 404 `KHONG_TIM_THAY`."),
        ("Route `/quan-tri/**` (booking và medical)",
         "Chỉ vai trò `QUAN_TRI_VIEN`; vai trò khác nhận 403 `KHONG_CO_QUYEN`. Quản trị viên thấy mọi lịch hẹn."),
        ("Id lịch hẹn",
         "Khác phía bệnh nhân (TK 3.10): các route ở đây dùng id số của lịch hẹn trên đường dẫn: `{id}` ở "
         "booking-service, `{idLichHen}` ở medical-service, là cùng 1 id (`id` của `LichHenTrongCaResponse`, 4.2)."),
        ("Tham số `ma` của route tra cứu",
         "Nhận 1 trong 3 dạng: mã tra cứu in trên phiếu `ECL-<yyyyMMdd>-<số>` (không phân biệt hoa thường); mã phiếu "
         "khám 43 ký tự (phân biệt hoa thường); hoặc nguyên link phiếu khám mà máy quét QR đọc ra (lấy đoạn cuối của "
         "đường dẫn, bỏ phần `?...` và `#...`). Khoảng trắng hai đầu được bỏ. Dạng khác, không có lịch hẹn, hoặc "
         "lịch hẹn của bác sĩ khác: 404 `KHONG_TIM_THAY`."),
    ])
    return x


def muc_3_4():
    x = kit.tieu_de(2, "3.4 Lịch hẹn phía bác sĩ")
    x += kit.doan("Ý nghĩa từng `trangThai` ở TK 3.11. Danh sách lịch hẹn của bác sĩ có mọi trạng thái, kể cả lịch đã "
                  "hủy và lịch cũ sau khi đổi. Bác sĩ làm được:")
    x += bang(RONG_2, ("trangThai", "Bác sĩ làm được"), [
        ("`DA_XAC_NHAN`",
         "Hủy kèm lý do, trước giờ khám (dưới đây). Trong ngày khám: gọi vào khám, đánh dấu vắng mặt (3.5). Từ ngày "
         "khám trở đi: ghi hồ sơ bệnh án (3.7). Lịch có `canDoiLich` = `true` không hủy, không gọi khám được."),
        ("`DA_HOAN_THANH`", "Xem và sửa hồ sơ bệnh án trong hạn sửa; xem lịch sử sửa (3.7)."),
        ("`BI_TU_CHOI`, `DA_HUY`, `DA_HUY_DO_DOI_LICH`, `KHONG_DEN`",
         "Chỉ xem. `lyDoHuy` có ở `BI_TU_CHOI` (lý do bác sĩ ghi) và ở `DA_HUY` khi bệnh nhân có ghi lý do."),
        ("Mọi trạng thái", "Mở hồ sơ khám, sửa thông tin hành chính của bệnh nhân, đánh dấu đã đối chiếu giấy tờ (3.6)."),
    ])
    x += kit.nhan("BÁC SĨ HỦY LỊCH")
    x += kit.cham("Chỉ hủy được lịch `DA_XAC_NHAN` (khác: 409 `LICH_HEN_KHONG_HUY_DOI_DUOC`), không đang chờ đổi lịch "
                  "(`canDoiLich` = `true`: 409 `LICH_HEN_CAN_DOI_LICH`), và trước giờ khám dự kiến (đã tới giờ: 409 "
                  "`LICH_HEN_DA_QUA_GIO`). Không có hạn 2 giờ như phía bệnh nhân.")
    x += kit.cham("Lý do bắt buộc, tối đa 500 ký tự; lưu vào `lyDoHuy`, bệnh nhân thấy trên phiếu khám.")
    x += kit.cham("Lịch thành `BI_TU_CHOI` và trả lại lượt khám: người khác đặt được khung giờ đó.")
    x += kit.cham("Bệnh nhân nhận thông báo `LICH_HEN_BI_TU_CHOI` nếu có tài khoản, và email nếu có email (TK 3.13); "
                  "cần notification-service (3.2).")
    x += kit.nhan("KHOẢNG NGÀY")
    x += kit.cham("Các route xem theo khoảng ngày nhận `tuNgay`, `denNgay` (bắt buộc, dạng `2026-10-16`, tính cả hai "
                  "đầu), tối đa 42 ngày mỗi lần gọi (lưới tháng 6 tuần). `denNgay` trước `tuNgay`, hoặc khoảng dài hơn "
                  "42 ngày: 400 `DU_LIEU_KHONG_HOP_LE`.")
    x += kit.cham("Ca làm việc trong khoảng ngày có cả ca đã hủy (`trangThai` = `DA_HUY`, 3.9); lịch hẹn của ca đã hủy "
                  "vẫn hiện trong danh sách lịch hẹn với `canDoiLich` = `true` cho tới khi bệnh nhân đổi hoặc hủy.")
    return x


def muc_3_5():
    x = kit.tieu_de(2, "3.5 Hàng đợi khám")
    x += kit.doan("Hàng đợi là các lịch hẹn của bác sĩ trong 1 ngày, nhóm theo ca làm việc đang hoạt động, xếp theo số "
                  "thứ tự. Có mặt trong hàng đợi: lịch `DA_XAC_NHAN` không chờ đổi lịch, `DA_HOAN_THANH` và "
                  "`KHONG_DEN`. Mỗi lịch hẹn có 1 `trangThaiHangDoi`:")
    x += bang(RONG_2, ("trangThaiHangDoi", "Khi nào"), [
        ("`CHO_KHAM`", "Chưa được gọi (mặc định khi đặt lịch)."),
        ("`DANG_KHAM`", "Bác sĩ đã gọi vào khám, chưa ghi hồ sơ bệnh án. Mỗi bác sĩ có nhiều nhất 1 lượt `DANG_KHAM` "
                        "trong ngày, tính mọi ca."),
        ("`VANG_MAT`", "Đã đánh dấu vắng mặt; gọi lại được trong ngày. Lịch `KHONG_DEN` của ngày đã qua cũng hiện "
                       "`VANG_MAT`."),
        ("`DA_KHAM`", "Đã ghi hồ sơ bệnh án (lịch `DA_HOAN_THANH`)."),
    ])
    x += kit.cham("Gọi tiếp, gọi 1 bệnh nhân và đánh dấu vắng mặt chỉ làm được ở lịch `DA_XAC_NHAN` của hôm nay: "
                  "ngày khác thì 409 `KHONG_PHAI_NGAY_KHAM`; lịch không còn khám được hoặc đang chờ đổi lịch thì 409 "
                  "`LICH_HEN_KHONG_KHAM_DUOC`. Xem hàng đợi thì được với mọi ngày.")
    x += kit.cham("Gọi tiếp: gọi lượt `CHO_KHAM` có số thứ tự nhỏ nhất của ca. Không còn lượt `CHO_KHAM`: 409 "
                  "`HANG_DOI_TRONG`. Bệnh nhân vắng mặt không được gọi lại tự động: bác sĩ gọi lại đúng người đó.")
    x += kit.cham("Đang có bệnh nhân khác `DANG_KHAM`: gọi thêm người nữa trả 409 `DANG_KHAM_BENH_NHAN_KHAC`, thông "
                  "điệp nêu họ tên và số thứ tự người đang khám. Ghi hồ sơ bệnh án của người đó, hoặc đánh dấu người "
                  "đó vắng mặt, rồi mới gọi người tiếp theo.")
    x += kit.cham("Bấm lại không đổi gì và vẫn trả 200: gọi lại người đang `DANG_KHAM` (giữ nguyên `goiLuc`), đánh dấu "
                  "vắng mặt người đã `VANG_MAT`.")
    x += kit.cham("Mỗi thao tác trả lại toàn bộ hàng đợi của hôm nay; frontend vẽ lại từ response, không tự đổi trạng "
                  "thái. Hàng đợi không tự đẩy về trình duyệt: khi màn hình đang mở, gọi lại route xem hàng đợi định kỳ "
                  "(vd 30 giây một lần).")
    return x


def muc_3_6():
    x = kit.tieu_de(2, "3.6 Đối chiếu giấy tờ và sửa thông tin bệnh nhân")
    x += kit.doan("Khi đặt lịch, người đặt nhập họ tên, ngày sinh, giới tính của người khám (và họ tên người giám hộ). "
                  "Nếu hồ sơ bệnh nhân đã có từ trước mà họ tên hoặc ngày sinh nhập vào khác hồ sơ (hoặc khác người "
                  "giám hộ đã khai), hồ sơ không bị sửa; lịch hẹn mang `doiChieu.canDoiChieu` = `true`, còn "
                  "`doiChieu` giữ những gì đã nhập (4.2).")
    x += kit.cham("Khi `canDoiChieu` = `true`, frontend hiện cảnh báo và đặt cạnh nhau `benhNhan` (hồ sơ) với "
                  "`doiChieu` (đã nhập). Bác sĩ hoặc quản trị viên xem giấy tờ, sửa hồ sơ nếu cần, rồi đánh dấu đã đối "
                  "chiếu: `canDoiChieu` thành `false`. Đánh dấu lại khi đã `false` vẫn trả 200.")
    x += kit.cham("Lịch hẹn tạo trước khi hệ thống lưu thông tin đã nhập có `doiChieu.hoTenDaNhap` = `null` (các trường "
                  "đã nhập khác cũng `null`). Đổi lịch chuyển nguyên cờ và thông tin đã nhập sang lịch mới.")
    x += kit.cham("Sửa thông tin bệnh nhân (chỉ bác sĩ của lịch hẹn) là sửa hồ sơ bệnh nhân, dùng chung cho mọi lịch "
                  "hẹn của người đó, và không tự đánh dấu đã đối chiếu. Body gửi đủ mọi trường như đang hiển thị: "
                  "`diaChi`, `soBaoHiemYTe` gửi trống là xoá trắng; `gioiTinh` gửi `null` là xoá.")
    x += kit.cham("CCCD chỉ điền được khi hồ sơ chưa có. Hồ sơ đã có CCCD: gửi lại đúng số cũ hoặc để trống thì giữ "
                  "nguyên; gửi số khác: 400 `DU_LIEU_KHONG_HOP_LE`. Số CCCD đã thuộc hồ sơ khác: 409 `XUNG_DOT_DU_LIEU`.")
    x += kit.cham("Hồ sơ đã gắn tài khoản bệnh nhân (`daLienKetTaiKhoan`, 4.4) thì chủ tài khoản cũng tự sửa được hồ sơ "
                  "đó (TK 17).")
    return x


def muc_3_7():
    x = kit.tieu_de(2, "3.7 Hồ sơ bệnh án: ghi, sửa, lịch sử sửa")
    x += bang(RONG_2, ("Việc", "Quy tắc"), [
        ("Ghi (POST, trả 201)",
         "Lịch hẹn `DA_XAC_NHAN` của chính bác sĩ (khác, kể cả lịch đã có hồ sơ bệnh án: 409 "
         "`LICH_HEN_KHONG_KHAM_DUOC`), ngày khám là hôm nay hoặc đã qua (ngày khám ở tương lai: 409 "
         "`CHUA_DEN_NGAY_KHAM`). Không cần gọi vào khám trước. Lưu xong lịch thành `DA_HOAN_THANH`, hàng đợi hiện "
         "`DA_KHAM`. Mỗi lịch hẹn có 1 hồ sơ bệnh án."),
        ("Sửa (PUT)",
         "Chỉ bác sĩ của lịch hẹn, trong hạn sửa: `hanSua` = `ngayTao` + `RECORD_EDIT_DAYS` ngày (mặc định 7). Dùng "
         "`hanSua` của response để ẩn / hiện nút sửa, không tự tính. Quá hạn: 409 `HET_HAN_SUA_BENH_AN`."),
        ("Lý do và phiên bản",
         "Mỗi lần sửa bắt buộc `lyDoSua` (tối đa 500 ký tự) và `phienBan` đang xem. Đã có người sửa trước "
         "(`phienBan` khác): 409 `DU_LIEU_DA_THAY_DOI`: tải lại hồ sơ rồi sửa lại. Nội dung gửi lên giống hệt nội "
         "dung đang lưu: 400 `DU_LIEU_KHONG_HOP_LE`."),
        ("Nội dung khi sửa",
         "Body sửa là toàn bộ hồ sơ: trường bỏ trống là xoá trắng; `donThuoc` thay toàn bộ đơn cũ (thuốc đã có trong "
         "đơn giữ lại được dù đã ngừng dùng, 3.8). Sửa thành công: `phienBan` tăng 1, `ngaySuaCuoi` là lúc sửa (bệnh "
         "nhân thấy dấu \"đã chỉnh sửa\", TK 4.27), thêm 1 lần sửa vào lịch sử."),
        ("Ngày tái khám đề xuất",
         "Không bắt buộc, không tự tạo lịch hẹn. Phải sau hôm nay; khi sửa mà giữ nguyên ngày cũ thì được dù ngày đó "
         "đã qua. Sai: 400 `DU_LIEU_KHONG_HOP_LE`. Bệnh nhân nhận email hẹn tái khám (TK 3.13)."),
        ("Xem",
         "Bác sĩ của lịch hẹn xem hồ sơ bệnh án; lịch hẹn chưa có hồ sơ: 404 `KHONG_TIM_THAY`. Kết quả khám của các "
         "lần khám trước (của mọi bác sĩ) xem trong hồ sơ khám (4.4)."),
        ("Lịch sử sửa",
         "Bác sĩ xem được lịch sử của 1 lượt khám khi lượt đó nằm trong hồ sơ khám bác sĩ mở được: lượt khám của "
         "mình, hoặc lượt khám trước đó của 1 bệnh nhân mà bác sĩ có lịch hẹn từ giờ khám của lượt đó trở đi "
         "(`lanKhamTruoc` ở 4.4). Khác: 404 `KHONG_TIM_THAY`. Quản trị viên xem được mọi lượt. Lượt chưa có hồ sơ "
         "bệnh án: 404 `KHONG_TIM_THAY`. Bệnh nhân không xem được lịch sử sửa."),
    ])
    x += kit.cham("Màn hình khám nên điền sẵn `lyDoKham` bằng lý do bệnh nhân ghi khi đặt lịch (`lyDoKham` của lịch "
                  "hẹn, 4.2); bác sĩ sửa lại nếu cần.")
    return x


def muc_3_8():
    x = kit.tieu_de(2, "3.8 Sinh hiệu, chẩn đoán và đơn thuốc")
    x += kit.doan("Giới hạn của body ghi / sửa hồ sơ bệnh án. Sai giới hạn: 400 `DU_LIEU_KHONG_HOP_LE`, trừ khi ghi "
                  "khác. Lỗi của từng trường (khoảng giá trị, độ dài, trường bắt buộc) có `chiTiet` (TK 3.1); lỗi phải "
                  "đối chiếu nhiều trường hoặc danh mục (cặp huyết áp, cân nặng của trẻ, thuốc trùng, mã ICD-10 không "
                  "có, số ngày dùng quá hạn mức) không có `chiTiet`, chỉ có `thongDiep` nói rõ chỗ sai.")
    x += bang(RONG_2, ("Dữ liệu", "Quy tắc"), [
        ("`sinhHieu`", "Không bắt buộc; từng chỉ số cũng không bắt buộc. Không gửi = không đo."),
        ("`mach`, `nhipTho`", "Số nguyên, lần/phút: mạch 20-250, nhịp thở 5-80."),
        ("`nhietDo`", "30.0-45.0 °C, tối đa 1 chữ số thập phân (`38.20` tính như `38.2`)."),
        ("`huyetApTamThu`, `huyetApTamTruong`",
         "Số nguyên, mmHg: tâm thu 50-300, tâm trương 20-200. Gửi cả hai số hoặc không số nào; tâm trương phải nhỏ hơn "
         "tâm thu."),
        ("`canNang`", "0.3-400 kg, tối đa 2 chữ số thập phân. Bắt buộc khi kê thuốc cho trẻ dưới 72 tháng tuổi tính "
                      "theo ngày khám (Thông tư 26/2025/TT-BYT)."),
        ("`chieuCao`", "20-250 cm, tối đa 1 chữ số thập phân."),
        ("Chữ tự do",
         "`chanDoan` bắt buộc, tối đa 5000 ký tự. Không bắt buộc: `lyDoKham` 2000, `khamLamSang` 5000, "
         "`chanDoanKemTheo` 2000, `ghiChu` (lời dặn) 5000. Gửi chuỗi rỗng = không ghi (lưu `null`)."),
        ("`maIcdChinh`",
         "Không bắt buộc, tối đa 10 ký tự, không phân biệt hoa thường; phải có trong danh mục ICD-10 (gợi ý: route "
         "ICD-10). Mã có `manTinh` = `true` cho kê mỗi thuốc tới 90 ngày."),
        ("`donThuoc`",
         "Không bắt buộc; tối đa 30 dòng; mỗi thuốc 1 dòng (trùng: 400). `idThuoc` lấy từ gợi ý thuốc: thuốc phải "
         "đang dùng và đã xác minh (ngừng dùng: 409 `THUOC_NGUNG_DUNG`; chưa xác minh: 409 `THUOC_CHUA_XAC_MINH`); "
         "id không có: 404 `KHONG_TIM_THAY`. Khi sửa, thuốc đã có sẵn trong đơn thì giữ lại được dù đã ngừng dùng "
         "hoặc chưa xác minh."),
        ("1 dòng thuốc",
         "`soNgayDung` bắt buộc: 1-30 ngày, hoặc 1-90 khi chẩn đoán chính có mã mạn tính (quá: 400, thông điệp nêu tên "
         "thuốc). `soLuong` bắt buộc: 1-9999, theo đơn vị của thuốc. `soLanMoiNgay` không bắt buộc, từ 1. `lieuDung` "
         "tối đa 100 ký tự, `ghiChuSuDung` tối đa 255."),
        ("Gợi ý thuốc, ICD-10",
         "Tối đa 20 kết quả; `tuKhoa` rỗng hoặc không gửi trả `[]`. Thuốc: tên thuốc hoặc hoạt chất chứa từ khoá, chỉ "
         "thuốc kê được, xếp theo tên rồi hàm lượng. ICD-10: mã bắt đầu bằng từ khoá, hoặc tên tiếng Việt / tiếng Anh "
         "chứa từ khoá, xếp theo mã."),
    ])
    return x


def muc_3_9():
    x = kit.tieu_de(2, "3.9 Enum mới")
    x += kit.doan("Các enum khác (`TrangThaiLichHen`, `GioiTinh`, `QuanHeGiamHo`, `DuongDungThuoc`...) ở TK 3.7.")
    x += bang(RONG_2, ("Enum", "Giá trị"), [
        ("TrangThaiLichLamViec", "`HOAT_DONG`, `DA_HUY` (ca bị hủy, vd khi quản trị viên duyệt đơn xin nghỉ của bác "
                                 "sĩ)."),
        ("BuoiKham", "Buổi của ca theo giờ bắt đầu: `SANG` (trước 12:00), `CHIEU` (trước 17:00), `TOI` (từ 17:00)."),
        ("TrangThaiHangDoi", "`CHO_KHAM`, `DANG_KHAM`, `VANG_MAT`, `DA_KHAM`. Ý nghĩa ở 3.5."),
        ("TruongBenhAn",
         "Phần hồ sơ bệnh án mà 1 lần sửa đã đổi: `LY_DO_KHAM`, `SINH_HIEU`, `KHAM_LAM_SANG`, `CHAN_DOAN` (chữ hoặc mã "
         "ICD-10), `CHAN_DOAN_KEM_THEO`, `GHI_CHU` (lời dặn), `NGAY_TAI_KHAM`, `DON_THUOC` (thêm, bỏ thuốc hoặc đổi "
         "liều dùng, số lần, số ngày, số lượng, ghi chú của 1 dòng; chỉ đổi thứ tự các dòng thì không tính). Nhãn "
         "hiển thị do frontend đặt."),
    ])
    return x


def sua(tl):
    them_cuoi(tl, kit.tieu_de(1, "3. Quy ước") + muc_3_1() + muc_3_2() + muc_3_3() + muc_3_4() + muc_3_5()
              + muc_3_6() + muc_3_7() + muc_3_8() + muc_3_9())
