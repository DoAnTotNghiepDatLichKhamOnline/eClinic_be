package iuh.fit.se.eclinic.common.dulieumau;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import iuh.fit.se.eclinic.common.enums.GioiTinh;

/**
 * URL ảnh mẫu thật trên Cloudinary ({@code eclinic/mau/...}) cho dữ liệu mẫu, DOANTOTNGH-27.
 * <p>
 * FILE TỰ SINH, không sửa tay: sửa scripts/anh-mau/anh-mau.json (qua tai-len.js) rồi chạy
 * {@code node scripts/anh-mau/sinh-java.js}.
 * <p>
 * Ảnh dùng chung cho nhiều bản ghi và không thuộc bản ghi nào: ảnh giới thiệu mẫu để {@code maLuuTru} null, còn
 * xoá ảnh đại diện chỉ xoá khoá riêng của tài khoản, nên không thao tác nào xoá được ảnh trong thư mục này.
 */
public final class AnhMau {

    private AnhMau() {
    }

    /** Chân dung riêng của 100 bác sĩ mẫu: phần tử i là bác sĩ số i + 1 (nữ là số chẵn). */
    private static final List<String> BAC_SI = List.of(
            "https://res.cloudinary.com/deied72qz/image/upload/v1794995834/eclinic/mau-hd/bac-si/bs01.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427703/eclinic/mau-hd/bac-si/bs02.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1794995903/eclinic/mau-hd/bac-si/bs03.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1794995905/eclinic/mau-hd/bac-si/bs04.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1794995907/eclinic/mau-hd/bac-si/bs05.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1794995909/eclinic/mau-hd/bac-si/bs06.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1794995910/eclinic/mau-hd/bac-si/bs07.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1794995912/eclinic/mau-hd/bac-si/bs08.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1794995913/eclinic/mau-hd/bac-si/bs09.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1794995915/eclinic/mau-hd/bac-si/bs10.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1794995917/eclinic/mau-hd/bac-si/bs11.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1794995918/eclinic/mau-hd/bac-si/bs12.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427529/eclinic/mau-hd/bac-si/bs13.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427532/eclinic/mau-hd/bac-si/bs14.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427534/eclinic/mau-hd/bac-si/bs15.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427535/eclinic/mau-hd/bac-si/bs16.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427538/eclinic/mau-hd/bac-si/bs17.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427540/eclinic/mau-hd/bac-si/bs18.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427542/eclinic/mau-hd/bac-si/bs19.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427544/eclinic/mau-hd/bac-si/bs20.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427545/eclinic/mau-hd/bac-si/bs21.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427548/eclinic/mau-hd/bac-si/bs22.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427550/eclinic/mau-hd/bac-si/bs23.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427551/eclinic/mau-hd/bac-si/bs24.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427553/eclinic/mau-hd/bac-si/bs25.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427554/eclinic/mau-hd/bac-si/bs26.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427980/eclinic/mau-hd/bac-si/bs27.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427558/eclinic/mau-hd/bac-si/bs28.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427560/eclinic/mau-hd/bac-si/bs29.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427562/eclinic/mau-hd/bac-si/bs30.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427564/eclinic/mau-hd/bac-si/bs31.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427566/eclinic/mau-hd/bac-si/bs32.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427568/eclinic/mau-hd/bac-si/bs33.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427570/eclinic/mau-hd/bac-si/bs34.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427572/eclinic/mau-hd/bac-si/bs35.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427574/eclinic/mau-hd/bac-si/bs36.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427575/eclinic/mau-hd/bac-si/bs37.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427577/eclinic/mau-hd/bac-si/bs38.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427579/eclinic/mau-hd/bac-si/bs39.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427580/eclinic/mau-hd/bac-si/bs40.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427582/eclinic/mau-hd/bac-si/bs41.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427584/eclinic/mau-hd/bac-si/bs42.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427586/eclinic/mau-hd/bac-si/bs43.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427589/eclinic/mau-hd/bac-si/bs44.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427590/eclinic/mau-hd/bac-si/bs45.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427592/eclinic/mau-hd/bac-si/bs46.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427594/eclinic/mau-hd/bac-si/bs47.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427596/eclinic/mau-hd/bac-si/bs48.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427597/eclinic/mau-hd/bac-si/bs49.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427599/eclinic/mau-hd/bac-si/bs50.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427982/eclinic/mau-hd/bac-si/bs51.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427604/eclinic/mau-hd/bac-si/bs52.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427983/eclinic/mau-hd/bac-si/bs53.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427608/eclinic/mau-hd/bac-si/bs54.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427609/eclinic/mau-hd/bac-si/bs55.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427611/eclinic/mau-hd/bac-si/bs56.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427614/eclinic/mau-hd/bac-si/bs57.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427616/eclinic/mau-hd/bac-si/bs58.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427618/eclinic/mau-hd/bac-si/bs59.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427622/eclinic/mau-hd/bac-si/bs60.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427624/eclinic/mau-hd/bac-si/bs61.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427626/eclinic/mau-hd/bac-si/bs62.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427628/eclinic/mau-hd/bac-si/bs63.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427630/eclinic/mau-hd/bac-si/bs64.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427631/eclinic/mau-hd/bac-si/bs65.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427633/eclinic/mau-hd/bac-si/bs66.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427985/eclinic/mau-hd/bac-si/bs67.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427639/eclinic/mau-hd/bac-si/bs68.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427641/eclinic/mau-hd/bac-si/bs69.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427987/eclinic/mau-hd/bac-si/bs70.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427645/eclinic/mau-hd/bac-si/bs71.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427647/eclinic/mau-hd/bac-si/bs72.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427649/eclinic/mau-hd/bac-si/bs73.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427651/eclinic/mau-hd/bac-si/bs74.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427653/eclinic/mau-hd/bac-si/bs75.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427655/eclinic/mau-hd/bac-si/bs76.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427656/eclinic/mau-hd/bac-si/bs77.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427658/eclinic/mau-hd/bac-si/bs78.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427660/eclinic/mau-hd/bac-si/bs79.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427662/eclinic/mau-hd/bac-si/bs80.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427663/eclinic/mau-hd/bac-si/bs81.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427665/eclinic/mau-hd/bac-si/bs82.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427667/eclinic/mau-hd/bac-si/bs83.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427669/eclinic/mau-hd/bac-si/bs84.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795428014/eclinic/mau-hd/bac-si/bs85.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427672/eclinic/mau-hd/bac-si/bs86.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427674/eclinic/mau-hd/bac-si/bs87.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427675/eclinic/mau-hd/bac-si/bs88.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427677/eclinic/mau-hd/bac-si/bs89.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427679/eclinic/mau-hd/bac-si/bs90.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427681/eclinic/mau-hd/bac-si/bs91.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427684/eclinic/mau-hd/bac-si/bs92.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795428034/eclinic/mau-hd/bac-si/bs93.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427687/eclinic/mau-hd/bac-si/bs94.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427689/eclinic/mau-hd/bac-si/bs95.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427692/eclinic/mau-hd/bac-si/bs96.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427693/eclinic/mau-hd/bac-si/bs97.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427695/eclinic/mau-hd/bac-si/bs98.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427696/eclinic/mau-hd/bac-si/bs99.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427698/eclinic/mau-hd/bac-si/bs100.jpg");

    /** Chân dung dùng chung theo giới tính (bác sĩ chính + nam01/02), chỉ cho bác sĩ mẫu số lớn hơn 100. */
    private static final List<String> BAC_SI_NAM = List.of(
            "https://res.cloudinary.com/deied72qz/image/upload/v1794995834/eclinic/mau-hd/bac-si/bs01.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1794995903/eclinic/mau-hd/bac-si/bs03.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1794995907/eclinic/mau-hd/bac-si/bs05.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1794995910/eclinic/mau-hd/bac-si/bs07.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1794995913/eclinic/mau-hd/bac-si/bs09.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1794995917/eclinic/mau-hd/bac-si/bs11.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427529/eclinic/mau-hd/bac-si/bs13.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427534/eclinic/mau-hd/bac-si/bs15.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427538/eclinic/mau-hd/bac-si/bs17.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427542/eclinic/mau-hd/bac-si/bs19.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427545/eclinic/mau-hd/bac-si/bs21.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427550/eclinic/mau-hd/bac-si/bs23.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427553/eclinic/mau-hd/bac-si/bs25.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427980/eclinic/mau-hd/bac-si/bs27.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427560/eclinic/mau-hd/bac-si/bs29.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427564/eclinic/mau-hd/bac-si/bs31.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427568/eclinic/mau-hd/bac-si/bs33.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427572/eclinic/mau-hd/bac-si/bs35.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427575/eclinic/mau-hd/bac-si/bs37.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427579/eclinic/mau-hd/bac-si/bs39.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427582/eclinic/mau-hd/bac-si/bs41.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427586/eclinic/mau-hd/bac-si/bs43.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427590/eclinic/mau-hd/bac-si/bs45.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427594/eclinic/mau-hd/bac-si/bs47.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427597/eclinic/mau-hd/bac-si/bs49.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427982/eclinic/mau-hd/bac-si/bs51.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427983/eclinic/mau-hd/bac-si/bs53.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427609/eclinic/mau-hd/bac-si/bs55.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427614/eclinic/mau-hd/bac-si/bs57.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427618/eclinic/mau-hd/bac-si/bs59.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427624/eclinic/mau-hd/bac-si/bs61.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427628/eclinic/mau-hd/bac-si/bs63.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427631/eclinic/mau-hd/bac-si/bs65.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427985/eclinic/mau-hd/bac-si/bs67.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427641/eclinic/mau-hd/bac-si/bs69.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427645/eclinic/mau-hd/bac-si/bs71.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427649/eclinic/mau-hd/bac-si/bs73.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427653/eclinic/mau-hd/bac-si/bs75.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427656/eclinic/mau-hd/bac-si/bs77.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427660/eclinic/mau-hd/bac-si/bs79.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427663/eclinic/mau-hd/bac-si/bs81.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427667/eclinic/mau-hd/bac-si/bs83.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795428014/eclinic/mau-hd/bac-si/bs85.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427674/eclinic/mau-hd/bac-si/bs87.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427677/eclinic/mau-hd/bac-si/bs89.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427681/eclinic/mau-hd/bac-si/bs91.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795428034/eclinic/mau-hd/bac-si/bs93.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427689/eclinic/mau-hd/bac-si/bs95.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427693/eclinic/mau-hd/bac-si/bs97.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427696/eclinic/mau-hd/bac-si/bs99.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1794995920/eclinic/mau-hd/bac-si/nam01.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1794995922/eclinic/mau-hd/bac-si/nam02.jpg");

    private static final List<String> BAC_SI_NU = List.of(
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427703/eclinic/mau-hd/bac-si/bs02.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1794995905/eclinic/mau-hd/bac-si/bs04.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1794995909/eclinic/mau-hd/bac-si/bs06.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1794995912/eclinic/mau-hd/bac-si/bs08.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1794995915/eclinic/mau-hd/bac-si/bs10.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427698/eclinic/mau-hd/bac-si/bs100.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1794995918/eclinic/mau-hd/bac-si/bs12.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427532/eclinic/mau-hd/bac-si/bs14.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427535/eclinic/mau-hd/bac-si/bs16.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427540/eclinic/mau-hd/bac-si/bs18.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427544/eclinic/mau-hd/bac-si/bs20.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427548/eclinic/mau-hd/bac-si/bs22.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427551/eclinic/mau-hd/bac-si/bs24.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427554/eclinic/mau-hd/bac-si/bs26.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427558/eclinic/mau-hd/bac-si/bs28.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427562/eclinic/mau-hd/bac-si/bs30.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427566/eclinic/mau-hd/bac-si/bs32.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427570/eclinic/mau-hd/bac-si/bs34.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427574/eclinic/mau-hd/bac-si/bs36.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427577/eclinic/mau-hd/bac-si/bs38.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427580/eclinic/mau-hd/bac-si/bs40.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427584/eclinic/mau-hd/bac-si/bs42.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427589/eclinic/mau-hd/bac-si/bs44.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427592/eclinic/mau-hd/bac-si/bs46.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427596/eclinic/mau-hd/bac-si/bs48.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427599/eclinic/mau-hd/bac-si/bs50.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427604/eclinic/mau-hd/bac-si/bs52.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427608/eclinic/mau-hd/bac-si/bs54.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427611/eclinic/mau-hd/bac-si/bs56.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427616/eclinic/mau-hd/bac-si/bs58.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427622/eclinic/mau-hd/bac-si/bs60.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427626/eclinic/mau-hd/bac-si/bs62.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427630/eclinic/mau-hd/bac-si/bs64.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427633/eclinic/mau-hd/bac-si/bs66.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427639/eclinic/mau-hd/bac-si/bs68.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427987/eclinic/mau-hd/bac-si/bs70.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427647/eclinic/mau-hd/bac-si/bs72.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427651/eclinic/mau-hd/bac-si/bs74.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427655/eclinic/mau-hd/bac-si/bs76.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427658/eclinic/mau-hd/bac-si/bs78.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427662/eclinic/mau-hd/bac-si/bs80.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427665/eclinic/mau-hd/bac-si/bs82.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427669/eclinic/mau-hd/bac-si/bs84.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427672/eclinic/mau-hd/bac-si/bs86.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427675/eclinic/mau-hd/bac-si/bs88.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427679/eclinic/mau-hd/bac-si/bs90.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427684/eclinic/mau-hd/bac-si/bs92.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427687/eclinic/mau-hd/bac-si/bs94.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427692/eclinic/mau-hd/bac-si/bs96.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427695/eclinic/mau-hd/bac-si/bs98.jpg");

    /** Ảnh đại diện bệnh nhân / quản trị viên (thường phục, không áo blouse). */
    private static final List<String> NGUOI_DUNG_NAM = List.of(
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427818/eclinic/mau-hd/nguoi-dung/nam01.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1794995924/eclinic/mau-hd/nguoi-dung/nam02.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1794995925/eclinic/mau-hd/nguoi-dung/nam03.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427822/eclinic/mau-hd/nguoi-dung/nam04.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1794995927/eclinic/mau-hd/nguoi-dung/nam05.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1794995928/eclinic/mau-hd/nguoi-dung/nam06.jpg");

    private static final List<String> NGUOI_DUNG_NU = List.of(
            "https://res.cloudinary.com/deied72qz/image/upload/v1794995929/eclinic/mau-hd/nguoi-dung/nu01.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1794995930/eclinic/mau-hd/nguoi-dung/nu02.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427828/eclinic/mau-hd/nguoi-dung/nu03.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1794995932/eclinic/mau-hd/nguoi-dung/nu04.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427831/eclinic/mau-hd/nguoi-dung/nu05.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1795427833/eclinic/mau-hd/nguoi-dung/nu06.jpg");

    /** Ảnh làm việc (khám, tư vấn, phòng khám). */
    private static final List<String> CONG_VIEC = List.of(
            "https://res.cloudinary.com/deied72qz/image/upload/v1794995835/eclinic/mau-hd/cong-viec/cv01.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1794995937/eclinic/mau-hd/cong-viec/cv02.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1794995939/eclinic/mau-hd/cong-viec/cv03.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1794995940/eclinic/mau-hd/cong-viec/cv04.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1794995942/eclinic/mau-hd/cong-viec/cv05.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1794995943/eclinic/mau-hd/cong-viec/cv06.jpg");

    /** Ảnh chứng chỉ / bằng cấp. */
    private static final List<String> CHUNG_CHI = List.of(
            "https://res.cloudinary.com/deied72qz/image/upload/v1794995944/eclinic/mau-hd/chung-chi/cc01.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1794995954/eclinic/mau-hd/chung-chi/cc02.jpg");

    /** Ảnh giải thưởng (cúp, huy chương). */
    private static final List<String> GIAI_THUONG = List.of(
            "https://res.cloudinary.com/deied72qz/image/upload/v1794995956/eclinic/mau-hd/giai-thuong/gt01.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1794995960/eclinic/mau-hd/giai-thuong/gt02.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1794995961/eclinic/mau-hd/giai-thuong/gt03.jpg",
            "https://res.cloudinary.com/deied72qz/image/upload/v1794995963/eclinic/mau-hd/giai-thuong/gt04.jpg");

    /** Tên chuyên khoa mẫu (đúng như DuLieuMau / DuLieuMauLon) -> ảnh minh hoạ. */
    public static final Map<String, String> CHUYEN_KHOA = Map.ofEntries(
            Map.entry("Chấn thương chỉnh hình", "https://res.cloudinary.com/deied72qz/image/upload/v1795427904/eclinic/mau-hd/chuyen-khoa/chan-thuong-chinh-hinh.png"),
            Map.entry("Cơ xương khớp", "https://res.cloudinary.com/deied72qz/image/upload/v1795427879/eclinic/mau-hd/chuyen-khoa/co-xuong-khop.png"),
            Map.entry("Da liễu", "https://res.cloudinary.com/deied72qz/image/upload/v1795427856/eclinic/mau-hd/chuyen-khoa/da-lieu.png"),
            Map.entry("Dị ứng - Miễn dịch", "https://res.cloudinary.com/deied72qz/image/upload/v1795427917/eclinic/mau-hd/chuyen-khoa/di-ung-mien-dich.png"),
            Map.entry("Dinh dưỡng", "https://res.cloudinary.com/deied72qz/image/upload/v1795427902/eclinic/mau-hd/chuyen-khoa/dinh-duong.png"),
            Map.entry("Hô hấp", "https://res.cloudinary.com/deied72qz/image/upload/v1795427874/eclinic/mau-hd/chuyen-khoa/ho-hap.png"),
            Map.entry("Huyết học", "https://res.cloudinary.com/deied72qz/image/upload/v1795427885/eclinic/mau-hd/chuyen-khoa/huyet-hoc.png"),
            Map.entry("Lão khoa", "https://res.cloudinary.com/deied72qz/image/upload/v1795427913/eclinic/mau-hd/chuyen-khoa/lao-khoa.png"),
            Map.entry("Mắt", "https://res.cloudinary.com/deied72qz/image/upload/v1795427866/eclinic/mau-hd/chuyen-khoa/mat.png"),
            Map.entry("Nam khoa", "https://res.cloudinary.com/deied72qz/image/upload/v1795427919/eclinic/mau-hd/chuyen-khoa/nam-khoa.png"),
            Map.entry("Ngoại thần kinh", "https://res.cloudinary.com/deied72qz/image/upload/v1795427908/eclinic/mau-hd/chuyen-khoa/ngoai-than-kinh.png"),
            Map.entry("Ngoại tổng quát", "https://res.cloudinary.com/deied72qz/image/upload/v1795427906/eclinic/mau-hd/chuyen-khoa/ngoai-tong-quat.png"),
            Map.entry("Nhi khoa", "https://res.cloudinary.com/deied72qz/image/upload/v1795427854/eclinic/mau-hd/chuyen-khoa/nhi-khoa.png"),
            Map.entry("Nội tiết", "https://res.cloudinary.com/deied72qz/image/upload/v1795427876/eclinic/mau-hd/chuyen-khoa/noi-tiet.png"),
            Map.entry("Nội tổng quát", "https://res.cloudinary.com/deied72qz/image/upload/v1795427852/eclinic/mau-hd/chuyen-khoa/noi-tong-quat.png"),
            Map.entry("Phẫu thuật tạo hình", "https://res.cloudinary.com/deied72qz/image/upload/v1795427924/eclinic/mau-hd/chuyen-khoa/phau-thuat-tao-hinh.png"),
            Map.entry("Phục hồi chức năng", "https://res.cloudinary.com/deied72qz/image/upload/v1795427896/eclinic/mau-hd/chuyen-khoa/phuc-hoi-chuc-nang.png"),
            Map.entry("Răng Hàm Mặt", "https://res.cloudinary.com/deied72qz/image/upload/v1795427868/eclinic/mau-hd/chuyen-khoa/rang-ham-mat.png"),
            Map.entry("Sản phụ khoa", "https://res.cloudinary.com/deied72qz/image/upload/v1795427862/eclinic/mau-hd/chuyen-khoa/san-phu-khoa.png"),
            Map.entry("Sơ sinh", "https://res.cloudinary.com/deied72qz/image/upload/v1795427922/eclinic/mau-hd/chuyen-khoa/so-sinh.png"),
            Map.entry("Tai Mũi Họng", "https://res.cloudinary.com/deied72qz/image/upload/v1795427858/eclinic/mau-hd/chuyen-khoa/tai-mui-hong.png"),
            Map.entry("Tâm thần", "https://res.cloudinary.com/deied72qz/image/upload/v1795427890/eclinic/mau-hd/chuyen-khoa/tam-than.png"),
            Map.entry("Thần kinh", "https://res.cloudinary.com/deied72qz/image/upload/v1795427870/eclinic/mau-hd/chuyen-khoa/than-kinh.png"),
            Map.entry("Thận - Tiết niệu", "https://res.cloudinary.com/deied72qz/image/upload/v1795427881/eclinic/mau-hd/chuyen-khoa/than-tiet-nieu.png"),
            Map.entry("Tiêu hoá", "https://res.cloudinary.com/deied72qz/image/upload/v1795427872/eclinic/mau-hd/chuyen-khoa/tieu-hoa.png"),
            Map.entry("Tim mạch", "https://res.cloudinary.com/deied72qz/image/upload/v1795427860/eclinic/mau-hd/chuyen-khoa/tim-mach.png"),
            Map.entry("Truyền nhiễm", "https://res.cloudinary.com/deied72qz/image/upload/v1795427887/eclinic/mau-hd/chuyen-khoa/truyen-nhiem.png"),
            Map.entry("Ung bướu", "https://res.cloudinary.com/deied72qz/image/upload/v1795427883/eclinic/mau-hd/chuyen-khoa/ung-buou.png"),
            Map.entry("Y học cổ truyền", "https://res.cloudinary.com/deied72qz/image/upload/v1795427892/eclinic/mau-hd/chuyen-khoa/y-hoc-co-truyen.png"),
            Map.entry("Y học thể thao", "https://res.cloudinary.com/deied72qz/image/upload/v1795427926/eclinic/mau-hd/chuyen-khoa/y-hoc-the-thao.png"));

    /**
     * URL ảnh mẫu cũ (thư mục {@code eclinic/mau/}, DOANTOTNGH-27) -> URL mới cùng ảnh ({@code eclinic/mau-hd/}, cỡ lớn
     * hơn; chuyên khoa thành biểu tượng), để đổi URL đã lưu trong DB cũ. URL cũ vẫn mở được trên Cloudinary.
     */
    public static final Map<String, String> URL_CU = Map.ofEntries(
            Map.entry("https://res.cloudinary.com/deied72qz/image/upload/v1794322383/eclinic/mau/bac-si/bs01.jpg",
                    "https://res.cloudinary.com/deied72qz/image/upload/v1794995834/eclinic/mau-hd/bac-si/bs01.jpg"),
            Map.entry("https://res.cloudinary.com/deied72qz/image/upload/v1794322385/eclinic/mau/bac-si/bs02.jpg",
                    "https://res.cloudinary.com/deied72qz/image/upload/v1795427703/eclinic/mau-hd/bac-si/bs02.jpg"),
            Map.entry("https://res.cloudinary.com/deied72qz/image/upload/v1794322386/eclinic/mau/bac-si/bs03.jpg",
                    "https://res.cloudinary.com/deied72qz/image/upload/v1794995903/eclinic/mau-hd/bac-si/bs03.jpg"),
            Map.entry("https://res.cloudinary.com/deied72qz/image/upload/v1794322387/eclinic/mau/bac-si/bs04.jpg",
                    "https://res.cloudinary.com/deied72qz/image/upload/v1794995905/eclinic/mau-hd/bac-si/bs04.jpg"),
            Map.entry("https://res.cloudinary.com/deied72qz/image/upload/v1794322388/eclinic/mau/bac-si/bs05.jpg",
                    "https://res.cloudinary.com/deied72qz/image/upload/v1794995907/eclinic/mau-hd/bac-si/bs05.jpg"),
            Map.entry("https://res.cloudinary.com/deied72qz/image/upload/v1794322389/eclinic/mau/bac-si/bs06.jpg",
                    "https://res.cloudinary.com/deied72qz/image/upload/v1794995909/eclinic/mau-hd/bac-si/bs06.jpg"),
            Map.entry("https://res.cloudinary.com/deied72qz/image/upload/v1794322390/eclinic/mau/bac-si/bs07.jpg",
                    "https://res.cloudinary.com/deied72qz/image/upload/v1794995910/eclinic/mau-hd/bac-si/bs07.jpg"),
            Map.entry("https://res.cloudinary.com/deied72qz/image/upload/v1794322391/eclinic/mau/bac-si/bs08.jpg",
                    "https://res.cloudinary.com/deied72qz/image/upload/v1794995912/eclinic/mau-hd/bac-si/bs08.jpg"),
            Map.entry("https://res.cloudinary.com/deied72qz/image/upload/v1794322392/eclinic/mau/bac-si/bs09.jpg",
                    "https://res.cloudinary.com/deied72qz/image/upload/v1794995913/eclinic/mau-hd/bac-si/bs09.jpg"),
            Map.entry("https://res.cloudinary.com/deied72qz/image/upload/v1794322393/eclinic/mau/bac-si/bs10.jpg",
                    "https://res.cloudinary.com/deied72qz/image/upload/v1794995915/eclinic/mau-hd/bac-si/bs10.jpg"),
            Map.entry("https://res.cloudinary.com/deied72qz/image/upload/v1794322394/eclinic/mau/bac-si/bs11.jpg",
                    "https://res.cloudinary.com/deied72qz/image/upload/v1794995917/eclinic/mau-hd/bac-si/bs11.jpg"),
            Map.entry("https://res.cloudinary.com/deied72qz/image/upload/v1794322395/eclinic/mau/bac-si/bs12.jpg",
                    "https://res.cloudinary.com/deied72qz/image/upload/v1794995918/eclinic/mau-hd/bac-si/bs12.jpg"),
            Map.entry("https://res.cloudinary.com/deied72qz/image/upload/v1794322396/eclinic/mau/bac-si/nam01.jpg",
                    "https://res.cloudinary.com/deied72qz/image/upload/v1794995920/eclinic/mau-hd/bac-si/nam01.jpg"),
            Map.entry("https://res.cloudinary.com/deied72qz/image/upload/v1794322397/eclinic/mau/bac-si/nam02.jpg",
                    "https://res.cloudinary.com/deied72qz/image/upload/v1794995922/eclinic/mau-hd/bac-si/nam02.jpg"),
            Map.entry("https://res.cloudinary.com/deied72qz/image/upload/v1794322398/eclinic/mau/nguoi-dung/nam01.jpg",
                    "https://res.cloudinary.com/deied72qz/image/upload/v1795427818/eclinic/mau-hd/nguoi-dung/nam01.jpg"),
            Map.entry("https://res.cloudinary.com/deied72qz/image/upload/v1794322221/eclinic/mau/nguoi-dung/nam02.jpg",
                    "https://res.cloudinary.com/deied72qz/image/upload/v1794995924/eclinic/mau-hd/nguoi-dung/nam02.jpg"),
            Map.entry("https://res.cloudinary.com/deied72qz/image/upload/v1794322222/eclinic/mau/nguoi-dung/nam03.jpg",
                    "https://res.cloudinary.com/deied72qz/image/upload/v1794995925/eclinic/mau-hd/nguoi-dung/nam03.jpg"),
            Map.entry("https://res.cloudinary.com/deied72qz/image/upload/v1794322401/eclinic/mau/nguoi-dung/nam04.jpg",
                    "https://res.cloudinary.com/deied72qz/image/upload/v1795427822/eclinic/mau-hd/nguoi-dung/nam04.jpg"),
            Map.entry("https://res.cloudinary.com/deied72qz/image/upload/v1794322224/eclinic/mau/nguoi-dung/nam05.jpg",
                    "https://res.cloudinary.com/deied72qz/image/upload/v1794995927/eclinic/mau-hd/nguoi-dung/nam05.jpg"),
            Map.entry("https://res.cloudinary.com/deied72qz/image/upload/v1794322224/eclinic/mau/nguoi-dung/nam06.jpg",
                    "https://res.cloudinary.com/deied72qz/image/upload/v1794995928/eclinic/mau-hd/nguoi-dung/nam06.jpg"),
            Map.entry("https://res.cloudinary.com/deied72qz/image/upload/v1794322404/eclinic/mau/nguoi-dung/nu01.jpg",
                    "https://res.cloudinary.com/deied72qz/image/upload/v1794995929/eclinic/mau-hd/nguoi-dung/nu01.jpg"),
            Map.entry("https://res.cloudinary.com/deied72qz/image/upload/v1794322405/eclinic/mau/nguoi-dung/nu02.jpg",
                    "https://res.cloudinary.com/deied72qz/image/upload/v1794995930/eclinic/mau-hd/nguoi-dung/nu02.jpg"),
            Map.entry("https://res.cloudinary.com/deied72qz/image/upload/v1794322227/eclinic/mau/nguoi-dung/nu03.jpg",
                    "https://res.cloudinary.com/deied72qz/image/upload/v1795427828/eclinic/mau-hd/nguoi-dung/nu03.jpg"),
            Map.entry("https://res.cloudinary.com/deied72qz/image/upload/v1794322228/eclinic/mau/nguoi-dung/nu04.jpg",
                    "https://res.cloudinary.com/deied72qz/image/upload/v1794995932/eclinic/mau-hd/nguoi-dung/nu04.jpg"),
            Map.entry("https://res.cloudinary.com/deied72qz/image/upload/v1794322407/eclinic/mau/nguoi-dung/nu05.jpg",
                    "https://res.cloudinary.com/deied72qz/image/upload/v1795427831/eclinic/mau-hd/nguoi-dung/nu05.jpg"),
            Map.entry("https://res.cloudinary.com/deied72qz/image/upload/v1794322408/eclinic/mau/nguoi-dung/nu06.jpg",
                    "https://res.cloudinary.com/deied72qz/image/upload/v1795427833/eclinic/mau-hd/nguoi-dung/nu06.jpg"),
            Map.entry("https://res.cloudinary.com/deied72qz/image/upload/v1794322231/eclinic/mau/cong-viec/cv01.jpg",
                    "https://res.cloudinary.com/deied72qz/image/upload/v1794995835/eclinic/mau-hd/cong-viec/cv01.jpg"),
            Map.entry("https://res.cloudinary.com/deied72qz/image/upload/v1794322232/eclinic/mau/cong-viec/cv02.jpg",
                    "https://res.cloudinary.com/deied72qz/image/upload/v1794995937/eclinic/mau-hd/cong-viec/cv02.jpg"),
            Map.entry("https://res.cloudinary.com/deied72qz/image/upload/v1794322234/eclinic/mau/cong-viec/cv03.jpg",
                    "https://res.cloudinary.com/deied72qz/image/upload/v1794995939/eclinic/mau-hd/cong-viec/cv03.jpg"),
            Map.entry("https://res.cloudinary.com/deied72qz/image/upload/v1794322235/eclinic/mau/cong-viec/cv04.jpg",
                    "https://res.cloudinary.com/deied72qz/image/upload/v1794995940/eclinic/mau-hd/cong-viec/cv04.jpg"),
            Map.entry("https://res.cloudinary.com/deied72qz/image/upload/v1794322237/eclinic/mau/cong-viec/cv05.jpg",
                    "https://res.cloudinary.com/deied72qz/image/upload/v1794995942/eclinic/mau-hd/cong-viec/cv05.jpg"),
            Map.entry("https://res.cloudinary.com/deied72qz/image/upload/v1794322239/eclinic/mau/cong-viec/cv06.jpg",
                    "https://res.cloudinary.com/deied72qz/image/upload/v1794995943/eclinic/mau-hd/cong-viec/cv06.jpg"),
            Map.entry("https://res.cloudinary.com/deied72qz/image/upload/v1794322240/eclinic/mau/chung-chi/cc01.jpg",
                    "https://res.cloudinary.com/deied72qz/image/upload/v1794995944/eclinic/mau-hd/chung-chi/cc01.jpg"),
            Map.entry("https://res.cloudinary.com/deied72qz/image/upload/v1794322244/eclinic/mau/chung-chi/cc02.jpg",
                    "https://res.cloudinary.com/deied72qz/image/upload/v1794995954/eclinic/mau-hd/chung-chi/cc02.jpg"),
            Map.entry("https://res.cloudinary.com/deied72qz/image/upload/v1794397332/eclinic/mau/giai-thuong/gt01.jpg",
                    "https://res.cloudinary.com/deied72qz/image/upload/v1794995956/eclinic/mau-hd/giai-thuong/gt01.jpg"),
            Map.entry("https://res.cloudinary.com/deied72qz/image/upload/v1794397335/eclinic/mau/giai-thuong/gt02.jpg",
                    "https://res.cloudinary.com/deied72qz/image/upload/v1794995960/eclinic/mau-hd/giai-thuong/gt02.jpg"),
            Map.entry("https://res.cloudinary.com/deied72qz/image/upload/v1794397336/eclinic/mau/giai-thuong/gt03.jpg",
                    "https://res.cloudinary.com/deied72qz/image/upload/v1794995961/eclinic/mau-hd/giai-thuong/gt03.jpg"),
            Map.entry("https://res.cloudinary.com/deied72qz/image/upload/v1794397337/eclinic/mau/giai-thuong/gt04.jpg",
                    "https://res.cloudinary.com/deied72qz/image/upload/v1794995963/eclinic/mau-hd/giai-thuong/gt04.jpg"),
            Map.entry("https://res.cloudinary.com/deied72qz/image/upload/v1794322245/eclinic/mau/chuyen-khoa/noi-tong-quat.jpg",
                    "https://res.cloudinary.com/deied72qz/image/upload/v1795427852/eclinic/mau-hd/chuyen-khoa/noi-tong-quat.png"),
            Map.entry("https://res.cloudinary.com/deied72qz/image/upload/v1794322248/eclinic/mau/chuyen-khoa/nhi-khoa.jpg",
                    "https://res.cloudinary.com/deied72qz/image/upload/v1795427854/eclinic/mau-hd/chuyen-khoa/nhi-khoa.png"),
            Map.entry("https://res.cloudinary.com/deied72qz/image/upload/v1794322250/eclinic/mau/chuyen-khoa/da-lieu.jpg",
                    "https://res.cloudinary.com/deied72qz/image/upload/v1795427856/eclinic/mau-hd/chuyen-khoa/da-lieu.png"),
            Map.entry("https://res.cloudinary.com/deied72qz/image/upload/v1794322252/eclinic/mau/chuyen-khoa/tai-mui-hong.jpg",
                    "https://res.cloudinary.com/deied72qz/image/upload/v1795427858/eclinic/mau-hd/chuyen-khoa/tai-mui-hong.png"),
            Map.entry("https://res.cloudinary.com/deied72qz/image/upload/v1794322255/eclinic/mau/chuyen-khoa/tim-mach.jpg",
                    "https://res.cloudinary.com/deied72qz/image/upload/v1795427860/eclinic/mau-hd/chuyen-khoa/tim-mach.png"),
            Map.entry("https://res.cloudinary.com/deied72qz/image/upload/v1794322256/eclinic/mau/chuyen-khoa/san-phu-khoa.jpg",
                    "https://res.cloudinary.com/deied72qz/image/upload/v1795427862/eclinic/mau-hd/chuyen-khoa/san-phu-khoa.png"),
            Map.entry("https://res.cloudinary.com/deied72qz/image/upload/v1794322259/eclinic/mau/chuyen-khoa/mat.jpg",
                    "https://res.cloudinary.com/deied72qz/image/upload/v1795427866/eclinic/mau-hd/chuyen-khoa/mat.png"),
            Map.entry("https://res.cloudinary.com/deied72qz/image/upload/v1794322260/eclinic/mau/chuyen-khoa/rang-ham-mat.jpg",
                    "https://res.cloudinary.com/deied72qz/image/upload/v1795427868/eclinic/mau-hd/chuyen-khoa/rang-ham-mat.png"),
            Map.entry("https://res.cloudinary.com/deied72qz/image/upload/v1794322262/eclinic/mau/chuyen-khoa/than-kinh.jpg",
                    "https://res.cloudinary.com/deied72qz/image/upload/v1795427870/eclinic/mau-hd/chuyen-khoa/than-kinh.png"),
            Map.entry("https://res.cloudinary.com/deied72qz/image/upload/v1794322265/eclinic/mau/chuyen-khoa/tieu-hoa.jpg",
                    "https://res.cloudinary.com/deied72qz/image/upload/v1795427872/eclinic/mau-hd/chuyen-khoa/tieu-hoa.png"),
            Map.entry("https://res.cloudinary.com/deied72qz/image/upload/v1794322268/eclinic/mau/chuyen-khoa/ho-hap.jpg",
                    "https://res.cloudinary.com/deied72qz/image/upload/v1795427874/eclinic/mau-hd/chuyen-khoa/ho-hap.png"),
            Map.entry("https://res.cloudinary.com/deied72qz/image/upload/v1794322271/eclinic/mau/chuyen-khoa/noi-tiet.jpg",
                    "https://res.cloudinary.com/deied72qz/image/upload/v1795427876/eclinic/mau-hd/chuyen-khoa/noi-tiet.png"),
            Map.entry("https://res.cloudinary.com/deied72qz/image/upload/v1794322273/eclinic/mau/chuyen-khoa/co-xuong-khop.jpg",
                    "https://res.cloudinary.com/deied72qz/image/upload/v1795427879/eclinic/mau-hd/chuyen-khoa/co-xuong-khop.png"),
            Map.entry("https://res.cloudinary.com/deied72qz/image/upload/v1794322276/eclinic/mau/chuyen-khoa/than-tiet-nieu.jpg",
                    "https://res.cloudinary.com/deied72qz/image/upload/v1795427881/eclinic/mau-hd/chuyen-khoa/than-tiet-nieu.png"),
            Map.entry("https://res.cloudinary.com/deied72qz/image/upload/v1794322280/eclinic/mau/chuyen-khoa/ung-buou.jpg",
                    "https://res.cloudinary.com/deied72qz/image/upload/v1795427883/eclinic/mau-hd/chuyen-khoa/ung-buou.png"),
            Map.entry("https://res.cloudinary.com/deied72qz/image/upload/v1794322284/eclinic/mau/chuyen-khoa/huyet-hoc.jpg",
                    "https://res.cloudinary.com/deied72qz/image/upload/v1795427885/eclinic/mau-hd/chuyen-khoa/huyet-hoc.png"),
            Map.entry("https://res.cloudinary.com/deied72qz/image/upload/v1794322285/eclinic/mau/chuyen-khoa/truyen-nhiem.jpg",
                    "https://res.cloudinary.com/deied72qz/image/upload/v1795427887/eclinic/mau-hd/chuyen-khoa/truyen-nhiem.png"),
            Map.entry("https://res.cloudinary.com/deied72qz/image/upload/v1794322286/eclinic/mau/chuyen-khoa/tam-than.jpg",
                    "https://res.cloudinary.com/deied72qz/image/upload/v1795427890/eclinic/mau-hd/chuyen-khoa/tam-than.png"),
            Map.entry("https://res.cloudinary.com/deied72qz/image/upload/v1794322287/eclinic/mau/chuyen-khoa/y-hoc-co-truyen.jpg",
                    "https://res.cloudinary.com/deied72qz/image/upload/v1795427892/eclinic/mau-hd/chuyen-khoa/y-hoc-co-truyen.png"),
            Map.entry("https://res.cloudinary.com/deied72qz/image/upload/v1794322289/eclinic/mau/chuyen-khoa/phuc-hoi-chuc-nang.jpg",
                    "https://res.cloudinary.com/deied72qz/image/upload/v1795427896/eclinic/mau-hd/chuyen-khoa/phuc-hoi-chuc-nang.png"),
            Map.entry("https://res.cloudinary.com/deied72qz/image/upload/v1794322292/eclinic/mau/chuyen-khoa/dinh-duong.jpg",
                    "https://res.cloudinary.com/deied72qz/image/upload/v1795427902/eclinic/mau-hd/chuyen-khoa/dinh-duong.png"),
            Map.entry("https://res.cloudinary.com/deied72qz/image/upload/v1794322293/eclinic/mau/chuyen-khoa/chan-thuong-chinh-hinh.jpg",
                    "https://res.cloudinary.com/deied72qz/image/upload/v1795427904/eclinic/mau-hd/chuyen-khoa/chan-thuong-chinh-hinh.png"),
            Map.entry("https://res.cloudinary.com/deied72qz/image/upload/v1794322295/eclinic/mau/chuyen-khoa/ngoai-tong-quat.jpg",
                    "https://res.cloudinary.com/deied72qz/image/upload/v1795427906/eclinic/mau-hd/chuyen-khoa/ngoai-tong-quat.png"),
            Map.entry("https://res.cloudinary.com/deied72qz/image/upload/v1794322297/eclinic/mau/chuyen-khoa/ngoai-than-kinh.jpg",
                    "https://res.cloudinary.com/deied72qz/image/upload/v1795427908/eclinic/mau-hd/chuyen-khoa/ngoai-than-kinh.png"),
            Map.entry("https://res.cloudinary.com/deied72qz/image/upload/v1794322300/eclinic/mau/chuyen-khoa/lao-khoa.jpg",
                    "https://res.cloudinary.com/deied72qz/image/upload/v1795427913/eclinic/mau-hd/chuyen-khoa/lao-khoa.png"),
            Map.entry("https://res.cloudinary.com/deied72qz/image/upload/v1794322303/eclinic/mau/chuyen-khoa/di-ung-mien-dich.jpg",
                    "https://res.cloudinary.com/deied72qz/image/upload/v1795427917/eclinic/mau-hd/chuyen-khoa/di-ung-mien-dich.png"),
            Map.entry("https://res.cloudinary.com/deied72qz/image/upload/v1794322304/eclinic/mau/chuyen-khoa/nam-khoa.jpg",
                    "https://res.cloudinary.com/deied72qz/image/upload/v1795427919/eclinic/mau-hd/chuyen-khoa/nam-khoa.png"),
            Map.entry("https://res.cloudinary.com/deied72qz/image/upload/v1794322308/eclinic/mau/chuyen-khoa/so-sinh.jpg",
                    "https://res.cloudinary.com/deied72qz/image/upload/v1795427922/eclinic/mau-hd/chuyen-khoa/so-sinh.png"),
            Map.entry("https://res.cloudinary.com/deied72qz/image/upload/v1794322310/eclinic/mau/chuyen-khoa/phau-thuat-tao-hinh.jpg",
                    "https://res.cloudinary.com/deied72qz/image/upload/v1795427924/eclinic/mau-hd/chuyen-khoa/phau-thuat-tao-hinh.png"),
            Map.entry("https://res.cloudinary.com/deied72qz/image/upload/v1794322311/eclinic/mau/chuyen-khoa/y-hoc-the-thao.jpg",
                    "https://res.cloudinary.com/deied72qz/image/upload/v1795427926/eclinic/mau-hd/chuyen-khoa/y-hoc-the-thao.png"));

    /** Mọi URL ảnh mẫu. */
    public static final Set<String> TAT_CA = Set.copyOf(Stream.of(BAC_SI, BAC_SI_NAM, BAC_SI_NU, NGUOI_DUNG_NAM,
            NGUOI_DUNG_NU, CONG_VIEC, CHUNG_CHI, GIAI_THUONG, List.copyOf(CHUYEN_KHOA.values())).flatMap(List::stream).toList());

    /** Chân dung riêng của bác sĩ mẫu số {@code so} (1-100); ngoài khoảng đó trả null. */
    public static String bacSi(int so) {
        return so >= 1 && so <= BAC_SI.size() ? BAC_SI.get(so - 1) : null;
    }

    /** Chân dung bác sĩ cùng giới tính, xoay vòng theo {@code so} (KHAC dùng ảnh nam). */
    public static String bacSiTheoGioiTinh(GioiTinh gioiTinh, int so) {
        return chon(gioiTinh == GioiTinh.NU ? BAC_SI_NU : BAC_SI_NAM, so);
    }

    /** Ảnh đại diện người dùng cùng giới tính, xoay vòng theo {@code so} (KHAC dùng ảnh nam). */
    public static String nguoiDung(GioiTinh gioiTinh, int so) {
        return chon(gioiTinh == GioiTinh.NU ? NGUOI_DUNG_NU : NGUOI_DUNG_NAM, so);
    }

    public static String congViec(int i) {
        return chon(CONG_VIEC, i);
    }

    public static String chungChi(int i) {
        return chon(CHUNG_CHI, i);
    }

    public static String giaiThuong(int i) {
        return chon(GIAI_THUONG, i);
    }

    private static String chon(List<String> danhSach, int i) {
        return danhSach.get(Math.floorMod(i, danhSach.size()));
    }
}
