package iuh.fit.se.eclinic.booking.util;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;

import javax.imageio.ImageIO;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

/**
 * Vẽ mã QR thành ảnh PNG đen trắng. ZXing chỉ tính ma trận, ảnh do ImageIO của JDK ghi (chạy được ở chế độ headless).
 */
public final class MaQr {

    /** Viền trắng quanh mã, tính bằng số ô: 4 là mức chuẩn để máy quét đọc được cả khi in ra giấy. */
    private static final int VIEN_TRANG = 4;

    private static final Map<EncodeHintType, Object> TUY_CHON = Map.of(
            EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M,
            EncodeHintType.CHARACTER_SET, "UTF-8",
            EncodeHintType.MARGIN, VIEN_TRANG);

    static {
        // Ảnh nhỏ, ghi thẳng vào bộ nhớ: không tạo file tạm trên đĩa
        ImageIO.setUseCache(false);
    }

    private MaQr() {
    }

    /**
     * @param noiDung   chuỗi đưa vào mã QR
     * @param kichThuoc cạnh ảnh (pixel); ảnh vuông
     */
    public static byte[] taoPng(String noiDung, int kichThuoc) {
        BitMatrix maTran;
        try {
            maTran = new QRCodeWriter().encode(noiDung, BarcodeFormat.QR_CODE, kichThuoc, kichThuoc, TUY_CHON);
        } catch (WriterException e) {
            throw new IllegalStateException("Không tạo được mã QR", e);
        }
        int rong = maTran.getWidth();
        int cao = maTran.getHeight();
        // Ảnh 1 bit/pixel: chỉ số màu 0 = đen, 1 = trắng
        BufferedImage anh = new BufferedImage(rong, cao, BufferedImage.TYPE_BYTE_BINARY);
        WritableRaster diemAnh = anh.getRaster();
        int[] dong = new int[rong];
        for (int y = 0; y < cao; y++) {
            for (int x = 0; x < rong; x++) {
                dong[x] = maTran.get(x, y) ? 0 : 1;
            }
            diemAnh.setPixels(0, y, rong, 1, dong);
        }
        try (ByteArrayOutputStream ketQua = new ByteArrayOutputStream()) {
            if (!ImageIO.write(anh, "png", ketQua)) {
                throw new IllegalStateException("JDK không có bộ ghi ảnh PNG");
            }
            return ketQua.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Không ghi được ảnh QR", e);
        }
    }

}
