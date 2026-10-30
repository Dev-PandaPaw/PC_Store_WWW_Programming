package iuh.fit.nhom16.pcstore.configuration.security;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class CsrfController {

    @GetMapping("/csrf")
    public Map<String, String> csrf(CsrfToken token) {
        if (token == null) {
            return Map.of();
        }
        return Map.of(
            "headerName", token.getHeaderName(),
            "parameterName", token.getParameterName(),
            "token", token.getToken()
        );
    }
}
