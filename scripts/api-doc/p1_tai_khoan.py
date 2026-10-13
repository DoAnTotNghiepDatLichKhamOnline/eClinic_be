"""Bản 1.1 của tài liệu API: sửa phần xác thực và tài khoản cho khớp mã nguồn hiện tại.

Giai đoạn 1 của chuỗi dựng tài liệu; chạy qua build.py.
"""

import kit

MUC_DANG_KY = "5.1  Đăng ký tài khoản bệnh nhân"
MUC_DANG_NHAP = "5.4  Đăng nhập bằng email và mật khẩu"
PHU_LUC_C = "Phụ lục C. Cấu hình ảnh hưởng tới API và tài khoản mẫu"


def sua(tl):
    # --- Trang đầu và mục 1 ---
    tl.thay_chu("1.0 — 03/10/2026", "1.1 — 09/10/2026")
    tl.thay_chu("25 route của identity-service: xác thực (AUTH), hồ sơ cá nhân, đổi mật khẩu, ảnh đại diện, đổi email, "
                "thiết bị đăng nhập, quản trị tài khoản",
                "26 route của identity-service: xác thực (AUTH), hồ sơ cá nhân, đổi mật khẩu, ảnh đại diện, đổi email, "
                "thiết bị đăng nhập, quản trị tài khoản")
    tl.thay_doan("Chưa có trong tài liệu này:", kit.doan(
        "**Chưa có trong bản này:** API của các service còn lại (`catalog-service`, `booking-service`, "
        "`medical-service`, `notification-service`, `chatbot-service`, `report-service`)."))

    # --- Mục 2: thêm route 5.11 ---
    tl.them_dong("2. Danh sách route", None,
                 kit.dong_route("5.11", "POST", "/api/auth/first-password", "ep_first_password", "Công khai",
                                "Đặt mật khẩu lần đầu (bác sĩ mới)"),
                 sau_dong_chua="/api/auth/google")

    # --- 3.6: first-password đếm chung với đăng nhập sai ---
    tl.them_dong("3.6 Giới hạn và thời hạn", None,
                 kit.dong_bang((4300, 5338), (
                     "Sai mật khẩu hiện tại khi đặt mật khẩu lần đầu (5.11)",
                     "Đếm chung với đăng nhập sai: 5 lần → khoá email 15 phút (429 `DANG_NHAP_SAI_QUA_NHIEU`)")),
                 sau_dong_chua="Đăng nhập sai mật khẩu")

    # --- 5.1: đăng ký nhận thêm cccd ---
    tl.them_dong(MUC_DANG_KY, "REQUEST BODY (JSON)", kit.dong_bang(kit.RONG_TRUONG, (
        "`cccd`", "string", "Không", "Số CCCD gồm đúng 12 chữ số; bỏ trống hoặc không gửi nếu không khai. Xem lưu ý.")))
    tl.thay_chu('  "soDienThoai": "0901234567"', '  "soDienThoai": "0901234567",')
    tl.chen_sau_doan('  "soDienThoai": "0901234567",', kit.dong_code('  "cccd": "079201000001"'))
    tl.chen_sau_doan(" (mặc định), email không được gửi mà liên kết chỉ ghi ra log của identity-service (xem Phụ lục C).",
                     kit.cham("`cccd` chỉ là lời khai lúc đăng ký. Nếu có, sau khi đăng nhập hệ thống tự tìm hồ sơ bệnh "
                              "nhân đã tạo khi đặt lịch như khách bằng số CCCD này để liên kết vào tài khoản. Không "
                              "API nào trả lại số này, và số đã có người dùng cũng không làm đăng ký bị từ chối."))

    # --- 5.4: tài khoản còn mật khẩu mặc định không đăng nhập được ---
    tl.them_dong(MUC_DANG_NHAP, "MÃ LỖI", kit.dong_bang(kit.RONG_LOI, (
        "403", "`PHAI_DOI_MAT_KHAU`",
        "Mật khẩu đúng nhưng tài khoản còn dùng mật khẩu mặc định (tài khoản bác sĩ do quản trị viên tạo). Không cấp "
        "phiên; đặt mật khẩu mới ở 5.11 rồi đăng nhập lại."), cot_giua=(0,)),
                 sau_dong_chua="Mật khẩu đúng nhưng tài khoản bị vô hiệu hoá.")
    tl.chen_sau_doan("Đăng nhập đúng xoá bộ đếm sai mật khẩu.", kit.cham(
        "Nhận 403 `PHAI_DOI_MAT_KHAU`: giữ lại email và mật khẩu vừa nhập, hiện form đặt mật khẩu mới rồi gọi "
        "`POST /api/auth/first-password` (5.11)."))

    # --- 5.7: quên mật khẩu cũng gỡ được trạng thái mật khẩu mặc định ---
    tl.chen_sau_doan("Tài khoản chỉ đăng nhập Google (chưa có mật khẩu) dùng route này để đặt mật khẩu lần đầu.", kit.cham(
        "Tài khoản bác sĩ còn dùng mật khẩu mặc định cũng dùng được route này: đặt lại mật khẩu xong (5.8) thì đăng "
        "nhập như bình thường, không cần gọi 5.11."))

    # --- 5.11: route mới ---
    tl.chen_truoc_doan("6. Hồ sơ cá nhân và mật khẩu", kit.route(
        "5.11", "Đặt mật khẩu lần đầu", "POST", "/api/auth/first-password", "ep_first_password",
        tl.id_bookmark_moi(), "Công khai (không cần access token)",
        ["Dành cho tài khoản đang mang **mật khẩu mặc định**: tài khoản bác sĩ do quản trị viên tạo. Với tài khoản "
         "này `POST /api/auth/login` trả 403 `PHAI_DOI_MAT_KHAU` và không cấp phiên; frontend hiện form đặt mật khẩu "
         "mới rồi gọi route này.",
         "Route này không cấp phiên: sau khi thành công, gọi lại `POST /api/auth/login` bằng mật khẩu mới."],
        [kit.nhan("REQUEST BODY (JSON)"),
         kit.bang_truong([
             ("email", "string", "Có", "Đúng định dạng email, tối đa 255 ký tự."),
             ("matKhauHienTai", "string", "Có",
              "Mật khẩu mặc định (mật khẩu vừa dùng để đăng nhập). Không rỗng, tối đa 200 ký tự."),
             ("matKhauMoi", "string", "Có",
              "Tối thiểu 6 ký tự, tối đa 72 byte UTF-8. Phải khác mật khẩu hiện tại."),
         ]),
         kit.khoi_code('{\n'
                       '  "email": "bs.moi@eclinic.local",\n'
                       '  "matKhauHienTai": "Doctor@123",\n'
                       '  "matKhauMoi": "MatKhauMoi@456"\n'
                       '}'),
         kit.nhan("RESPONSE THÀNH CÔNG — 200 OK"),
         kit.khoi_code('{\n'
                       '  "thanhCong": true,\n'
                       '  "thongDiep": "Đã đặt mật khẩu, vui lòng đăng nhập bằng mật khẩu mới"\n'
                       '}'),
         kit.nhan("MÃ LỖI"),
         kit.bang_loi([
             (400, "DU_LIEU_KHONG_HOP_LE", "Body sai quy tắc; danh sách lỗi từng trường nằm trong `chiTiet`."),
             (400, "MAT_KHAU_MOI_TRUNG_MAT_KHAU_CU", "Mật khẩu mới giống mật khẩu hiện tại."),
             (401, "SAI_THONG_TIN_DANG_NHAP", "Email không tồn tại hoặc mật khẩu hiện tại không đúng."),
             (403, "TAI_KHOAN_CHUA_XAC_THUC", "Mật khẩu đúng nhưng tài khoản còn `CHO_XAC_NHAN`."),
             (403, "TAI_KHOAN_BI_VO_HIEU_HOA", "Mật khẩu đúng nhưng tài khoản bị vô hiệu hoá."),
             (409, "TRANG_THAI_TAI_KHOAN_KHONG_HOP_LE",
              "Tài khoản không mang mật khẩu mặc định (đã có mật khẩu riêng): đăng nhập như bình thường."),
             (429, "DANG_NHAP_SAI_QUA_NHIEU",
              "Sai mật khẩu 5 lần (đếm chung với đăng nhập sai): email bị khoá 15 phút."),
         ]),
         kit.luu_y([
             "Mật khẩu mặc định do phòng khám đặt (`DOCTOR_DEFAULT_PASSWORD`, mặc định `Doctor@123`, xem Phụ lục C). "
             "Email báo tài khoản gửi cho bác sĩ không chứa mật khẩu.",
             "Giống đăng nhập: trạng thái tài khoản (403, 409) chỉ được báo khi mật khẩu hiện tại đúng.",
             "Quên mật khẩu (5.7, 5.8) cũng gỡ được trạng thái mật khẩu mặc định.",
         ])]))

    # --- 10.3: bác sĩ dùng API ngừng công tác của catalog-service ---
    tl.chen_sau_doan(
        "Kích hoạt lại: xoá lý do vô hiệu hoá, gửi email báo; không khôi phục phiên nào, người dùng đăng nhập lại.",
        kit.cham("Route này không đổi trạng thái công tác của bác sĩ. Màn hình quản lý bác sĩ dùng API ngừng công "
                 "tác / công tác lại của `catalog-service` (không thuộc tài liệu này); các API đó tự vô hiệu hoá và "
                 "kích hoạt lại tài khoản của bác sĩ."))

    # --- Phụ lục A, B, C ---
    tl.them_dong("Phụ lục A. Bảng mã lỗi (maLoi)", None, kit.dong_bang(kit.RONG_MA_LOI, (
        "`PHAI_DOI_MAT_KHAU`", "403",
        "Tài khoản đang dùng mật khẩu mặc định, hãy đặt mật khẩu mới trước khi đăng nhập"), cot_giua=(1,)))

    so = tl.danh_sach_so_moi()
    tl.chen_truoc_doan(PHU_LUC_C, kit.tieu_de(2, "B.6 Bác sĩ đăng nhập lần đầu") + "".join(kit.cham(d, so) for d in [
        "Quản trị viên tạo tài khoản bác sĩ; bác sĩ nhận email báo email đăng nhập, mật khẩu là mật khẩu mặc định "
        "của phòng khám.",
        "`POST /api/auth/login` bằng mật khẩu mặc định → 403 `PHAI_DOI_MAT_KHAU`, không có phiên.",
        "Frontend hiện form đặt mật khẩu mới, gửi `POST /api/auth/first-password` với `email`, `matKhauHienTai`, "
        "`matKhauMoi` → 200.",
        "`POST /api/auth/login` bằng mật khẩu mới → 200, nhận `accessToken` trong body và cookie `eclinic_rt`.",
    ]) + kit.khoang_trong(60))

    rong_c = (2900, 3000, 3738)
    tl.them_dong(PHU_LUC_C, None, kit.dong_bang(rong_c, (
        "`DOCTOR_DEFAULT_PASSWORD`",
        "Mật khẩu mặc định của tài khoản bác sĩ do quản trị viên tạo; dùng ở `POST /api/auth/first-password`",
        "Không đặt thì dùng `Doctor@123`")))
    tl.them_dong(PHU_LUC_C, None, kit.dong_bang(rong_c, (
        "`CORS_ALLOWED_ORIGINS`",
        "Đặt cho api-gateway: các địa chỉ frontend được gọi API (mặc định `http://localhost:5173`, "
        "`http://localhost:3000`)",
        "Frontend chạy ở địa chỉ khác bị trình duyệt chặn vì CORS")))
