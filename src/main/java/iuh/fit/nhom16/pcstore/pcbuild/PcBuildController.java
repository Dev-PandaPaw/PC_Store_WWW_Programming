package iuh.fit.nhom16.pcstore.pcbuild;

import iuh.fit.nhom16.pcstore.cart.security.CartOwnerResolver;
import iuh.fit.nhom16.pcstore.cart.dto.CartDtos.CartResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import static iuh.fit.nhom16.pcstore.pcbuild.PcBuildDtos.*;

@RestController
@RequestMapping("/api/v1/pc-build")
public class PcBuildController {
    private final PcBuildService builds;
    private final CartOwnerResolver owners;
    public PcBuildController(PcBuildService builds, CartOwnerResolver owners) { this.builds = builds; this.owners = owners; }
    @PostMapping("/advice")
    public AdviceView advice(HttpSession session, Authentication authentication, @Valid @RequestBody AdviceInput input) {
        return builds.advice(session, owners.resolve(authentication, session), input);
    }
    @PostMapping("/confirm")
    public CartResponse confirm(HttpSession session, Authentication authentication, @Valid @RequestBody ConfirmInput input) {
        return builds.confirm(session, owners.resolve(authentication, session), input);
    }
}
