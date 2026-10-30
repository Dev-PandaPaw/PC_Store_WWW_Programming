package iuh.fit.nhom16.pcstore.cart.service;
import iuh.fit.nhom16.pcstore.cart.exception.CartException;
import iuh.fit.nhom16.pcstore.cart.model.CartSession;

import iuh.fit.nhom16.pcstore.catalog.api.dto.CartProductSnapshot;
import iuh.fit.nhom16.pcstore.catalog.infrastructure.persistence.ProductRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.LinkedHashSet;
import java.util.Collection;

import static iuh.fit.nhom16.pcstore.cart.dto.CartDtos.*;

@Service
public class CartService {
    public static final int MAX_DISTINCT_SKUS = 20;
    public static final int MAX_QUANTITY_PER_SKU = 99;

    private final ProductRepository productRepository;

    public CartService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public CartResponse get(CartSession cart) {
        synchronized (cart) {
            return response(cart);
        }
    }

    public CartResponse add(CartSession cart, AddItemRequest request) {
        synchronized (cart) {
            requirePositive(request.quantity());
            requireVersion(cart, request.cartVersion());
            var ids = new LinkedHashSet<>(cart.quantities().keySet());
            ids.add(request.productId());
            var products = products(ids);
            addLine(cart, request, products);
            return response(cart, products);
        }
    }

    // Merge validates each line but builds the response only once, after all lines pass.
    public CartResponse merge(CartSession cart, CartSession guest) {
        synchronized (cart) {
            var ids = new LinkedHashSet<>(cart.quantities().keySet());
            ids.addAll(guest.quantities().keySet());
            var products = products(ids);
            guest.quantities().forEach((id, quantity) ->
                    addLine(cart, new AddItemRequest(id, quantity, cart.version()), products));
            return response(cart, products);
        }
    }

    private void addLine(CartSession cart, AddItemRequest request, Map<UUID, CartProductSnapshot> products) {
        requirePositive(request.quantity());
        requireVersion(cart, request.cartVersion());
        CartProductSnapshot product = product(products, request.productId());
        requireForPurchase(product);

        Integer existing = cart.quantities().get(product.id());
        if (existing == null && cart.quantities().size() >= MAX_DISTINCT_SKUS) {
            throw conflict("CART_ITEM_LIMIT", "Giỏ hàng chỉ hỗ trợ tối đa 20 SKU khác nhau.");
        }
        int nextQuantity = Math.addExact(existing == null ? 0 : existing, request.quantity());
        requireQuantityWithinLimits(product, nextQuantity);
        cart.quantities().put(product.id(), nextQuantity);
        cart.incrementVersion();
    }

    public CartResponse update(CartSession cart, UUID productId, UpdateItemRequest request) {
        synchronized (cart) {
            requirePositive(request.quantity());
            requireVersion(cart, request.cartVersion());
            if (!cart.quantities().containsKey(productId)) {
                throw new CartException(HttpStatus.NOT_FOUND, "CART_ITEM_NOT_FOUND", "Sản phẩm không có trong giỏ hàng.");
            }
            var products = products(cart.quantities().keySet());
            CartProductSnapshot product = product(products, productId);
            requireForPurchase(product);
            requireQuantityWithinLimits(product, request.quantity());
            if (cart.quantities().get(productId).equals(request.quantity())) return response(cart, products);
            cart.quantities().put(productId, request.quantity());
            cart.incrementVersion();
            return response(cart, products);
        }
    }

    public CartResponse remove(CartSession cart, UUID productId, long cartVersion) {
        synchronized (cart) {
            requireVersion(cart, cartVersion);
            if (cart.quantities().remove(productId) == null) {
                throw new CartException(HttpStatus.NOT_FOUND, "CART_ITEM_NOT_FOUND", "Sản phẩm không có trong giỏ hàng.");
            }
            cart.incrementVersion();
            return response(cart);
        }
    }

    public CartResponse clear(CartSession cart, long cartVersion) {
        synchronized (cart) {
            requireVersion(cart, cartVersion);
            if (!cart.quantities().isEmpty()) {
                cart.quantities().clear();
                cart.incrementVersion();
            }
            return response(cart);
        }
    }

    private Map<UUID, CartProductSnapshot> products(Collection<UUID> ids) {
        return ids.isEmpty() ? Map.of() : productRepository.findCartProducts(ids).stream()
                .collect(Collectors.toMap(CartProductSnapshot::id, product -> product));
    }

    private CartProductSnapshot product(Map<UUID, CartProductSnapshot> products, UUID id) {
        CartProductSnapshot product = products.get(id);
        if (product == null) throw new CartException(HttpStatus.NOT_FOUND,
                "CART_PRODUCT_NOT_FOUND", "Sản phẩm không tồn tại.");
        return product;
    }

    private void requireVersion(CartSession cart, long expected) {
        if (cart.version() != expected) {
            throw conflict("CART_VERSION_CONFLICT", "Giỏ hàng đã thay đổi ở phiên khác. Vui lòng tải lại giỏ hàng.");
        }
    }

    private void requireForPurchase(CartProductSnapshot product) {
        if (!"ACTIVE".equalsIgnoreCase(product.status())) {
            throw conflict("CART_PRODUCT_UNAVAILABLE", "Sản phẩm hiện không còn được bán.");
        }
    }

    private void requirePositive(Integer quantity) {
        if (quantity == null || quantity < 1 || quantity > 99) {
            throw new CartException(HttpStatus.BAD_REQUEST, "CART_INVALID_QUANTITY", "Số lượng phải là số nguyên từ 1 đến 99.");
        }
    }

    private void requireQuantityWithinLimits(CartProductSnapshot product, int quantity) {
        int available = available(product);
        if (quantity > MAX_QUANTITY_PER_SKU) {
            throw conflict("CART_QUANTITY_LIMIT", "Mỗi SKU chỉ được mua tối đa 99 sản phẩm.");
        }
        if (quantity > available) {
            throw conflict("CART_INSUFFICIENT_STOCK", "Số lượng yêu cầu vượt quá số lượng khả dụng (" + available + ").");
        }
    }

    private int available(CartProductSnapshot product) {
        return Math.max(0, product.stockOnHand() - product.reservedQuantity());
    }

    private CartException conflict(String code, String message) {
        return new CartException(HttpStatus.CONFLICT, code, message);
    }

    private CartResponse response(CartSession cart) {
        return response(cart, products(cart.quantities().keySet()));
    }

    private CartResponse response(CartSession cart, Map<UUID, CartProductSnapshot> products) {
        List<CartItemResponse> items = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        int totalQuantity = 0;
        for (Map.Entry<UUID, Integer> entry : cart.quantities().entrySet()) {
            CartProductSnapshot product = products.get(entry.getKey());
            int quantity = entry.getValue();
            totalQuantity += quantity;
            if (product == null) {
                items.add(new CartItemResponse(entry.getKey(), null, "Sản phẩm không còn tồn tại", BigDecimal.ZERO,
                        quantity, 0, false));
                continue;
            }
            int available = available(product);
            boolean purchasable = "ACTIVE".equalsIgnoreCase(product.status()) && quantity <= available;
            items.add(new CartItemResponse(product.id(), product.sku(), product.name(), product.price(),
                    quantity, available, purchasable));
            subtotal = subtotal.add(product.price().multiply(BigDecimal.valueOf(quantity)));
        }
        return new CartResponse(cart.version(), List.copyOf(items), totalQuantity, subtotal);
    }
}
