package iuh.fit.nhom16.pcstore.pcbuild;

import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class PcBuildDtos {
    private PcBuildDtos() {}
    public record AdviceInput(@NotBlank @Size(max=2000) String question,
            @NotNull @DecimalMin("0") @Digits(integer=16, fraction=2) BigDecimal budgetVnd,
            @NotNull Boolean requireDedicatedGpu, @NotNull Boolean includeRag,
            @Size(max=20) List<@NotNull UUID> replaceProductIds,
            @Min(1) @Max(4096) Integer minRamGb, @Min(1) @Max(100000) Integer minStorageGb) {
        public AdviceInput(String question, BigDecimal budgetVnd, Boolean requireDedicatedGpu,
                           Boolean includeRag, List<UUID> replaceProductIds) {
            this(question, budgetVnd, requireDedicatedGpu, includeRag, replaceProductIds, null, null);
        }
    }
    public record ConfirmInput(@NotNull UUID proposalId, @NotNull @AssertTrue Boolean confirmed,
                               Boolean acknowledgeUnverified) {}
    public record Line(UUID productId, String name, int quantity, BigDecimal unitPrice,
                       long productVersion, boolean kept) implements Serializable {}
    public record ProposalView(UUID proposalId, String group, int optionIndex, Instant expiresAt,
            List<Line> items, List<UUID> removedProductIds, BigDecimal subtotalVnd,
            boolean requiresVerification) {}
    public record AdviceView(Map<String,Object> advice, List<ProposalView> proposals, long cartVersion) {}
}
