package iuh.fit.se.eclinic.catalog.service;

import java.util.List;
import java.util.Optional;

import iuh.fit.se.eclinic.common.entity.catalog.BacSi;

public interface BacSiService {

    BacSi layTheoId(Long id);

    Optional<BacSi> timTheoTaiKhoanId(Long taiKhoanId);

    List<BacSi> timTheoChuyenKhoa(Long chuyenKhoaId);

}
