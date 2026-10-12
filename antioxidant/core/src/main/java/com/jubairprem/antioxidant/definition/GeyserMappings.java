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

package com.jubairprem.antioxidant.definition;

import com.jubairprem.antioxidant.definition.block.GeyserBlockMappings;
import com.jubairprem.antioxidant.definition.item.GeyserItemMappings;
import com.jubairprem.antioxidant.definition.skull.GeyserSkullMappings;
import com.jubairprem.antioxidant.definition.waypoint.GeyserWaypointStyleMappings;
import com.jubairprem.antioxidant.stats.PackStatKeys;
import com.jubairprem.antioxidant.stats.PackStats;

public record GeyserMappings(GeyserBlockMappings blocks, GeyserItemMappings items,
                             GeyserSkullMappings skulls, GeyserWaypointStyleMappings waypointStyles) implements PackStats.Aggregator {

    public GeyserMappings() {
        this(new GeyserBlockMappings(), new GeyserItemMappings(),
                new GeyserSkullMappings(), new GeyserWaypointStyleMappings());
    }

    @Override
    public void collectStats(PackStats.Collector collector) {
        collector.collect(PackStatKeys.BLOCK_MAPPINGS, blocks)
                .collect(PackStatKeys.ITEM_MAPPINGS, items)
                .collect(PackStatKeys.SKULL_MAPPINGS, skulls)
                .collect(PackStatKeys.WAYPOINT_MAPPINGS, waypointStyles);
    }
}
