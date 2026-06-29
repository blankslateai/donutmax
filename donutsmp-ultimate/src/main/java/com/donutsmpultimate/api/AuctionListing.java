package com.donutsmpultimate.api;

import java.util.List;

/**
 * Represents a single DonutSMP auction house listing.
 */
public class AuctionListing {
    public final String itemName;
    public final String material;
    public final List<String> enchantments;
    public final long price;
    public final String seller;

    public AuctionListing(String itemName, String material, List<String> enchantments,
                          long price, String seller) {
        this.itemName = itemName;
        this.material = material;
        this.enchantments = enchantments;
        this.price = price;
        this.seller = seller;
    }
}
