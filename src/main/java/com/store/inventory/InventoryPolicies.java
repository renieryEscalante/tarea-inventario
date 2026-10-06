package com.store.inventory;

import com.store.inventory.api.ProductCategory;
import java.time.Duration;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/** Immutable policies for every supported product category. */
public final class InventoryPolicies {

    private final Map<ProductCategory, CategoryPolicy> policies;

    public InventoryPolicies(Map<ProductCategory, CategoryPolicy> policies) {
        if (policies == null || policies.size() != ProductCategory.values().length) {
            throw new IllegalArgumentException("A policy must be provided for every product category");
        }

        EnumMap<ProductCategory, CategoryPolicy> copy = new EnumMap<>(ProductCategory.class);
        for (ProductCategory category : ProductCategory.values()) {
            CategoryPolicy policy = policies.get(category);
            if (policy == null) {
                throw new IllegalArgumentException("Missing policy for category " + category);
            }
            copy.put(category, policy);
        }
        this.policies = Collections.unmodifiableMap(copy);
    }

    public static InventoryPolicies defaults() {
        EnumMap<ProductCategory, CategoryPolicy> defaults = new EnumMap<>(ProductCategory.class);
        defaults.put(ProductCategory.STANDARD,
                new CategoryPolicy(Duration.ofMinutes(15), java.util.OptionalInt.empty()));
        defaults.put(ProductCategory.PRE_ORDER,
                new CategoryPolicy(Duration.ofHours(24), java.util.OptionalInt.empty()));
        defaults.put(ProductCategory.FLASH_SALE,
                new CategoryPolicy(Duration.ofMinutes(5), java.util.OptionalInt.of(2)));
        return new InventoryPolicies(defaults);
    }

    public CategoryPolicy policyFor(ProductCategory category) {
        Objects.requireNonNull(category, "category");
        return policies.get(category);
    }

    public Map<ProductCategory, CategoryPolicy> asMap() {
        return policies;
    }
}
