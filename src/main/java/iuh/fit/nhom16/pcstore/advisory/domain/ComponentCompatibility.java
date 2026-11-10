package iuh.fit.nhom16.pcstore.advisory.domain;

import iuh.fit.nhom16.pcstore.catalog.api.RecommendationCatalog.Item;
import java.util.*;

/** Socket equality is a basic check, not a BIOS, RAM, power or case certification. */
public final class ComponentCompatibility {
    private ComponentCompatibility() {}
    public static String normalizeSocket(String value) {
        if (value == null) return null;
        String socket = value.toUpperCase(Locale.ROOT).replaceAll("[\\s-]", "");
        return socket.matches("LGA[0-9]{3,4}|AM[345]|FM[12]|TR4|STRX4|STR5") ? socket : null;
    }
    public static boolean socketComponent(Item item) {
        return Set.of("cpu", "mainboard").contains(item.product().category());
    }
    public static boolean contextValid(List<Item> items) {
        var sockets = items.stream().filter(ComponentCompatibility::socketComponent)
                .map(item -> normalizeSocket(item.socket())).toList();
        return !sockets.contains(null) && sockets.stream().distinct().count() <= 1;
    }
    public static String check(Item candidate, List<Item> context) {
        if (!socketComponent(candidate)) return "not_checked";
        if (context.stream().noneMatch(ComponentCompatibility::socketComponent)) return "needs_verification";
        String socket = normalizeSocket(candidate.socket());
        if (socket == null) return "needs_verification";
        for (Item selected : context) {
            if (!socketComponent(selected)) continue;
            String expected = normalizeSocket(selected.socket());
            if (expected == null) return "needs_verification";
            if (!socket.equals(expected)) return "incompatible";
        }
        return "socket_match";
    }
}
