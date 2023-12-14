package com.teampotato.interesium.mixin.vanilla;

import com.teampotato.interesium.api.extension.ExtendedSectionPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.Vec3i;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(SectionPos.class)
public class SectionPosMixin extends Vec3i implements ExtendedSectionPos {
    public SectionPosMixin(int x, int y, int z) {
        super(x, y, z);
    }


    @Override
    public void interesium$setPos(int chunkX, int chunkY, int chunkZ) {
        this.setX(chunkX);
        this.setY(chunkY);
        this.setZ(chunkZ);
    }
}
