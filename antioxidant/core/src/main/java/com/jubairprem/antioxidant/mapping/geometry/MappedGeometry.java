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

package com.jubairprem.antioxidant.mapping.geometry;

import com.jubairprem.antioxidant.mapping.PackAssetCache;
import com.jubairprem.antioxidant.mapping.PackSerializer;
import com.jubairprem.antioxidant.mapping.PackSerializingContext;

import java.util.concurrent.CompletableFuture;

public interface MappedGeometry extends PackAssetCache.Cacheable<MappedGeometry>, PackSerializer.Serializable {

    String identifier();

    @Override
    default CachedGeometry cachedCopy() {
        if (this instanceof CachedGeometry cached) {
            return cached;
        }
        return new CachedGeometry(identifier());
    }

    record CachedGeometry(String identifier) implements MappedGeometry {

        @Override
        public CompletableFuture<?> save(PackSerializingContext context) {
            return PackSerializer.noop();
        }
    }
}
