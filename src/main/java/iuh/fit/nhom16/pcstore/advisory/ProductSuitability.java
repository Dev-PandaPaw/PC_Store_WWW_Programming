package iuh.fit.nhom16.pcstore.advisory;

import iuh.fit.nhom16.pcstore.catalog.domain.Product;
import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "product_suitability")
public class ProductSuitability {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "need_profile", nullable = false)
    private String needProfile;

    @Column(nullable = false)
    private Short score = 100;

    @Column(name = "suitability_reason")
    private String suitabilityReason;

    @Column
    private String limitation;

    public ProductSuitability() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
    public String getNeedProfile() { return needProfile; }
    public void setNeedProfile(String needProfile) { this.needProfile = needProfile; }
    public Short getScore() { return score; }
    public void setScore(Short score) { this.score = score; }
    public String getSuitabilityReason() { return suitabilityReason; }
    public void setSuitabilityReason(String suitabilityReason) { this.suitabilityReason = suitabilityReason; }
    public String getLimitation() { return limitation; }
    public void setLimitation(String limitation) { this.limitation = limitation; }
}
