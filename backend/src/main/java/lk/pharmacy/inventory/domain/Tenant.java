package lk.pharmacy.inventory.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "tenants")
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 80)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(nullable = false)
    private boolean billingEnabled = true;

    @Column(nullable = false)
    private boolean transactionsEnabled = true;

    @Column(nullable = false)
    private boolean inventoryEnabled = true;

    @Column(nullable = false)
    private boolean analyticsEnabled = true;

    @Column(nullable = false)
    private boolean aiAssistantEnabled = true;

    @ManyToOne
    @JoinColumn(name = "default_pharmacy_id")
    private Pharmacy defaultPharmacy;

    @Column(length = 255)
    private String logoFileName;

    @Column(length = 120)
    private String logoContentType;

    @Lob
    @Column(columnDefinition = "BYTEA")
    private byte[] logoData;

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isBillingEnabled() {
        return billingEnabled;
    }

    public void setBillingEnabled(boolean billingEnabled) {
        this.billingEnabled = billingEnabled;
    }

    public boolean isTransactionsEnabled() {
        return transactionsEnabled;
    }

    public void setTransactionsEnabled(boolean transactionsEnabled) {
        this.transactionsEnabled = transactionsEnabled;
    }

    public boolean isInventoryEnabled() {
        return inventoryEnabled;
    }

    public void setInventoryEnabled(boolean inventoryEnabled) {
        this.inventoryEnabled = inventoryEnabled;
    }

    public boolean isAnalyticsEnabled() {
        return analyticsEnabled;
    }

    public void setAnalyticsEnabled(boolean analyticsEnabled) {
        this.analyticsEnabled = analyticsEnabled;
    }

    public boolean isAiAssistantEnabled() {
        return aiAssistantEnabled;
    }

    public void setAiAssistantEnabled(boolean aiAssistantEnabled) {
        this.aiAssistantEnabled = aiAssistantEnabled;
    }

    public Pharmacy getDefaultPharmacy() {
        return defaultPharmacy;
    }

    public void setDefaultPharmacy(Pharmacy defaultPharmacy) {
        this.defaultPharmacy = defaultPharmacy;
    }

    public String getLogoFileName() {
        return logoFileName;
    }

    public void setLogoFileName(String logoFileName) {
        this.logoFileName = logoFileName;
    }

    public String getLogoContentType() {
        return logoContentType;
    }

    public void setLogoContentType(String logoContentType) {
        this.logoContentType = logoContentType;
    }

    public byte[] getLogoData() {
        return logoData;
    }

    public void setLogoData(byte[] logoData) {
        this.logoData = logoData;
    }
}

