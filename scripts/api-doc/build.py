"""Dựng lại tài liệu API từ bản 1.0: áp lần lượt phần sửa của từng giai đoạn rồi kiểm tra.

Đọc temp/api-doc/eClinic_API_XacThuc_TaiKhoan.v1.0.docx, ghi đè eClinic_API_XacThuc_TaiKhoan.docx (hoặc ghi ra tệp
khác nếu truyền đường dẫn: dùng khi tài liệu chính đang mở trong Word / LibreOffice nên không ghi đè được).
Chạy lại bao nhiêu lần cũng cho cùng kết quả. Giai đoạn mới: thêm hàm sua của nó vào CAC_GIAI_DOAN.

    python scripts/api-doc/build.py [tep-ra.docx]
"""

import sys
from pathlib import Path

import check
import kit
import p1_tai_khoan
import p2_quy_uoc
import p3_dat_lich
import p4_cua_toi
import p5_phu_luc
import p6_ngon_ngu
import p7_chatbot

GOC = Path(__file__).resolve().parents[2]
BAN_1_0 = GOC / "temp/api-doc/eClinic_API_XacThuc_TaiKhoan.v1.0.docx"
CAC_GIAI_DOAN = (p1_tai_khoan.sua, p2_quy_uoc.sua, p3_dat_lich.sua, p4_cua_toi.sua, p5_phu_luc.sua,
                 p6_ngon_ngu.sua, p7_chatbot.sua)


def dung(den=len(CAC_GIAI_DOAN)):
    """Áp `den` giai đoạn đầu lên bản 1.0."""
    tai_lieu = kit.TaiLieu(BAN_1_0)
    for sua in CAC_GIAI_DOAN[:den]:
        sua(tai_lieu)
    tai_lieu.danh_so_lai_bookmark()
    return tai_lieu


if __name__ == "__main__":
    sys.stdout.reconfigure(encoding="utf-8")
    dich = Path(sys.argv[1]).resolve() if len(sys.argv) > 1 else GOC / kit.TAI_LIEU
    try:
        dung().luu(dich)
    except PermissionError:
        sys.exit(f"Không ghi được {dich.name}: tệp đang mở ở chương trình khác. Đóng tệp rồi chạy lại, hoặc truyền "
                 "1 đường dẫn khác: python scripts/api-doc/build.py <tep-ra.docx>")
    print("Đã ghi", dich.name)
    cac_loi = check.kiem_tra(str(dich))
    for dong in cac_loi:
        print("LỖI:", dong)
    print("ĐẠT" if not cac_loi else f"KHÔNG ĐẠT ({len(cac_loi)} lỗi)")
    sys.exit(1 if cac_loi else 0)
