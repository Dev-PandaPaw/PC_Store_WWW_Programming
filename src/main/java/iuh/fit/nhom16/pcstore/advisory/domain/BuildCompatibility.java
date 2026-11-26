package iuh.fit.nhom16.pcstore.advisory.domain;

import iuh.fit.nhom16.pcstore.catalog.api.RecommendationCatalog.Item;
import java.util.*;

/** Catalog checks for one NEW unit per category. A passed check has evidence for every field it uses.
 * It does not certify the installed BIOS, memory overclock, measured load or AIO routing. */
public final class BuildCompatibility {
    private BuildCompatibility() {}
    public record Result(String label, String status, String detail) {}
    private record Field(Item item, String key) {}
    private static final Map<String,String> LABELS=Map.ofEntries(
            Map.entry("socket","socket"),Map.entry("family","dòng CPU"),Map.entry("supported_cpu_families","dòng CPU mainboard hỗ trợ"),
            Map.entry("memory_type","chuẩn bộ nhớ"),Map.entry("ram_type","chuẩn RAM"),Map.entry("supported_memory_types","chuẩn RAM CPU hỗ trợ"),
            Map.entry("memory_form_factor","loại khe RAM"),Map.entry("form_factor","khổ linh kiện"),Map.entry("capacity_gb","dung lượng RAM"),
            Map.entry("max_memory_gb","dung lượng RAM tối đa"),Map.entry("module_count","số thanh RAM"),Map.entry("dimm_slots","số khe RAM"),
            Map.entry("wattage_w","công suất PSU"),Map.entry("recommended_psu_w","mức nguồn hãng GPU khuyến nghị"),
            Map.entry("pcie_8pin_count","số đầu PCIe 8-pin"),Map.entry("pcie_16pin_count","số đầu PCIe 16-pin"),
            Map.entry("eps_8pin_count","số đầu EPS CPU 8-pin"),Map.entry("atx_24pin_count","số đầu ATX 24-pin"),
            Map.entry("length_mm","chiều dài linh kiện"),Map.entry("max_gpu_length_mm","giới hạn chiều dài GPU"),
            Map.entry("max_psu_length_mm","giới hạn chiều dài PSU"),Map.entry("supported_motherboard_form_factors","khổ mainboard vỏ hỗ trợ"),
            Map.entry("supported_psu_form_factors","khổ PSU vỏ hỗ trợ"),Map.entry("cooler_type","loại tản nhiệt"),
            Map.entry("supported_sockets","socket tản hỗ trợ"),Map.entry("height_mm","chiều cao linh kiện"),
            Map.entry("max_cooler_height_mm","giới hạn chiều cao tản CPU"),Map.entry("ram_clearance_unrestricted","khoảng trống khe RAM"),
            Map.entry("ram_clearance_mm","khoảng trống chiều cao RAM dưới tản"),Map.entry("interface","chuẩn kết nối ổ"),
            Map.entry("storage_interfaces","chuẩn ổ mainboard hỗ trợ"),Map.entry("m2_form_factor","kích thước SSD M.2"),
            Map.entry("m2_form_factors","kích thước M.2 mainboard hỗ trợ"),Map.entry("m2_slots","số khe M.2"));

    private static Item item(List<Item> items,String category) {
        return items.stream().filter(i->i.product().category().equals(category)).findFirst().orElse(null);
    }
    private static Object raw(Item item,String key) { return item==null ? null : item.componentSpecs().get(key); }
    private static String text(Item item,String key) {
        Object value=raw(item,key);
        return value==null || value.toString().isBlank() ? null : norm(value.toString());
    }
    private static String norm(String s) {
        return s.toUpperCase(Locale.ROOT).replaceAll("[\\s-]", "").replace("MICROATX","MATX");
    }
    private static Set<String> values(Item item,String key) {
        Object value=raw(item,key);
        if(value==null) return null;
        Set<String> result=new HashSet<>();
        if(value instanceof Collection<?> list) list.forEach(v->{ if(v!=null) result.add(norm(v.toString())); });
        else for(String part:value.toString().replaceAll("[\\[\\]\"']", "").split("[,;/|]+")) result.add(norm(part));
        result.remove("");
        return result.isEmpty() ? null : result;
    }
    private static Double number(Item item,String key) {
        Object value=raw(item,key);
        if(value==null) return null;
        try { double n=Double.parseDouble(value.toString());return Double.isFinite(n)&&n>=0?n:null; }
        catch(NumberFormatException ignored) { return null; }
    }
    private static boolean greater(Item a,String ak,Item b,String bk) {
        Double x=number(a,ak),y=number(b,bk);return x!=null&&y!=null&&x>y;
    }
    private static boolean different(String a,String b) { return a!=null&&b!=null&&!a.equals(b); }
    private static boolean unsupported(Item host,String key,String value) {
        Set<String> list=values(host,key);return list!=null&&value!=null&&!list.contains(value);
    }
    private static String socket(Item i) {
        if(i==null) return null;
        return ComponentCompatibility.normalizeSocket(i.socket()!=null?i.socket():Objects.toString(raw(i,"socket"),null));
    }
    private static String memory(Item i) {
        String s=text(i,"memory_type");return s!=null?s:text(i,"ram_type");
    }

    /** Also used while enumerating partial builds: absent fields do not assert incompatibility. */
    public static boolean valid(List<Item> items) {
        Item cpu=item(items,"cpu"),board=item(items,"mainboard"),ram=item(items,"ram"),gpu=item(items,"vga"),
                psu=item(items,"psu"),pcCase=item(items,"case"),cooler=item(items,"cooling"),ssd=item(items,"storage");
        if(different(socket(cpu),socket(board)) || different(memory(board),memory(ram))) return false;
        if(unsupported(board,"supported_cpu_families",text(cpu,"family")) || unsupported(cpu,"supported_memory_types",memory(ram))) return false;
        if(different(text(board,"memory_form_factor"),text(ram,"form_factor"))
                || greater(ram,"capacity_gb",board,"max_memory_gb") || greater(ram,"module_count",board,"dimm_slots")) return false;
        if(greater(gpu,"recommended_psu_w",psu,"wattage_w") || greater(gpu,"pcie_8pin_count",psu,"pcie_8pin_count")
                || greater(gpu,"pcie_16pin_count",psu,"pcie_16pin_count") || greater(board,"eps_8pin_count",psu,"eps_8pin_count")
                || greater(board,"atx_24pin_count",psu,"atx_24pin_count")) return false;
        if(greater(gpu,"length_mm",pcCase,"max_gpu_length_mm") || greater(psu,"length_mm",pcCase,"max_psu_length_mm")) return false;
        if(unsupported(pcCase,"supported_motherboard_form_factors",text(board,"form_factor"))
                || unsupported(pcCase,"supported_psu_form_factors",text(psu,"form_factor"))) return false;
        if(greater(cooler,"height_mm",pcCase,"max_cooler_height_mm")
                || greater(cpu,"max_power_w",cooler,"max_tdp_w")
                || unsupported(cooler,"supported_sockets",socket(cpu))) return false;
        if(!Boolean.TRUE.equals(raw(cooler,"ram_clearance_unrestricted")) && greater(ram,"height_mm",cooler,"ram_clearance_mm")) return false;
        Double radiator=number(cooler,"radiator_size_mm");
        if(radiator!=null && unsupported(pcCase,"supported_radiator_sizes_mm",String.valueOf(radiator.intValue()))) return false;
        if(unsupported(board,"storage_interfaces",text(ssd,"interface"))) return false;
        if("NVME".equals(text(ssd,"interface"))) {
            if(unsupported(board,"m2_form_factors",text(ssd,"m2_form_factor"))) return false;
            Double count=number(board,"m2_slots");if(count!=null && count<1) return false;
        }
        return true;
    }
    private static Field f(Item i,String key) { return new Field(i,key); }
    private static Field mem(Item i) { return f(i,text(i,"memory_type")!=null?"memory_type":"ram_type"); }
    private static boolean evidenced(Field field) {
        Object value=raw(field.item(),field.key());
        if(value==null || value.toString().isBlank() || value instanceof Collection<?> list && list.isEmpty()) return false;
        String key=field.key();
        if(key.endsWith("_w") || key.endsWith("_mm") || key.endsWith("_gb") || key.endsWith("_count") || key.endsWith("_slots")) {
            Double n=number(field.item(),key);
            if(n==null || n==0 && !Set.of("pcie_8pin_count","pcie_16pin_count").contains(key)) return false;
        }
        if(key.equals("socket") && ComponentCompatibility.normalizeSocket(value.toString())==null) return false;
        Object verified=raw(field.item(),"verified_fields");
        return verified instanceof Collection<?> fields && fields.contains(field.key());
    }
    private static Result check(String label,String detail,Field... fields) {
        List<String> missing=Arrays.stream(fields).filter(f->!evidenced(f)).map(f->
                (f.item()==null ? "linh kiện" : f.item().product().name())+": "+LABELS.getOrDefault(f.key(),f.key())).distinct().toList();
        return new Result(label,missing.isEmpty()?"passed":"needs_verification",missing.isEmpty()?detail:
                "Cần đối chiếu thông số: "+String.join("; ",missing)+".");
    }

    public static List<Result> checks(List<Item> items) {
        Item cpu=item(items,"cpu"),board=item(items,"mainboard"),ram=item(items,"ram"),gpu=item(items,"vga"),
                psu=item(items,"psu"),pcCase=item(items,"case"),cooler=item(items,"cooling"),ssd=item(items,"storage");
        List<Result> checks=new ArrayList<>();
        checks.add(check("CPU và mainboard","Cùng socket "+socket(cpu)+" và dòng CPU được mainboard hỗ trợ theo hãng. Đối chiếu BIOS thực tế trước khi lắp.",
                f(cpu,"socket"),f(cpu,"family"),f(board,"socket"),f(board,"supported_cpu_families")));
        checks.add(check("RAM: chuẩn, dung lượng và số thanh","RAM cùng chuẩn "+memory(ram)+", đúng loại khe; số thanh và dung lượng nằm trong giới hạn mainboard. XMP/EXPO cần kiểm tra riêng.",
                mem(board),mem(ram),f(cpu,"supported_memory_types"),f(board,"memory_form_factor"),f(ram,"form_factor"),
                f(ram,"capacity_gb"),f(board,"max_memory_gb"),f(ram,"module_count"),f(board,"dimm_slots")));
        checks.add(check("Nguồn và đầu cấp điện","Công suất nguồn đạt mức hãng GPU khuyến nghị; đủ đầu ATX, EPS và PCIe theo danh mục. Dùng đúng cáp đi kèm PSU. Công suất tải thực tế chưa được đo.",
                f(gpu,"recommended_psu_w"),f(psu,"wattage_w"),f(gpu,"pcie_8pin_count"),f(psu,"pcie_8pin_count"),
                f(gpu,"pcie_16pin_count"),f(psu,"pcie_16pin_count"),f(board,"eps_8pin_count"),f(psu,"eps_8pin_count"),
                f(board,"atx_24pin_count"),f(psu,"atx_24pin_count")));
        checks.add(check("Chiều dài GPU, PSU và khổ mainboard","Chiều dài GPU/PSU và khổ mainboard nằm trong giới hạn vỏ. Kiểm tra thêm lối đi cáp và vị trí quạt khi lắp thực tế.",
                f(gpu,"length_mm"),f(pcCase,"max_gpu_length_mm"),f(board,"form_factor"),f(pcCase,"supported_motherboard_form_factors"),
                f(psu,"form_factor"),f(pcCase,"supported_psu_form_factors"),f(psu,"length_mm"),f(pcCase,"max_psu_length_mm")));
        if("AIR".equals(text(cooler,"cooler_type"))) {
            List<Field> fields=new ArrayList<>(List.of(f(cooler,"cooler_type"),f(cooler,"supported_sockets"),f(cpu,"socket"),
                    f(cooler,"height_mm"),f(pcCase,"max_cooler_height_mm")));
            if(Boolean.TRUE.equals(raw(cooler,"ram_clearance_unrestricted"))) fields.add(f(cooler,"ram_clearance_unrestricted"));
            else { fields.add(f(cooler,"ram_clearance_mm"));fields.add(f(ram,"height_mm")); }
            // A measured thermal result cannot be inferred from cross-vendor marketing TDP figures.
            checks.add(check("Socket và khoảng trống tản khí","Tản hỗ trợ socket CPU, đủ chiều cao trong vỏ và khoảng trống RAM. Nhiệt độ phụ thuộc giới hạn công suất CPU và luồng gió.",fields.toArray(Field[]::new)));
        } else {
            checks.add(new Result("Tản nhiệt và radiator","needs_verification",
                    "Cần đối chiếu revision bộ gá, vị trí, chiều dài và tổng độ dày radiator + quạt. Cỡ radiator danh nghĩa chưa đủ để xác nhận lắp vừa."));
        }
        if(!"NVME".equals(text(ssd,"interface"))) {
            checks.add(new Result("Kết nối ổ lưu trữ","needs_verification","Cần xác minh kiểu ổ, cổng và dây SATA hoặc chuẩn M.2 tương ứng."));
        } else checks.add(check("SSD M.2 NVMe","Một SSD NVMe đúng kích thước M.2 được mainboard hỗ trợ. PCIe khác thế hệ chạy theo tốc độ chung thấp hơn; kiểm tra chia sẻ làn nếu lắp thêm ổ.",
                f(ssd,"interface"),f(board,"storage_interfaces"),f(ssd,"m2_form_factor"),f(board,"m2_form_factors"),f(board,"m2_slots")));
        return List.copyOf(checks);
    }
}
