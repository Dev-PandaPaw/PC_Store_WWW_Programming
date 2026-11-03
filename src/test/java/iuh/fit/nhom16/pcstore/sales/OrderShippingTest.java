package iuh.fit.nhom16.pcstore.sales;

import iuh.fit.nhom16.pcstore.sales.dto.OrderDtos.Shipping;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class OrderShippingTest {
    @Test void addressIncludesSelectedDistrictBetweenWardAndProvince() {
        var shipping = new Shipping("An Nguyen", "0912345678", "Hà Nội", "Phường A", "123 Street", "Quận Ba Đình");
        assertEquals("123 Street, Phường A, Quận Ba Đình, Hà Nội", shipping.address());
    }

    @Test void olderClientsWithoutDistrictStillProduceValidAddresses() {
        var shipping = new Shipping("An Nguyen", "0912345678", "Hà Nội", "Phường A", "123 Street");
        assertEquals("123 Street, Phường A, Hà Nội", shipping.address());
    }
}
