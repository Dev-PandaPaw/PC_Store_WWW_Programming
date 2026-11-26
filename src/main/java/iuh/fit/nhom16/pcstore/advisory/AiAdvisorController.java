package iuh.fit.nhom16.pcstore.advisory;

import iuh.fit.nhom16.pcstore.ai.AiFailure;
import iuh.fit.nhom16.pcstore.ai.AiGateway;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AiAdvisorController {
    private final AiAdvisorService advisor;
    private final AiGateway ai;
    private final JsonMapper json;
    public AiAdvisorController(AiAdvisorService advisor, AiGateway ai, JsonMapper json) {
        this.advisor = advisor; this.ai = ai; this.json = json;
    }
    @GetMapping("/status")
    public ResponseEntity<?> status() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(Map.of(
                "configured",ai.configured(),"model",ai.model(),"maxMessageLength",1200,"maxTurns",8));
    }
    @PostMapping(value = "/chat", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AiAdvisorService.ChatResponse> chat(@RequestBody String body, HttpServletRequest request) {
        if (body.getBytes(StandardCharsets.UTF_8).length > 24*1024)
            throw new AiFailure(413,"BODY_TOO_LARGE","Nội dung gửi quá dài.");
        AiAdvisorService.ChatRequest input;
        try { input = json.readerFor(AiAdvisorService.ChatRequest.class)
                .with(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).readValue(body); }
        catch (RuntimeException e) { throw new AiFailure(400,"INVALID_REQUEST","Nội dung yêu cầu không hợp lệ."); }
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(advisor.advise(input,request.getRemoteAddr()));
    }
}
