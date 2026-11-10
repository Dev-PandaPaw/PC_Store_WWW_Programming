package iuh.fit.nhom16.pcstore.advisory.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Integration port for Nhan's behavior service. Return category slugs ranked by recent views.
 * Implementations must scope all events to userId and since, and return at most limit entries. */
public interface RecentCategoryProvider {
    List<String> topCategories(UUID userId, Instant since, int limit);
}
