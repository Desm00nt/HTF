package com.howtofish.mod.entity;

/**
 * All "regular" fish/crustacean species that can be caught with the fishing rod.
 * Mirrors the How to Fish idea of small cheap critters vs. big valuable ones.
 * health   - hit points of the released live fish entity (has to be killed with the knife)
 * price    - Rubles earned when the meat is given to the Old Man
 * texture  - texture path suffix used by FishModel/FishRenderer
 */
public enum FishType {
    ANCHOVY("anchovy", 3, 1, 0.55f, 0x8FA6B0),
    HERRING("herring", 4, 2, 0.65f, 0xB9C4C9),
    CRAB("crab", 6, 3, 0.5f, 0xC1502E),
    SHRIMP("shrimp", 4, 2, 0.45f, 0xE8A6A0),
    LOBSTER("lobster", 10, 6, 0.8f, 0x9C2B1B),
    PUFFERFISH("pufferfish", 8, 5, 0.7f, 0xE0C34C);

    private final String id;
    private final int health;
    private final int price;
    private final float scale;
    private final int color;

    FishType(String id, int health, int price, float scale, int color) {
        this.id = id;
        this.health = health;
        this.price = price;
        this.scale = scale;
        this.color = color;
    }

    public String getId() {
        return id;
    }

    public String getMeatId() {
        return id + "_meat";
    }

    public int getHealth() {
        return health;
    }

    public int getPrice() {
        return price;
    }

    public float getScale() {
        return scale;
    }

    public int getColor() {
        return color;
    }
}
