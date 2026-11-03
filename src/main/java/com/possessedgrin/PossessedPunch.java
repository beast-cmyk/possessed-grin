package com.possessedgrin;

import com.possessedgrin.mixin.DefaultAttributeContainerAccessor;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lets every possessed mob attack, including passive ones like villagers, cows or the Ender Dragon.
 *
 * <p>Requiem's possessed melee attack refuses to run for mobs without an attack-damage stat, and passive mobs
 * don't have one. So:
 * <ol>
 *   <li>Every mob without that stat gets it, with a base value of <b>0</b>. Zero on purpose: Requiem uses the base
 *       value to decide how hard a soul is to capture, and 0 counts exactly the same as "no stat", so soul capture
 *       is unchanged. Their AI never attacks, so nothing else about them changes either.</li>
 *   <li>While a possessed one swings, it temporarily gets extra damage scaled to its size: a tenth of its max
 *       health, from 1 (chicken, cow) through 2 (villager) up to 10 (Ender Dragon). Held weapons add their damage
 *       on top, and Requiem's attack-cooldown scaling still applies.</li>
 * </ol>
 */
public final class PossessedPunch {
    private static final UUID PUNCH_MODIFIER_ID = UUID.fromString("5d1f0e7a-2a4f-4c7e-9a52-7c4f0c3b9e11");

    /** Mob types that were given the stat by this addon, with their patched default stats. */
    private static final Map<EntityType<?>, DefaultAttributeContainer> WITH_ATTACK_STAT = new ConcurrentHashMap<>();

    private PossessedPunch() {
    }

    /**
     * Returns the mob's default stats plus a zero attack-damage stat, or {@code null} to leave them unchanged.
     * The tag check isn't cached, because tags may not be loaded yet the first time this is asked.
     */
    @Nullable
    public static DefaultAttributeContainer withAttackStat(EntityType<?> type, @Nullable DefaultAttributeContainer original) {
        if (original == null || original.has(EntityAttributes.GENERIC_ATTACK_DAMAGE)
                || type == EntityType.PLAYER || type.isIn(PossessedGrin.NEVER_PUNCH)) {
            return null;
        }
        return WITH_ATTACK_STAT.computeIfAbsent(type, t -> {
            DefaultAttributeContainer.Builder builder = DefaultAttributeContainer.builder();
            ((DefaultAttributeContainerAccessor) original).possessedgrin$getInstances()
                    .forEach((attribute, instance) -> builder.add(attribute, instance.getBaseValue()));
            builder.add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 0.0);
            // Minecraft's mob attack also reads knockback; almost every mob has it, but make sure
            if (!original.has(EntityAttributes.GENERIC_ATTACK_KNOCKBACK)) {
                builder.add(EntityAttributes.GENERIC_ATTACK_KNOCKBACK, 0.0);
            }
            return builder.build();
        });
    }

    public static float punchDamage(MobEntity mob) {
        return MathHelper.clamp(mob.getMaxHealth() / 10.0F, 1.0F, 10.0F);
    }

    /** Called right before a mob attacks. */
    public static void beforeAttack(MobEntity mob) {
        if (!WITH_ATTACK_STAT.containsKey(mob.getType()) || !PossessedGrin.isPossessed(mob)) {
            return;
        }
        EntityAttributeInstance attack = mob.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE);
        if (attack != null && attack.getModifier(PUNCH_MODIFIER_ID) == null) {
            attack.addTemporaryModifier(new EntityAttributeModifier(
                    PUNCH_MODIFIER_ID, "Possessed punch", punchDamage(mob), EntityAttributeModifier.Operation.ADDITION));
        }
    }

    /** Called right after a mob attacks. */
    public static void afterAttack(MobEntity mob) {
        if (!WITH_ATTACK_STAT.containsKey(mob.getType())) {
            return;
        }
        EntityAttributeInstance attack = mob.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE);
        if (attack != null && attack.getModifier(PUNCH_MODIFIER_ID) != null) {
            attack.removeModifier(PUNCH_MODIFIER_ID);
        }
    }
}
