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

package com.jubairprem.antioxidant.mapping;

import com.jubairprem.antioxidant.definition.GeyserMappings;
import com.jubairprem.antioxidant.mapping.geometry.GeometryRenderer;
import com.jubairprem.antioxidant.mapping.geometry.MappedGeometryCache;
import com.jubairprem.antioxidant.mapping.texture.BlockModelTextures;
import com.jubairprem.antioxidant.mapping.texture.ItemModelTextures;
import com.jubairprem.antioxidant.mapping.texture.ModelTextureCache;
import com.jubairprem.antioxidant.pack.PackPaths;
import com.jubairprem.antioxidant.stats.PackStatKeys;

import java.util.Optional;

// TODO maybe split the responsibilities of this class
public final class PackContext {
    private final GeyserMappings mappings;
    private final PackPaths paths;
    private final BedrockAssetConsumer assetConsumer;
    private final AssetResolver assetResolver;
    private final Optional<GeometryRenderer> geometryRenderer;
    private final boolean reportSuccesses;
    private final ModelTextureCache<BlockModelTextures> blockTextureCache = new ModelTextureCache<>(PackStatKeys.BLOCK_TEXTURE_CACHE);
    private final ModelTextureCache<ItemModelTextures> itemTextureCache = new ModelTextureCache<>(PackStatKeys.ITEM_TEXTURE_CACHE);
    private final MappedGeometryCache geometryCache = new MappedGeometryCache();

    public PackContext(GeyserMappings mappings, PackPaths paths, BedrockAssetConsumer assetConsumer, AssetResolver assetResolver,
                       Optional<GeometryRenderer> geometryRenderer, boolean reportSuccesses) {
        this.mappings = mappings;
        this.paths = paths;
        this.assetConsumer = assetConsumer;
        this.assetResolver = assetResolver;
        this.geometryRenderer = geometryRenderer;
        this.reportSuccesses = reportSuccesses;
    }

    public GeyserMappings mappings() {
        return mappings;
    }

    public PackPaths paths() {
        return paths;
    }

    public BedrockAssetConsumer assetConsumer() {
        return assetConsumer;
    }

    public AssetResolver assetResolver() {
        return assetResolver;
    }

    public Optional<GeometryRenderer> geometryRenderer() {
        return geometryRenderer;
    }

    public boolean reportSuccesses() {
        return reportSuccesses;
    }

    public ModelTextureCache<BlockModelTextures> blockTextureCache() {
        return blockTextureCache;
    }

    public ModelTextureCache<ItemModelTextures> itemTextureCache() {
        return itemTextureCache;
    }

    public MappedGeometryCache geometryCache() {
        return geometryCache;
    }
}
