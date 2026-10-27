package iuh.fit.nhom16.pcstore.payment.controller;

import iuh.fit.nhom16.pcstore.payment.service.VnpayIpnService;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments/vnpay")
public class VnpayController {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(VnpayController.class);
    private final VnpayIpnService service;
    public VnpayController(VnpayIpnService service) { this.service = service; }
    @GetMapping("/ipn")
    public Map<String,String> ipn(@RequestParam MultiValueMap<String,String> parameters) {
        if (parameters.values().stream().anyMatch(values -> values.size() != 1)) return Map.of("RspCode", "97", "Message", "Ambiguous parameters");
        try { return service.handle(parameters.toSingleValueMap()); }
        catch (RuntimeException ex) {
            // The service transaction has rolled back; ask the gateway to retry without exposing SQL/PII.
            log.warn("VNPAY IPN transaction failed ({})", ex.getClass().getSimpleName());
            return Map.of("RspCode", "99", "Message", "Please retry");
        }
    }
}
