package iuh.fit.nhom16.pcstore.sales.service;

import iuh.fit.nhom16.pcstore.sales.dto.OrderDtos.Shipping;
import iuh.fit.nhom16.pcstore.sales.dto.OrderDtos.ShippingMethod;
import iuh.fit.nhom16.pcstore.sales.dto.OrderDtos.ShippingOptionView;
import iuh.fit.nhom16.pcstore.sales.exception.OrderException;
import iuh.fit.nhom16.pcstore.sales.service.VietnamAddressRegistry.DistrictNode;
import iuh.fit.nhom16.pcstore.sales.service.VietnamAddressRegistry.ProvinceNode;
import iuh.fit.nhom16.pcstore.sales.service.VietnamAddressRegistry.VerifiedLocation;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Server-authoritative shipping eligibility and fee calculation based strictly on verified
 * GSO administrative codes (provinceCode & districtCode) rather than raw address substrings.
 */
public final class ShippingPolicy {
    public static final BigDecimal EXPRESS_FEE = BigDecimal.valueOf(50_000);
    public static final BigDecimal INSTANT_FEE = BigDecimal.valueOf(80_000);
    public static final BigDecimal STORE_PICKUP_FEE = BigDecimal.ZERO;

    public static final int HA_NOI_PROVINCE_CODE = 1;
    public static final int HO_CHI_MINH_PROVINCE_CODE = 79;

    // Inner-city urban districts of Ha Noi (code=1) eligible for 2h instant delivery
    private static final Set<Integer> HA_NOI_INSTANT_DISTRICTS = Set.of(
            1, 2, 3, 4, 5, 6, 7, 8, 9, 19, 21, 268);

    // Inner-city urban districts & Thu Duc City of Ho Chi Minh City (code=79) eligible for 2h instant delivery
    private static final Set<Integer> HO_CHI_MINH_INSTANT_DISTRICTS = Set.of(
            760, 761, 764, 765, 766, 767, 768, 769, 770, 771, 772, 773, 774, 775, 776, 777, 778);

    // Official GSO offshore island district codes where door-to-door courier delivery is unavailable
    // (Co To, Bach Long Vi, Con Co, Hoang Sa, Ly Son, Truong Sa, Phu Quy, Con Dao, Kien Hai)
    private static final Set<Integer> OFFSHORE_ISLAND_DISTRICT_CODES = Set.of(
            207, 318, 471, 498, 536, 576, 602, 755, 912);

    private ShippingPolicy() { }

    public record ResolvedShipping(VerifiedLocation location, ShippingMethod method, BigDecimal fee) { }

    public static ResolvedShipping verifyAndResolve(ShippingMethod method, Shipping shipping) {
        VerifiedLocation verified = VietnamAddressRegistry.verify(shipping);
        ShippingMethod effective = method == null ? ShippingMethod.EXPRESS : method;
        BigDecimal fee = resolveFeeByCodes(effective, verified.provinceCode(), verified.districtCode(), verified.districtName(), verified.provinceName());
        return new ResolvedShipping(verified, effective, fee);
    }

    public static BigDecimal resolveShippingFee(ShippingMethod method, Shipping shipping) {
        return verifyAndResolve(method, shipping).fee();
    }

    public static BigDecimal resolveFeeByCodes(ShippingMethod method, Integer provinceCode, Integer districtCode,
                                               String districtLabel, String provinceLabel) {
        ShippingMethod effective = method == null ? ShippingMethod.EXPRESS : method;
        if (effective == ShippingMethod.STORE_PICKUP) {
            return STORE_PICKUP_FEE;
        }
        if (provinceCode == null || districtCode == null) {
            throw OrderException.badRequest("INVALID_SHIPPING_ADDRESS",
                    "Thiếu mã hành chính tỉnh/quận hợp lệ để tính phí vận chuyển.");
        }
        if (OFFSHORE_ISLAND_DISTRICT_CODES.contains(districtCode)) {
            throw OrderException.conflict(
                    "SHIPPING_ADDRESS_UNSUPPORTED",
                    "Khu vực hải đảo (" + (districtLabel == null ? "" : districtLabel)
                            + ") hiện chưa hỗ trợ giao hàng tận nơi. Vui lòng chọn địa chỉ đất liền hoặc chọn Nhận tại cửa hàng.",
                    Map.of("supportedShippingMethods", List.of(ShippingMethod.STORE_PICKUP.name())));
        }
        if (effective == ShippingMethod.INSTANT) {
            if (!isInstantSupported(provinceCode, districtCode)) {
                throw OrderException.conflict(
                        "SHIPPING_METHOD_UNSUPPORTED",
                        "Ship hỏa tốc 2h chỉ áp dụng cho các quận nội thành TP. Hồ Chí Minh và Hà Nội. Vui lòng chọn Vận chuyển nhanh hoặc Nhận tại cửa hàng.",
                        Map.of("supportedShippingMethods", List.of(
                                ShippingMethod.EXPRESS.name(),
                                ShippingMethod.STORE_PICKUP.name())));
            }
            return INSTANT_FEE;
        }
        return EXPRESS_FEE;
    }

    public static boolean isInstantSupported(int provinceCode, int districtCode) {
        if (provinceCode == HA_NOI_PROVINCE_CODE) {
            return HA_NOI_INSTANT_DISTRICTS.contains(districtCode);
        }
        if (provinceCode == HO_CHI_MINH_PROVINCE_CODE) {
            return HO_CHI_MINH_INSTANT_DISTRICTS.contains(districtCode);
        }
        return false;
    }

    public static List<ShippingOptionView> optionsForLocation(String province, String district) {
        Optional<ProvinceNode> provOpt = VietnamAddressRegistry.findProvince(province);
        Optional<DistrictNode> distOpt = provOpt.flatMap(p -> VietnamAddressRegistry.findDistrict(p.code(), district));

        boolean offshoreUnsupported = distOpt.isPresent() && OFFSHORE_ISLAND_DISTRICT_CODES.contains(distOpt.get().code());
        boolean instantSupported = provOpt.isPresent() && distOpt.isPresent()
                && !offshoreUnsupported
                && isInstantSupported(provOpt.get().code(), distOpt.get().code());

        String instantReason;
        if (offshoreUnsupported) {
            instantReason = "Khu vực hải đảo không hỗ trợ giao hỏa tốc.";
        } else if (provOpt.isEmpty() || distOpt.isEmpty()) {
            instantReason = "Chọn tỉnh/thành và quận/huyện nội thành TP.HCM hoặc Hà Nội để kiểm tra Ship hỏa tốc.";
        } else {
            instantReason = "Chỉ áp dụng cho các quận nội thành TP. Hồ Chí Minh và Hà Nội.";
        }

        List<ShippingOptionView> options = new ArrayList<>();
        options.add(new ShippingOptionView(
                ShippingMethod.EXPRESS,
                "Vận chuyển nhanh",
                "Giao hàng tiêu chuẩn toàn quốc (2–4 ngày làm việc)",
                EXPRESS_FEE,
                !offshoreUnsupported,
                offshoreUnsupported ? "Khu vực huyện đảo xa chưa hỗ trợ giao tận nơi." : null));
        options.add(new ShippingOptionView(
                ShippingMethod.INSTANT,
                "Ship hỏa tốc 2h",
                "Giao nhanh trong 2 giờ nội thành TP.HCM & Hà Nội",
                INSTANT_FEE,
                instantSupported,
                instantSupported ? null : instantReason));
        options.add(new ShippingOptionView(
                ShippingMethod.STORE_PICKUP,
                "Nhận tại cửa hàng",
                "Giữ hàng và nhận trực tiếp tại showroom PixelCore (Miễn phí)",
                STORE_PICKUP_FEE,
                true,
                null));
        return List.copyOf(options);
    }
}
