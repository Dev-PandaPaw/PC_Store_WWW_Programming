package iuh.fit.nhom16.pcstore.payment.web;

import iuh.fit.nhom16.pcstore.payment.application.PaymentService;
import iuh.fit.nhom16.pcstore.payment.infrastructure.vnpay.VNPayConfig;
import iuh.fit.nhom16.pcstore.sales.security.OrderActorResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller xử lý thanh toán VNPay Sandbox.
 *
 * <ul>
 *   <li>{@code POST /api/v1/payments/vnpay/create-url} — frontend gọi để lấy URL redirect sang VNPay</li>
 *   <li>{@code GET  /api/v1/payments/vnpay/callback}   — VNPay redirect trình duyệt về đây sau thanh toán</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/payments/vnpay")
public class PaymentController {

    private final PaymentService paymentService;
    private final VNPayConfig vnPayConfig;
    private final OrderActorResolver actorResolver;

    public PaymentController(PaymentService paymentService,
                             VNPayConfig vnPayConfig,
                             OrderActorResolver actorResolver) {
        this.paymentService = paymentService;
        this.vnPayConfig = vnPayConfig;
        this.actorResolver = actorResolver;
    }

    @PostMapping("/create-url")
    public ResponseEntity<CreateUrlResponse> createPaymentUrl(
            @Valid @RequestBody CreateUrlRequest requestBody,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        UUID actorId = actorResolver.resolve(authentication, httpRequest.getSession());
        String paymentUrl = paymentService.createVnPayPaymentUrl(
                actorId,
                requestBody.orderId(),
                requestBody.amount(),
                requestBody.idempotencyKey(),
                httpRequest
        );
        return ResponseEntity.ok(new CreateUrlResponse(paymentUrl));
    }

    @GetMapping("/callback")
    public RedirectView handleVnPayCallback(@RequestParam Map<String, String> allParams) {
        PaymentService.CallbackResult result = paymentService.processVnPayCallback(allParams);

        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(vnPayConfig.getReturnUrl())
                .queryParam("result", result.success() ? "success" : "failed");

        if (result.txnRef() != null && !result.txnRef().isBlank()) {
            builder.queryParam("txnRef", result.txnRef());
        }
        if (result.orderId() != null) {
            builder.queryParam("orderId", result.orderId());
        }
        if (result.responseCode() != null && !result.responseCode().isBlank()) {
            builder.queryParam("vnp_ResponseCode", result.responseCode());
        }
        if (result.transactionNo() != null && !result.transactionNo().isBlank()) {
            builder.queryParam("vnp_TransactionNo", result.transactionNo());
        }
        if (result.bankCode() != null && !result.bankCode().isBlank()) {
            builder.queryParam("vnp_BankCode", result.bankCode());
        }
        if (result.vnpAmount() != null && !result.vnpAmount().isBlank()) {
            builder.queryParam("vnp_Amount", result.vnpAmount());
        }

        RedirectView redirectView = new RedirectView(builder.build().encode().toUriString());
        redirectView.setExposeModelAttributes(false);
        return redirectView;
    }

    public record CreateUrlRequest(
            @NotNull(message = "orderId không được để trống")
            UUID orderId,

            @NotNull(message = "amount không được để trống")
            @Positive(message = "amount phải là số dương")
            BigDecimal amount,

            @NotBlank(message = "idempotencyKey không được để trống")
            String idempotencyKey
    ) {}

    public record CreateUrlResponse(String paymentUrl) {}
}
