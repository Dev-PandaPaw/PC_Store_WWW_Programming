package iuh.fit.nhom16.pcstore.sales;

import iuh.fit.nhom16.pcstore.sales.dto.OrderDtos.Shipping;
import iuh.fit.nhom16.pcstore.sales.dto.OrderDtos.ShippingMethod;
import iuh.fit.nhom16.pcstore.sales.exception.OrderException;
import iuh.fit.nhom16.pcstore.sales.service.ShippingPolicy;
import iuh.fit.nhom16.pcstore.sales.service.VietnamAddressRegistry;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OrderShippingTest {
    private static Shipping at(String province, String district, String ward, String street) {
        return new Shipping("An Nguyen", "0912345678", province, ward, street, district);
    }

    private static void assertInvalidAddress(Shipping shipping, String field) {
        OrderException ex = assertThrows(OrderException.class, () -> VietnamAddressRegistry.verify(shipping));
        assertEquals("INVALID_SHIPPING_ADDRESS", ex.code());
        assertEquals(400, ex.status().value());
        assertEquals(field, ex.details().get("field"));
    }

    @Test
    void addressIncludesSelectedDistrictBetweenWardAndProvince() {
        var shipping = new Shipping("An Nguyen", "0912345678", "Hà Nội", "Phường A", "123 Street", "Quận Ba Đình");
        assertEquals("123 Street, Phường A, Quận Ba Đình, Hà Nội", shipping.address());
    }

    @Test
    void validThreeLevelHierarchyReturnsOfficialCodes() {
        var verified = VietnamAddressRegistry.verify(
                at("Thành phố Hồ Chí Minh", "Quận 1", "Phường Bến Nghé", "123 Nguyễn Huệ"));
        assertEquals(79, verified.provinceCode());
        assertEquals(760, verified.districtCode());
        assertNotNull(verified.wardCode());
        assertEquals("123 Nguyễn Huệ, Phường Bến Nghé, Quận 1, Thành phố Hồ Chí Minh", verified.formattedAddress());

        // Short province name typed by older clients still resolves to the same official unit
        assertEquals(1, VietnamAddressRegistry.verify(
                at("Hà Nội", "Quận Ba Đình", "Phường Phúc Xá", "12 Phan Đình Phùng")).provinceCode());
    }

    @Test
    void fakeOrMismatchedAdministrativeUnitsAreRejected() {
        assertInvalidAddress(at("City", "Quận 1", "Phường Bến Nghé", "123 Street"), "province");
        // District belongs to Ha Noi, not Ho Chi Minh City
        assertInvalidAddress(at("Thành phố Hồ Chí Minh", "Quận Ba Đình", "Phường Phúc Xá", "123 Street"), "district");
        // Ward belongs to Quan Ba Dinh, not Quan 1
        assertInvalidAddress(at("Thành phố Hồ Chí Minh", "Quận 1", "Phường Phúc Xá", "123 Street"), "ward");
        assertInvalidAddress(at("Thành phố Hồ Chí Minh", "Quận 1", "Phường Không Tồn Tại", "123 Street"), "ward");
        assertInvalidAddress(at("Thành phố Hồ Chí Minh", null, "Phường Bến Nghé", "123 Street"), "district");
        assertInvalidAddress(at("Thành phố Hồ Chí Minh", "  ", "Phường Bến Nghé", "123 Street"), "district");
        assertInvalidAddress(at("Thành phố Hồ Chí Minh", "Quận 1", "", "123 Street"), "ward");
    }

    @Test
    void instantShippingUsesDistrictCodesNotAddressText() {
        assertEquals(BigDecimal.valueOf(80_000), ShippingPolicy.resolveShippingFee(ShippingMethod.INSTANT,
                at("Thành phố Hồ Chí Minh", "Quận 1", "Phường Bến Nghé", "123 Nguyễn Huệ")));
        assertEquals(BigDecimal.valueOf(80_000), ShippingPolicy.resolveShippingFee(ShippingMethod.INSTANT,
                at("Thành phố Hà Nội", "Quận Ba Đình", "Phường Phúc Xá", "12 Phan Đình Phùng")));

        for (Shipping outside : List.of(
                at("Thành phố Hồ Chí Minh", "Huyện Cần Giờ", "Thị trấn Cần Thạnh", "12 Đường Rừng Sác"),
                at("Thành phố Hà Nội", "Huyện Ba Vì", "Thị trấn Tây Đằng", "12 Quảng Oai"),
                at("Thành phố Đà Nẵng", "Quận Liên Chiểu", "Phường Hòa Khánh Bắc", "123 Tôn Đức Thắng"))) {
            OrderException unsupported = assertThrows(OrderException.class,
                    () -> ShippingPolicy.resolveShippingFee(ShippingMethod.INSTANT, outside));
            assertEquals("SHIPPING_METHOD_UNSUPPORTED", unsupported.code());
            assertEquals(List.of("EXPRESS", "STORE_PICKUP"), unsupported.details().get("supportedShippingMethods"));
            assertEquals(BigDecimal.valueOf(50_000), ShippingPolicy.resolveShippingFee(ShippingMethod.EXPRESS, outside));
            assertEquals(BigDecimal.ZERO, ShippingPolicy.resolveShippingFee(ShippingMethod.STORE_PICKUP, outside));
        }
    }

    @Test
    void streetNamedAfterIslandOnMainlandIsNotTreatedAsIsland() {
        var mainland = at("Thành phố Hồ Chí Minh", "Quận 1", "Phường Đa Kao", "123 Đường Hoàng Sa");
        assertEquals(BigDecimal.valueOf(50_000), ShippingPolicy.resolveShippingFee(ShippingMethod.EXPRESS, mainland));
    }

    @Test
    void offshoreIslandDistrictsRejectHomeDeliveryAndAllowStorePickup() {
        for (Shipping island : List.of(
                at("Thành phố Đà Nẵng", "Huyện Hoàng Sa", "Đảo Hoàng Sa", "Đảo Hoàng Sa"),
                at("Tỉnh Bà Rịa - Vũng Tàu", "Huyện Côn Đảo", "Côn Đảo", "12 Tôn Đức Thắng"),
                at("Tỉnh Khánh Hòa", "Huyện Trường Sa", "Thị trấn Trường Sa", "Đảo Trường Sa Lớn"))) {
            for (ShippingMethod method : List.of(ShippingMethod.EXPRESS, ShippingMethod.INSTANT)) {
                OrderException ex = assertThrows(OrderException.class,
                        () -> ShippingPolicy.resolveShippingFee(method, island));
                assertEquals("SHIPPING_ADDRESS_UNSUPPORTED", ex.code());
            }
            assertEquals(BigDecimal.ZERO, ShippingPolicy.resolveShippingFee(ShippingMethod.STORE_PICKUP, island));
        }
    }

    @Test
    void shippingOptionsAreResolvedFromProvinceAndDistrict() {
        var inner = ShippingPolicy.optionsForLocation("Thành phố Hồ Chí Minh", "Quận 1");
        assertTrue(inner.stream().filter(o -> o.method() == ShippingMethod.INSTANT).findFirst().orElseThrow().available());

        var rural = ShippingPolicy.optionsForLocation("Thành phố Hồ Chí Minh", "Huyện Cần Giờ");
        assertFalse(rural.stream().filter(o -> o.method() == ShippingMethod.INSTANT).findFirst().orElseThrow().available());

        var island = ShippingPolicy.optionsForLocation("Tỉnh Bà Rịa - Vũng Tàu", "Huyện Côn Đảo");
        assertFalse(island.stream().filter(o -> o.method() == ShippingMethod.EXPRESS).findFirst().orElseThrow().available());
        assertTrue(island.stream().filter(o -> o.method() == ShippingMethod.STORE_PICKUP).findFirst().orElseThrow().available());
    }
}
