"""Dựng tài liệu API thứ 2: lịch hẹn và khám bệnh phía bác sĩ và quản trị viên.

Lấy bản 1.0 của tài liệu thứ nhất (temp/api-doc/eClinic_API_XacThuc_TaiKhoan.v1.0.docx) làm khung: giữ định dạng, trang
đầu, đầu trang và chân trang, bỏ toàn bộ nội dung từ mục 1. Sau đó áp lần lượt phần sửa của từng giai đoạn rồi kiểm tra
bằng bsqt_check.py. Ghi đè eClinic_API_LichHen_KhamBenh_BacSi_QuanTri.docx (hoặc ghi ra tệp khác nếu truyền đường dẫn:
dùng khi tài liệu đang mở trong Word / LibreOffice nên không ghi đè được). Chạy lại bao nhiêu lần cũng cho cùng kết quả.
Giai đoạn mới: thêm hàm sua của nó vào CAC_GIAI_DOAN.

    python scripts/api-doc/bsqt_build.py [tep-ra.docx]
"""

import re
import sys
from pathlib import Path
from xml.sax.saxutils import escape

import bsqt_check
import bsqt_p2_khung
import bsqt_p2_mo_hinh
import bsqt_p2_quy_uoc
import bsqt_p3_lich_hang_doi
import bsqt_p4_kham_benh
import bsqt_p5_phu_luc
import bsqt_p5_quan_tri
import bsqt_p6_lich_lam_viec_pdf
import kit

GOC = Path(__file__).resolve().parents[2]
BAN_1_0 = GOC / "temp/api-doc/eClinic_API_XacThuc_TaiKhoan.v1.0.docx"
TAI_LIEU = "eClinic_API_LichHen_KhamBenh_BacSi_QuanTri.docx"
DAU_TRANG_CU = "eClinic — Tài liệu API: Xác thực và Tài khoản"
DAU_TRANG = "eClinic — Tài liệu API: Lịch hẹn và khám bệnh (bác sĩ, quản trị viên)"
TEN_TAI_LIEU = "eClinic — Tài liệu API Lịch hẹn và khám bệnh phía bác sĩ và quản trị viên"
CAC_GIAI_DOAN = (bsqt_p2_khung.sua, bsqt_p2_quy_uoc.sua, bsqt_p2_mo_hinh.sua, bsqt_p3_lich_hang_doi.sua,
                bsqt_p4_kham_benh.sua, bsqt_p5_quan_tri.sua, bsqt_p5_phu_luc.sua,
                bsqt_p6_lich_lam_viec_pdf.sua)


def khung():
    """Bản 1.0 chỉ còn trang đầu (3 dòng tiêu đề, bảng thông tin), đầu trang và tên tài liệu của tài liệu này, không còn
    nhận xét nào của tài liệu thứ nhất."""
    tl = kit.TaiLieu(BAN_1_0)
    tl.xml = tl.xml[:tl.vi_tri_tieu_de("1. Giới thiệu và phạm vi")] + tl.xml[tl.xml.rindex("<w:sectPr"):]
    tl.thay_dau_trang(DAU_TRANG_CU, DAU_TRANG)
    core = tl.tep["docProps/core.xml"].decode("utf-8")
    core, so = re.subn(r"<dc:title>[^<]*</dc:title>", f"<dc:title>{escape(TEN_TAI_LIEU)}</dc:title>", core)
    assert so == 1, "docProps/core.xml không có đúng 1 <dc:title>"
    tl.tep["docProps/core.xml"] = core.encode("utf-8")
    nhan_xet = tl.tep["word/comments.xml"].decode("utf-8")
    mo = re.search(r"<w:comments\b[^>]*>", nhan_xet)
    tl.tep["word/comments.xml"] = (nhan_xet[:mo.end()] + "</w:comments>").encode("utf-8")
    return tl


def dung(den=len(CAC_GIAI_DOAN)):
    """Áp `den` giai đoạn đầu lên khung."""
    tai_lieu = khung()
    for sua in CAC_GIAI_DOAN[:den]:
        sua(tai_lieu)
    tai_lieu.danh_so_lai_bookmark()
    return tai_lieu


if __name__ == "__main__":
    sys.stdout.reconfigure(encoding="utf-8")
    dich = Path(sys.argv[1]).resolve() if len(sys.argv) > 1 else GOC / TAI_LIEU
    try:
        dung().luu(dich)
    except PermissionError:
        sys.exit(f"Không ghi được {dich.name}: tệp đang mở ở chương trình khác. Đóng tệp rồi chạy lại, hoặc truyền "
                 "1 đường dẫn khác: python scripts/api-doc/bsqt_build.py <tep-ra.docx>")
    print("Đã ghi", dich.name)
    cac_loi = bsqt_check.kiem_tra(str(dich))
    for dong in cac_loi:
        print("LỖI:", dong)
    print("ĐẠT" if not cac_loi else f"KHÔNG ĐẠT ({len(cac_loi)} lỗi)")
    sys.exit(1 if cac_loi else 0)
