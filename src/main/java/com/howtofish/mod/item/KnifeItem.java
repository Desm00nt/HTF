package com.howtofish.mod.item;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;

import java.util.UUID;

/**
 * The Knife - used to finish off fish/crustaceans that were released by the
 * fishing rod. Slightly faster and, against fish specifically, deadlier than
 * a vanilla sword (see the extra damage bonus in FishingEvents).
 */
public class KnifeItem extends SwordItem {

    public static final Tier KNIFE_TIER = Tiers.IRON;

    public KnifeItem(Properties properties) {
        super(KNIFE_TIER, 2, -0.8f, properties);
    }
}
