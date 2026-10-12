/*
 * Copyright (c) 2026 GeyserMC. https://geysermc.org
 *
 * This file is part of Antioxidant.
 *
 * Rainbow is free software: you can redistribute it and/or modify it under the terms of the
 * GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * Rainbow is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * Antioxidant. If not, see <https://www.gnu.org/licenses/>.
 */

package com.jubairprem.antioxidant.mapping.texture;

import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import com.jubairprem.antioxidant.mapping.PackAssetCache;
import com.jubairprem.antioxidant.mapping.PackSerializer;
import com.jubairprem.antioxidant.mixin.TextureSlotsAccessor;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public interface ModelTextures<T extends ModelTextures<T>> extends PackAssetCache.Cacheable<T>, PackSerializer.Serializable {

    int width();

    int height();

    Optional<SpriteInfo> getSprite(String key);

    boolean requiresAttachable();

    record SpriteInfo(int x, int y, int width, int height) {
        public static final SpriteInfo EMPTY = new SpriteInfo(0, 0, 0, 0);

        public SpriteInfo(TextureResource texture) {
            this(0, 0, texture.sizeOfFrame().width(), texture.sizeOfFrame().height());
        }
    }

    static Map<String, Material> getCleanMaterials(TextureSlots textures) {
        Map<String, Material> materials = new HashMap<>(((TextureSlotsAccessor) textures).getResolvedValues());
        materials.remove(UnbakedModel.PARTICLE_TEXTURE_REFERENCE);
        return materials;
    }

    static boolean hasNoMaterials(TextureSlots textures) {
        return getCleanMaterials(textures).isEmpty();
    }

    static boolean usesSingleMaterial(Map<String, Material> cleanMaterials) {
        return cleanMaterials.size() == 1 || areAllMaterialsTheSame(cleanMaterials);
    }

    static String sanitizeMaterialReference(String reference) {
        if (TextureSlotsAccessor.invokeIsTextureReference(reference)) {
            return reference.substring(1);
        }
        return reference;
    }

    private static boolean areAllMaterialsTheSame(Map<String, Material> materials) {
        return materials.values().stream().distinct().count() == 1L;
    }
}
