package iuh.fit.nhom16.pcstore.advisory;

import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/advisory")
public class AdvisoryController {

    private final ProductSuitabilityRepository suitabilityRepository;

    public AdvisoryController(ProductSuitabilityRepository suitabilityRepository) {
        this.suitabilityRepository = suitabilityRepository;
    }

    @GetMapping("/recommendations")
    public List<ProductSuitability> getRecommendations(
            @RequestParam(defaultValue = "GAMING") String purpose,
            @RequestParam(required = false) BigDecimal budget
    ) {
        return suitabilityRepository.findRecommendations(purpose, budget);
    }
}
