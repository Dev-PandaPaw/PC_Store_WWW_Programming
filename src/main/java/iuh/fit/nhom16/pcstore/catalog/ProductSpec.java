package iuh.fit.nhom16.pcstore.catalog;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "product_specs")
public class ProductSpec {

    @Id
    @Column(name = "product_id")
    private UUID productId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "product_id")
    @JsonIgnore
    private Product product;

    private String cpu;
    private String gpu;

    @Column(name = "ram_gb")
    private Integer ramGb;

    @Column(name = "storage_gb")
    private Integer storageGb;

    @Column(name = "storage_type")
    private String storageType;

    private String motherboard;
    private String psu;

    @Column(name = "case_name")
    private String caseName;

    @Column(name = "operating_system")
    private String operatingSystem;

    public ProductSpec() {}

    public UUID getProductId() { return productId; }
    public void setProductId(UUID productId) { this.productId = productId; }
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
    public String getCpu() { return cpu; }
    public void setCpu(String cpu) { this.cpu = cpu; }
    public String getGpu() { return gpu; }
    public void setGpu(String gpu) { this.gpu = gpu; }
    public Integer getRamGb() { return ramGb; }
    public void setRamGb(Integer ramGb) { this.ramGb = ramGb; }
    public Integer getStorageGb() { return storageGb; }
    public void setStorageGb(Integer storageGb) { this.storageGb = storageGb; }
    public String getStorageType() { return storageType; }
    public void setStorageType(String storageType) { this.storageType = storageType; }
    public String getMotherboard() { return motherboard; }
    public void setMotherboard(String motherboard) { this.motherboard = motherboard; }
    public String getPsu() { return psu; }
    public void setPsu(String psu) { this.psu = psu; }
    public String getCaseName() { return caseName; }
    public void setCaseName(String caseName) { this.caseName = caseName; }
    public String getOperatingSystem() { return operatingSystem; }
    public void setOperatingSystem(String operatingSystem) { this.operatingSystem = operatingSystem; }
}
