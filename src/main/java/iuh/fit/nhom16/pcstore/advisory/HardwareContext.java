package iuh.fit.nhom16.pcstore.advisory;

import iuh.fit.nhom16.pcstore.ai.AiFailure;
import iuh.fit.nhom16.pcstore.ai.ProductCriteria;
import java.util.*;

/** Extracted reference data, deliberately separate from the purchase request. */
public record HardwareContext(String use,List<Part> components,String filename) {
    public record Part(String category,String model,String socket,String memoryType,int capacityGb,int wattageW,String confidence) {}
    public static void validate(HardwareContext context) {
        if(context==null) return;
        if(!Set.of("current","desired").contains(Objects.toString(context.use(),"")) || !text(context.filename(),160,true)) throw invalid();
        validateParts(context.components(),false);
    }
    static void validateParts(List<Part> parts,boolean emptyAllowed) {
        if(parts==null || parts.size()>12 || !emptyAllowed && parts.isEmpty()) throw invalid();
        Set<String> groups=new HashSet<>();
        for(Part part:parts) {
            if(part==null || part.category()==null || part.category().equals("pc") || !ProductCriteria.GROUPS.containsKey(part.category())
                    || !groups.add(part.category()) || !text(part.model(),160,false) || !text(part.socket(),24,true)
                    || !Set.of("","DDR3","DDR4","DDR5").contains(Objects.toString(part.memoryType(),"?"))
                    || part.capacityGb()<0 || part.capacityGb()>100000 || part.wattageW()<0 || part.wattageW()>5000
                    || !Set.of("high","low").contains(Objects.toString(part.confidence(),""))) throw invalid();
        }
    }
    static boolean text(String value,int max,boolean blankAllowed) {
        return value!=null && value.length()<=max && (blankAllowed || !value.isBlank()) && value.chars().noneMatch(c->c<32 || c==127);
    }
    private static AiFailure invalid() { return new AiFailure(400,"INVALID_HARDWARE_CONTEXT","Thông số máy không hợp lệ. Hãy kiểm tra lại các mục đã nhập."); }
}
