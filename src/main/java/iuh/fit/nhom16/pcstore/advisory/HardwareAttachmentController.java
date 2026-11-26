package iuh.fit.nhom16.pcstore.advisory;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/ai/attachments")
public class HardwareAttachmentController {
    private final HardwareAttachmentService service;
    public HardwareAttachmentController(HardwareAttachmentService service) { this.service=service; }
    @PostMapping(value="/analyze",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<HardwareAttachmentService.Extraction> analyze(@RequestParam("file") MultipartFile file,HttpServletRequest request) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.analyze(file,request.getRemoteAddr()));
    }
}
