package lk.pharmacy.inventory.tenant;

import lk.pharmacy.inventory.domain.Tenant;
import lk.pharmacy.inventory.exception.ApiException;
import lk.pharmacy.inventory.util.CurrentUserService;
import org.springframework.stereotype.Service;

@Service
public class TenantFeatureGuardService {

    private final CurrentUserService currentUserService;

    public TenantFeatureGuardService(CurrentUserService currentUserService) {
        this.currentUserService = currentUserService;
    }

    public void requireInventoryEnabled() {
        Tenant tenant = currentUserService.getCurrentTenantUser().getTenant();
        if (!tenant.isEnabled() || !isInventoryEnabled(tenant)) {
            throw new ApiException("Inventory module is disabled for this tenant.");
        }
    }

    public void requireBillingEnabled() {
        Tenant tenant = currentUserService.getCurrentTenantUser().getTenant();
        if (!tenant.isEnabled() || !isBillingEnabled(tenant)) {
            throw new ApiException("Billing module is disabled for this tenant.");
        }
    }

    public void requireTransactionsEnabled() {
        Tenant tenant = currentUserService.getCurrentTenantUser().getTenant();
        if (!tenant.isEnabled() || !isTransactionsEnabled(tenant)) {
            throw new ApiException("Transactions module is disabled for this tenant.");
        }
    }

    public void requireAnalyticsEnabled() {
        Tenant tenant = currentUserService.getCurrentTenantUser().getTenant();
        if (!tenant.isEnabled() || !isAnalyticsEnabled(tenant)) {
            throw new ApiException("Analytics module is disabled for this tenant.");
        }
    }

    public void requireAiEnabled() {
        Tenant tenant = currentUserService.getCurrentTenantUser().getTenant();
        if (!tenant.isEnabled() || !isAiEnabled(tenant)) {
            throw new ApiException("AI assistant is disabled for this tenant.");
        }
    }

    private boolean isBillingEnabled(Tenant tenant) {
        return isLegacyUnconfigured(tenant) || tenant.isBillingEnabled();
    }

    private boolean isTransactionsEnabled(Tenant tenant) {
        return isLegacyUnconfigured(tenant) || tenant.isTransactionsEnabled();
    }

    private boolean isInventoryEnabled(Tenant tenant) {
        return isLegacyUnconfigured(tenant) || tenant.isInventoryEnabled();
    }

    private boolean isAnalyticsEnabled(Tenant tenant) {
        return isLegacyUnconfigured(tenant) || tenant.isAnalyticsEnabled();
    }

    private boolean isAiEnabled(Tenant tenant) {
        return isLegacyUnconfigured(tenant) || tenant.isAiAssistantEnabled();
    }

    private boolean isLegacyUnconfigured(Tenant tenant) {
        return !tenant.isBillingEnabled()
                && !tenant.isTransactionsEnabled()
                && !tenant.isInventoryEnabled()
                && !tenant.isAnalyticsEnabled()
                && !tenant.isAiAssistantEnabled();
    }
}

