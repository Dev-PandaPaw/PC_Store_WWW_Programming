package iuh.fit.nhom16.pcstore.sales.controller;

import iuh.fit.nhom16.pcstore.sales.dto.OrderDtos.*;
import iuh.fit.nhom16.pcstore.sales.model.OrderStatus;
import iuh.fit.nhom16.pcstore.sales.security.OrderActorResolver;
import iuh.fit.nhom16.pcstore.sales.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {
    private final OrderService service;
    private final OrderActorResolver actors;
    public OrderController(OrderService service, OrderActorResolver actors) { this.service = service; this.actors = actors; }
    @GetMapping("/account")
    public Account account(Authentication auth, HttpServletRequest request) {
        return service.account(actors.resolve(auth, request.getSession()));
    }
    @PostMapping("/quotes")
    public Quote quote(Authentication auth, HttpServletRequest request, @Valid @RequestBody QuoteRequest body) {
        return service.quote(actors.resolve(auth, request.getSession()), body);
    }
    @PostMapping
    public Order place(Authentication auth, HttpServletRequest request, @RequestHeader("Idempotency-Key") String key,
                       @Valid @RequestBody PlaceRequest body) {
        return service.place(actors.resolve(auth, request.getSession()), key, body);
    }
    @GetMapping
    public List<Order> list(Authentication auth, HttpServletRequest request, @RequestParam(defaultValue="0") int page) {
        return service.list(actors.resolve(auth, request.getSession()), false, page);
    }
    @GetMapping("/{id}")
    public Order get(Authentication auth, HttpServletRequest request, @PathVariable UUID id) {
        return service.get(actors.resolve(auth, request.getSession()), id, false);
    }
    @PostMapping("/{id}/cancel")
    public Order cancel(Authentication auth, HttpServletRequest request, @PathVariable UUID id,
                        @Valid @RequestBody TransitionRequest body) {
        return service.transition(actors.resolve(auth, request.getSession()), id, false,
                new TransitionRequest(body.version(), OrderStatus.CANCELLED, body.reason()));
    }
}
