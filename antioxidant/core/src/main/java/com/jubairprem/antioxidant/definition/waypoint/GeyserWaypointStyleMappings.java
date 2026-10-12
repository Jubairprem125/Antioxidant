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

package com.jubairprem.antioxidant.definition.waypoint;

import com.mojang.serialization.Codec;
import net.minecraft.client.resources.WaypointStyle;
import net.minecraft.resources.Identifier;
import com.jubairprem.antioxidant.definition.AbstractGeyserMappings;

import java.util.Map;

public final class GeyserWaypointStyleMappings extends AbstractGeyserMappings<Identifier, WaypointStyle> {
    public static final Codec<GeyserWaypointStyleMappings> CODEC = AbstractGeyserMappings.createCodec("waypoint_styles", 1,
            Identifier.CODEC, WaypointStyle.CODEC, GeyserWaypointStyleMappings::new);

    public GeyserWaypointStyleMappings() {}

    private GeyserWaypointStyleMappings(Map<Identifier, WaypointStyle> mappings) {
        super(mappings);
    }

    @Override
    public void map(Identifier key, WaypointStyle value) {
        super.map(key, value);
    }
}
