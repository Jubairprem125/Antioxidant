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

import com.mojang.serialization.Codec;
import net.minecraft.util.ProblemReporter;
import com.jubairprem.antioxidant.pack.PackPaths;

import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Function;

public record PackSerializingContext(AssetResolver assetResolver, PackSerializer serializer, PackPaths paths, ProblemReporter reporter) {

    public <T> CompletableFuture<?> save(Codec<T> codec, Optional<T> optional, Function<PackPaths, Path> pathResolver) {
        return optional.map(object -> save(codec, object, pathResolver)).orElseGet(PackSerializer::noop);
    }

    public <T> CompletableFuture<?> save(Codec<T> codec, T object, Function<PackPaths, Path> pathGetter) {
        return save(codec, object, (paths, ignored) -> pathGetter.apply(paths));
    }

    public <T> CompletableFuture<?> save(Codec<T> codec, Optional<T> optional, BiFunction<PackPaths, T, Path> pathResolver) {
        return optional.map(object -> save(codec, object, pathResolver)).orElseGet(PackSerializer::noop);
    }

    public <T> CompletableFuture<?> save(Codec<T> codec, T object, BiFunction<PackPaths, T, Path> pathResolver) {
        return serializer.saveJson(codec, object, pathResolver.apply(paths, object));
    }
}
