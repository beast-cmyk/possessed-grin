package com.possessedgrin.mixin.client;

import com.possessedgrin.client.PossessedFaceFeatureRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Adds the possessed-face layer to every living entity renderer. It is added before the subclass
 * layers (armor, held items...), so it draws right on top of the mob's skin.
 */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<T extends LivingEntity, M extends EntityModel<T>> implements FeatureRendererContext<T, M> {
    @Shadow
    @Final
    protected List<FeatureRenderer<T, M>> features;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void possessedgrin$addFaceLayer(EntityRendererFactory.Context ctx, M model, float shadowRadius, CallbackInfo ci) {
        this.features.add(new PossessedFaceFeatureRenderer<>(this));
    }
}
