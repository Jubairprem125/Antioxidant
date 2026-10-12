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

import net.minecraft.resources.Identifier;
import net.minecraft.util.ProblemReporter;
import com.jubairprem.antioxidant.mapping.AssetResolver;
import com.jubairprem.antioxidant.mapping.PackSerializer;
import com.jubairprem.antioxidant.mapping.PackSerializingContext;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class CopyTextureHolder extends TextureHolder {

    public CopyTextureHolder(Identifier destination) {
        super(destination);
    }

    @Override
    public Optional<TextureResource> load(AssetResolver assetResolver, ProblemReporter reporter) {
        return Optional.empty();
    }

    @Override
    public CompletableFuture<?> save(PackSerializingContext context) {
        return PackSerializer.noop();
    }

    @Override
    public TextureHolder copy() {
        return this;
    }

    @Override
    protected boolean shouldReportMissingWhenAbsent() {
        return false;
    }
}
