package iuh.fit.nhom16.pcstore.catalog.api.dto;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record StoreProduct(String id, String catalogKey, String slug, String name, String category,
        String brand, String description, long price, long originalPrice, List<Spec> specs,
        int stockOnHand, String image, List<TechnicalSpec> technicalSpecs, Boolean demoCatalog) {
    public record Spec(String label) {}
    public record TechnicalSpec(String label,String value,boolean verified) {}
    public StoreProduct {
        specs=specs==null?List.of():List.copyOf(specs);
        technicalSpecs=technicalSpecs==null?List.of():List.copyOf(technicalSpecs);
        demoCatalog=Boolean.TRUE.equals(demoCatalog);
    }
    public StoreProduct(String id,String catalogKey,String slug,String name,String category,String brand,
                        String description,long price,long originalPrice,List<Spec> specs,int stockOnHand,String image) {
        this(id,catalogKey,slug,name,category,brand,description,price,originalPrice,specs,stockOnHand,image,List.of(),false);
    }
    public String searchable() {
        return name + " " + brand + " " + description + " "
                + String.join(" ", specs.stream().map(Spec::label).toList())+" "
                + String.join(" ",technicalSpecs.stream().map(s->s.label()+" "+s.value()).toList());
    }
}
