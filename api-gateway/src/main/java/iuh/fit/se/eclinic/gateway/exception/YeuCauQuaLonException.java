package iuh.fit.se.eclinic.gateway.exception;

import org.springframework.util.unit.DataSize;

import lombok.Getter;

/** Body của request vượt giới hạn dung lượng của gateway (GioiHanKichThuocFilter) -> 413. */
@Getter
public class YeuCauQuaLonException extends RuntimeException {

    private final DataSize kichThuocToiDa;

    public YeuCauQuaLonException(DataSize kichThuocToiDa) {
        super("Request vượt quá " + kichThuocToiDa.toBytes() + " byte");
        this.kichThuocToiDa = kichThuocToiDa;
    }

}
