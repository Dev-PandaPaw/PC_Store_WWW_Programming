package iuh.fit.nhom16.pcstore.advisory;

import iuh.fit.nhom16.pcstore.advisory.api.RecentCategoryProvider;
import iuh.fit.nhom16.pcstore.tracking.service.UserTrackingService;
import iuh.fit.nhom16.pcstore.tracking.repository.UserActivityLogRepository;
import iuh.fit.nhom16.pcstore.tracking.infrastructure.RecentCategoryConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.domain.PageRequest;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Verifies the recommendation adapter uses development's existing tracking service. */
class ProductViewHistoryTest {
    @Test void historyReadIsScopedToUserWindowAndLimit() {
        var repository=mock(UserActivityLogRepository.class);
        var history=new UserTrackingService(repository);
        UUID user=UUID.randomUUID(); Instant since=Instant.parse("2026-10-10T00:00:00Z");
        when(repository.findTopViewedCategoriesByUser(user,since.atOffset(ZoneOffset.UTC),PageRequest.of(0,3)))
                .thenReturn(List.of("cpu","mainboard"));
        assertEquals(List.of("cpu","mainboard"),history.getTopViewedCategories(user,since,3));
        verify(repository).findTopViewedCategoriesByUser(user,since.atOffset(ZoneOffset.UTC),PageRequest.of(0,3));
        verifyNoMoreInteractions(repository);
    }
    @Test void invalidQueriesDoNotReadAnotherUsersHistory() {
        var repository=mock(UserActivityLogRepository.class); var history=new UserTrackingService(repository);
        assertThrows(IllegalArgumentException.class,()->history.getTopViewedCategories(null,Instant.now(),3));
        assertThrows(IllegalArgumentException.class,()->history.getTopViewedCategories(UUID.randomUUID(),Instant.now(),100));
        verifyNoInteractions(repository);
    }
    @Test void defaultAdapterDelegatesToExistingServiceAndCanBeReplaced() {
        var history=mock(UserTrackingService.class);
        var runner=new ApplicationContextRunner().withUserConfiguration(RecentCategoryConfiguration.class)
                .withBean(UserTrackingService.class,()->history);
        UUID user=UUID.randomUUID();Instant since=Instant.now();
        when(history.getTopViewedCategories(user,since,3)).thenReturn(List.of("cpu"));
        runner.run(context->assertEquals(List.of("cpu"),context.getBean(RecentCategoryProvider.class).topCategories(user,since,3)));
        runner.withPropertyValues("app.recommendations.behavior-source=external").run(context->
                assertTrue(context.getBeansOfType(RecentCategoryProvider.class).isEmpty()));
    }
}
