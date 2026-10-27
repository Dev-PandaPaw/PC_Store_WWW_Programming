package iuh.fit.nhom16.pcstore.sales.controller;

import iuh.fit.nhom16.pcstore.sales.dto.OrderDtos.*;
import iuh.fit.nhom16.pcstore.sales.security.OrderActorResolver;
import iuh.fit.nhom16.pcstore.sales.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/orders")
public class AdminOrderController {
    private final OrderService service;
    private final OrderActorResolver actors;
    public AdminOrderController(OrderService service, OrderActorResolver actors) { this.service = service; this.actors = actors; }
    @GetMapping
    public List<Order> list(Authentication auth, HttpServletRequest request, @RequestParam(defaultValue="0") int page) {
        return service.list(actors.resolve(auth, request.getSession()), true, page);
    }
    @GetMapping("/{id}")
    public Order get(Authentication auth, HttpServletRequest request, @PathVariable UUID id) {
        return service.get(actors.resolve(auth, request.getSession()), id, true);
    }
    @PatchMapping("/{id}/status")
    public Order transition(Authentication auth, HttpServletRequest request, @PathVariable UUID id,
                            @Valid @RequestBody TransitionRequest body) {
        return service.transition(actors.resolve(auth, request.getSession()), id, true, body);
    }
    @PatchMapping("/{id}/items/{productId}")
    public Order quantity(Authentication auth, HttpServletRequest request, @PathVariable UUID id, @PathVariable UUID productId,
                          @Valid @RequestBody QuantityRequest body) {
        return service.quantity(actors.resolve(auth, request.getSession()), id, productId, body);
    }
}
