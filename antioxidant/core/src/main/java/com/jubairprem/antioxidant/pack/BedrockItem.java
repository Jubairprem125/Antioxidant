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

package com.jubairprem.antioxidant.pack;

import net.minecraft.resources.Identifier;
import com.jubairprem.antioxidant.mapping.PackSerializer;
import com.jubairprem.antioxidant.mapping.PackSerializingContext;
import com.jubairprem.antioxidant.mapping.attachable.BedrockAttachableContext;
import com.jubairprem.antioxidant.mapping.geometry.BedrockGeometryContext;
import com.jubairprem.antioxidant.mapping.texture.ItemModelTextures;
import com.jubairprem.antioxidant.mapping.texture.TextureHolder;

import java.util.concurrent.CompletableFuture;

public record BedrockItem(Identifier identifier, TextureHolder icon, ItemModelTextures textures, BedrockGeometryContext geometryContext,
                          BedrockAttachableContext attachableContext) implements PackSerializer.Serializable {

    @Override
    public CompletableFuture<?> save(PackSerializingContext context) {
        return icon
                .with(textures)
                .with(geometryContext)
                .with(attachableContext)
                .save(context);
    }
}
