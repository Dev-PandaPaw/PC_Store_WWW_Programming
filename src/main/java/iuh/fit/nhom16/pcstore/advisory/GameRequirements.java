package iuh.fit.nhom16.pcstore.advisory;

import iuh.fit.nhom16.pcstore.ai.AiGateway;
import java.util.List;

public record GameRequirements(boolean found, String title, Hardware minimum, Hardware recommended,
        List<AiGateway.Source> sources, String checkedAt, String lookupStatus) {
    public record Hardware(String cpu, String gpu, int ramGb, int storageGb, boolean ssdRequired, String notes) {
        public boolean valid() { return cpu != null && !cpu.isBlank() && cpu.length() <= 300
                && gpu != null && !gpu.isBlank() && gpu.length() <= 300
                && ramGb > 0 && ramGb <= 256 && storageGb > 0 && storageGb <= 2000
                && notes != null && notes.length() <= 800; }
    }
    /** Model only supplies facts; source links and lookup dates are assigned by the server. */
    public record Lookup(boolean found, String title, Hardware minimum, Hardware recommended) {}
    public boolean valid() { return found && title != null && !title.isBlank() && title.length() <= 150
            && minimum != null && minimum.valid() && recommended != null && recommended.valid()
            && sources != null && !sources.isEmpty() && sources.stream().allMatch(s -> AiGateway.safeSource(s.url())); }
}
