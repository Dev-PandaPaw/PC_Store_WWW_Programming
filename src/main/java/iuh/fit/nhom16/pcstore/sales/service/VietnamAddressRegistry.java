package iuh.fit.nhom16.pcstore.sales.service;

import iuh.fit.nhom16.pcstore.sales.dto.OrderDtos.Shipping;
import iuh.fit.nhom16.pcstore.sales.exception.OrderException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.text.Normalizer;
import java.util.*;

/**
 * Authoritative 3-level Vietnam administrative hierarchy (63 provinces, 696 districts, 10,051 wards).
 * Verifies that province, district, and ward genuinely exist and belong to each other,
 * returning canonical GSO administrative codes for shipping eligibility checks.
 */
public final class VietnamAddressRegistry {
    public record WardNode(int code, String name) { }
    public record DistrictNode(int code, String name, List<WardNode> wards) { }
    public record ProvinceNode(int code, String name, List<DistrictNode> districts) { }

    public record VerifiedLocation(
            int provinceCode,
            String provinceName,
            int districtCode,
            String districtName,
            Integer wardCode,
            String wardName,
            String addressLine,
            String formattedAddress) { }

    private static final Map<Integer, ProvinceNode> PROVINCES_BY_CODE;
    private static final Map<String, ProvinceNode> PROVINCES_BY_KEY;
    private static final Map<Integer, Map<Integer, DistrictNode>> DISTRICTS_BY_PROVINCE;
    private static final Map<Integer, Map<String, DistrictNode>> DISTRICT_KEYS_BY_PROVINCE;
    private static final Map<Integer, Map<String, WardNode>> WARD_KEYS_BY_DISTRICT;

    static {
        try (InputStream stream = VietnamAddressRegistry.class.getClassLoader()
                .getResourceAsStream("addresses/vietnam-divisions.json")) {
            if (stream == null) {
                throw new IllegalStateException("Missing classpath resource addresses/vietnam-divisions.json");
            }
            List<ProvinceNode> provinces = new ObjectMapper().readValue(stream, new TypeReference<>() {});
            Map<Integer, ProvinceNode> byCode = new LinkedHashMap<>();
            Map<String, ProvinceNode> byKey = new HashMap<>();
            Map<Integer, Map<Integer, DistrictNode>> distByProv = new HashMap<>();
            Map<Integer, Map<String, DistrictNode>> distKeyByProv = new HashMap<>();
            Map<Integer, Map<String, WardNode>> wardKeyByDist = new HashMap<>();

            for (ProvinceNode province : provinces) {
                byCode.put(province.code(), province);
                for (String key : candidateProvinceKeys(province.name())) {
                    byKey.putIfAbsent(key, province);
                }

                Map<Integer, DistrictNode> dCodeMap = new LinkedHashMap<>();
                Map<String, DistrictNode> dKeyMap = new HashMap<>();
                for (DistrictNode district : province.districts()) {
                    dCodeMap.put(district.code(), district);
                    for (String dKey : candidateDivisionKeys(district.name(), "quan", "huyen", "thi xa", "thanh pho")) {
                        // Exact full name always takes priority; short name added only if unambiguous
                        if (dKey.equals(normalize(district.name()))) {
                            dKeyMap.put(dKey, district);
                        } else {
                            dKeyMap.putIfAbsent(dKey, district);
                        }
                    }

                    Map<String, WardNode> wKeyMap = new HashMap<>();
                    for (WardNode ward : district.wards()) {
                        for (String wKey : candidateDivisionKeys(ward.name(), "phuong", "xa", "thi tran")) {
                            if (wKey.equals(normalize(ward.name()))) {
                                wKeyMap.put(wKey, ward);
                            } else {
                                wKeyMap.putIfAbsent(wKey, ward);
                            }
                        }
                    }
                    wardKeyByDist.put(district.code(), Map.copyOf(wKeyMap));
                }
                distByProv.put(province.code(), Map.copyOf(dCodeMap));
                distKeyByProv.put(province.code(), Map.copyOf(dKeyMap));
            }

            PROVINCES_BY_CODE = Map.copyOf(byCode);
            PROVINCES_BY_KEY = Map.copyOf(byKey);
            DISTRICTS_BY_PROVINCE = Map.copyOf(distByProv);
            DISTRICT_KEYS_BY_PROVINCE = Map.copyOf(distKeyByProv);
            WARD_KEYS_BY_DISTRICT = Map.copyOf(wardKeyByDist);
        } catch (Exception ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    private VietnamAddressRegistry() { }

    public static VerifiedLocation verify(Shipping shipping) {
        if (shipping == null) {
            throw invalidAddress("shipping", "Vui lòng cung cấp đầy đủ thông tin nhận hàng.");
        }
        String recipient = shipping.recipient() == null ? "" : shipping.recipient().trim();
        String street = shipping.addressLine() == null ? "" : shipping.addressLine().trim();
        if (recipient.length() < 2 || recipient.length() > 100) {
            throw invalidAddress("recipient", "Họ tên người nhận phải từ 2 đến 100 ký tự.");
        }
        if (street.length() < 5 || street.length() > 255) {
            throw invalidAddress("addressLine", "Địa chỉ chi tiết (số nhà, tên đường) phải từ 5 đến 255 ký tự.");
        }

        ProvinceNode province = findProvince(shipping.province())
                .orElseThrow(() -> invalidAddress(
                        "province",
                        "Tỉnh / thành phố \"" + safe(shipping.province()) + "\" không hợp lệ hoặc không tồn tại."));

        if (shipping.district() == null || shipping.district().isBlank()) {
            throw invalidAddress("district", "Vui lòng chọn quận / huyện thuộc " + province.name() + ".");
        }
        DistrictNode district = findDistrict(province.code(), shipping.district())
                .orElseThrow(() -> invalidAddress(
                        "district",
                        "Quận / huyện \"" + safe(shipping.district()) + "\" không tồn tại hoặc không thuộc " + province.name() + "."));

        if (shipping.ward() == null || shipping.ward().isBlank()) {
            throw invalidAddress("ward", "Vui lòng chọn phường / xã thuộc " + district.name() + ", " + province.name() + ".");
        }

        Integer wardCode = null;
        String wardName;
        if (district.wards().isEmpty()) {
            // 1-tier offshore island districts without commune/ward subdivisions in GSO v1
            wardName = shipping.ward().trim();
        } else {
            WardNode ward = findWard(district.code(), shipping.ward())
                    .orElseThrow(() -> invalidAddress(
                            "ward",
                            "Phường / xã \"" + safe(shipping.ward()) + "\" không tồn tại hoặc không thuộc "
                                    + district.name() + ", " + province.name() + "."));
            wardCode = ward.code();
            wardName = ward.name();
        }

        String formatted = street + ", " + wardName + ", " + district.name() + ", " + province.name();
        return new VerifiedLocation(
                province.code(),
                province.name(),
                district.code(),
                district.name(),
                wardCode,
                wardName,
                street,
                formatted);
    }

    public static Optional<ProvinceNode> findProvince(String rawProvince) {
        if (rawProvince == null || rawProvince.isBlank()) return Optional.empty();
        String key = normalize(rawProvince)
                .replaceFirst("^tp\\.?\\s+", "")
                .trim();
        ProvinceNode direct = PROVINCES_BY_KEY.get(normalize(rawProvince));
        return Optional.ofNullable(direct != null ? direct : PROVINCES_BY_KEY.get(key));
    }

    public static Optional<DistrictNode> findDistrict(int provinceCode, String rawDistrict) {
        if (rawDistrict == null || rawDistrict.isBlank()) return Optional.empty();
        Map<String, DistrictNode> map = DISTRICT_KEYS_BY_PROVINCE.get(provinceCode);
        if (map == null) return Optional.empty();
        for (String candidate : lookupVariants(rawDistrict, "quan", "huyen", "thi xa", "thanh pho")) {
            DistrictNode found = map.get(candidate);
            if (found != null) return Optional.of(found);
        }
        return Optional.empty();
    }

    public static Optional<WardNode> findWard(int districtCode, String rawWard) {
        if (rawWard == null || rawWard.isBlank()) return Optional.empty();
        Map<String, WardNode> map = WARD_KEYS_BY_DISTRICT.get(districtCode);
        if (map == null) return Optional.empty();
        for (String candidate : lookupVariants(rawWard, "phuong", "xa", "thi tran")) {
            WardNode found = map.get(candidate);
            if (found != null) return Optional.of(found);
        }
        return Optional.empty();
    }

    public static Optional<ProvinceNode> provinceByCode(int code) {
        return Optional.ofNullable(PROVINCES_BY_CODE.get(code));
    }

    public static Optional<DistrictNode> districtByCode(int provinceCode, int districtCode) {
        Map<Integer, DistrictNode> map = DISTRICTS_BY_PROVINCE.get(provinceCode);
        return map == null ? Optional.empty() : Optional.ofNullable(map.get(districtCode));
    }

    private static List<String> candidateProvinceKeys(String name) {
        String norm = normalize(name);
        List<String> keys = new ArrayList<>();
        keys.add(norm);
        String stripped = norm.replaceFirst("^(thanh pho|tinh)\\s+", "").trim();
        if (!stripped.isEmpty()) {
            keys.add(stripped);
            keys.add("tp " + stripped);
            keys.add("tp. " + stripped);
        }
        return keys;
    }

    private static List<String> candidateDivisionKeys(String name, String... prefixes) {
        String norm = normalize(name);
        Set<String> keys = new LinkedHashSet<>();
        keys.add(norm);
        keys.add(stripLeadingZeroInNumberedUnit(norm));
        for (String prefix : prefixes) {
            if (norm.startsWith(prefix + " ")) {
                String rest = norm.substring(prefix.length() + 1).trim();
                // Do not strip prefix if the remainder is purely digits (e.g. "Quận 1", "Phường 01"),
                // unless matched via normalized numbered unit ("quan 1", "phuong 1")
                if (!rest.matches("\\d+")) {
                    keys.add(rest);
                }
            }
        }
        return List.copyOf(keys);
    }

    private static List<String> lookupVariants(String raw, String... prefixes) {
        String norm = normalize(raw);
        Set<String> variants = new LinkedHashSet<>();
        variants.add(norm);
        variants.add(stripLeadingZeroInNumberedUnit(norm));
        for (String prefix : prefixes) {
            variants.add(prefix + " " + norm);
            variants.add(stripLeadingZeroInNumberedUnit(prefix + " " + norm));
        }
        return List.copyOf(variants);
    }

    private static String stripLeadingZeroInNumberedUnit(String normalized) {
        return normalized.replaceAll("\\b0+(\\d+)\\b", "$1");
    }

    static String normalize(String text) {
        if (text == null) return "";
        String decomposed = Normalizer.normalize(
                text.replace('đ', 'd').replace('Đ', 'D').replace('–', '-'),
                Normalizer.Form.NFD);
        return decomposed.replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s*-\\s*", " - ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static String safe(String input) {
        return input == null ? "" : input.trim();
    }

    private static OrderException invalidAddress(String field, String message) {
        return new OrderException(
                org.springframework.http.HttpStatus.BAD_REQUEST,
                "INVALID_SHIPPING_ADDRESS",
                message,
                Map.of("field", field));
    }
}
