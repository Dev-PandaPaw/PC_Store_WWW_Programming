package iuh.fit.nhom16.pcstore.ai;

import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/** Explicit user constraints remain authoritative even when AI is unavailable or misinterprets them. */
public final class CriteriaParser {
    private CriteriaParser() {}
    private static final Map<String, String> ALIASES = new LinkedHashMap<>();
    static {
        ALIASES.put("pc", "\\bpc\\b|bo may|may tinh|cau hinh|workstation|desktop");
        ALIASES.put("cpu", "\\bcpu\\b|vi xu ly|bo xu ly");
        ALIASES.put("vga", "\\bvga\\b|\\bgpu\\b|card (do hoa|man hinh)|\\b(?:rtx|gtx|rx)\\s*\\d{3,4}");
        ALIASES.put("mainboard", "mainboard|bo mach chu|\\bmain\\b");
        ALIASES.put("ram", "\\bram\\b|bo nho");
        ALIASES.put("storage", "\\bssd\\b|\\bhdd\\b|o cung|luu tru");
        ALIASES.put("cooling", "tan nhiet|quat cpu|\\baio\\b");
        ALIASES.put("case", "vo case|vo may|thung may|\\bcase\\b");
        ALIASES.put("psu", "\\bpsu\\b|\\bnguon\\b");
        ALIASES.put("monitor", "man hinh|\\bmonitor\\b");
        ALIASES.put("keyboard", "ban phim|\\bkeyboard\\b");
        ALIASES.put("mouse", "\\bchuot\\b|\\bmouse\\b");
    }
    public static String normalize(String text) {
        return Normalizer.normalize(text.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").replace('đ', 'd');
    }
    private static boolean has(String text, String regex) { return Pattern.compile(regex).matcher(text).find(); }
    public record Budget(Long min, Long max) {}
    private static long amount(String raw, String unit, String tail) {
        double value = Double.parseDouble(raw.replaceAll("\\.(?=\\d{3}(?:\\D|$))", "").replace(',', '.'));
        if (has(unit, "tr|trieu|cu")) value = value*1e6 + (tail == null ? 0 : Double.parseDouble("0."+tail)*1e6);
        else if (has(unit, "k|nghin|ngan")) value *= 1000;
        return Math.round(value);
    }
    public static Budget budget(String message) {
        String text = normalize(message);
        if (has(text, "khong (gioi han|can quan tam).*ngan sach|khong gioi han (gia|tien)|bo gioi han ngan sach"))
            return new Budget(null, null);
        Matcher range = Pattern.compile("(?:tu\\s*)?(\\d+(?:[.,]\\d+)?)\\s*(trieu|tr|cu|k|nghin|ngan)?\\s*(?:-|den|toi)\\s*(\\d+(?:[.,]\\d+)?)\\s*(trieu|tr|cu|k|nghin|ngan)\\b").matcher(text);
        if (range.find()) {
            long a = amount(range.group(1), range.group(2) == null ? range.group(4) : range.group(2), null);
            long b = amount(range.group(3), range.group(4), null);
            if (a > 0 && Math.max(a,b) <= 500_000_000) return new Budget(Math.min(a,b), Math.max(a,b));
        }
        Matcher matcher = Pattern.compile("(\\d+(?:[.,]\\d+)*)\\s*(trieu|tr|cu|nghin|ngan|k|vnd|dong|d|₫)(\\d{1,3})?(?=\\s|$|[,.!?])").matcher(text);
        Long value = null;
        while (matcher.find()) {
            if (matcher.group(2).equals("k") && Set.of("2","4","8").contains(matcher.group(1))
                    && !has(text.substring(Math.max(0,matcher.start()-15),matcher.start()), "ngan sach|gia|duoi|toi da")) continue;
            long n = amount(matcher.group(1),matcher.group(2),matcher.group(3));
            if (n >= 1000 && n <= 500_000_000) value = n;
        }
        if (value == null) {
            Matcher full = Pattern.compile("\\b(\\d{1,3}(?:\\.\\d{3}){2}|\\d{6,9})\\b").matcher(text);
            if (full.find()) { long n = Long.parseLong(full.group(1).replace(".","")); if (n <= 500_000_000) value = n; }
        }
        if (value == null) return null;
        return has(text, "\\b(tren|it nhat|tu)\\b") && !has(text,"duoi|toi da|khong qua|tam|khoang")
                ? new Budget(value,null) : new Budget(null,value);
    }
    public static ProductCriteria parse(String message) { return parse(message, ProductCriteria.empty()); }
    public static boolean hasExplicitGroup(String message) {
        String text = normalize(message);
        return ALIASES.values().stream().anyMatch(regex -> has(text, regex));
    }
    public static ProductCriteria parse(String message, ProductCriteria previous) {
        String text = normalize(message);
        List<String> groups = new ArrayList<>();
        for (var entry : ALIASES.entrySet()) if (has(text,entry.getValue())) groups.add(entry.getKey());
        if (groups.contains("pc") && !has(text,"\\b(va|kem|them)\\b|\\+")) {
            Matcher pc = Pattern.compile(ALIASES.get("pc")).matcher(text);
            pc.find();
            int pcPosition = pc.start();
            // "Chuột máy tính" and "nguồn cho PC" name an accessory first; "PC RTX 5060" names a PC first.
            String primary = "pc";
            int first = pcPosition;
            for (String group : groups) {
                Matcher match = Pattern.compile(ALIASES.get(group)).matcher(text);
                if (match.find() && match.start() < first) { first = match.start(); primary = group; }
            }
            groups = new ArrayList<>(List.of(primary));
        }
        else if (groups.contains("vga") && !groups.contains("pc")) groups.remove("monitor");
        String purpose = has(text,"do hoa|render|dung phim|blender|thiet ke|premiere|3d") ? "graphics"
                : has(text,"van phong|hoc tap|hoc online|excel|word|ke toan") ? "office"
                : has(text,"gaming|choi game|valorant|lien minh|\\blol\\b|\\bcs2\\b|pubg|fps|ban sung") ? "gaming" : null;
        Budget budget = budget(message);
        boolean stock = has(text,"con hang|san hang|co san|mua ngay") || previous.inStockOnly();
        if (has(text,"het hang cung duoc|tham khao|khong can con hang")) stock = false;
        boolean cheaper = has(text,"\\bre\\b|tiet kiem|gia thap|binh dan|sinh vien") || previous.cheaper();
        if (has(text,"khong can re|khong uu tien gia|xem tat ca|nhieu lua chon|so sanh cac")) cheaper = false;
        Long min = budget == null ? previous.minBudget() : budget.min();
        Long max = budget == null ? previous.maxBudget() : budget.max();
        boolean cheapest = cheaper && (has(text,"re nhat|gia thap nhat") || min == null && max == null
                || budget == null && previous.cheapest());
        List<String> keywords = new ArrayList<>();
        Matcher hardware = Pattern.compile("\\b(?:rtx|gtx|rx)\\s*\\d{3,4}(?:\\s*(?:ti|super))?\\b|\\bi[3579][- ]?\\d{4,5}[a-z]*\\b|\\bddr[345]\\b").matcher(text);
        while (hardware.find()) keywords.add(hardware.group().replaceAll("\\s+", ""));
        for (String brand : List.of("intel","amd","logitech","corsair","akko","dareu","zotac","kingston","samsung","asus","msi","deepcool","lian li","cooler master","razer","acer","viewsonic","kingmax"))
            if (text.contains(brand)) keywords.add(brand.replace(" ",""));
        if (has(text,"khong day|wireless|bluetooth")) keywords.add("wireless");
        if (has(text,"mau trang")) keywords.add("white");
        boolean newGroup = !groups.isEmpty();
        if (groups.isEmpty()) groups = new ArrayList<>(previous.groups());
        if (groups.isEmpty() && purpose != null) groups.add("pc");
        return new ProductCriteria(List.copyOf(groups), purpose != null ? purpose : previous.purpose(),
                min, max, stock, cheaper, cheapest, keywords.isEmpty() && !newGroup ? previous.keywords() : List.copyOf(keywords));
    }
    public static boolean unsupported(String query) { return has(normalize(query), "laptop|may tinh xach tay|ban ghe|ghe gaming"); }
}
