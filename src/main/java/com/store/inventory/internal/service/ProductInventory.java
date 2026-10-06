package com.store.inventory.internal.service;

import com.store.inventory.api.ProductCategory;
import com.store.inventory.internal.model.ProductState;

/** Coordinates stock calculations for a product. */
public final class ProductInventory {

    private final ProductState state;

    public ProductInventory(String sku, ProductCategory category) {
        this.state = new ProductState(sku, category);
    }

    public ProductState state() {
        return state;
    }

    public int availableUnits() {
        return state.availableUnits();
    }
}
