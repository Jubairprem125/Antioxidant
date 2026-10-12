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

package com.jubairprem.antioxidant.definition.item;

import net.minecraft.resources.Identifier;

import java.util.Optional;

public interface GeyserItemDefinition extends GeyserItemMapping {

    GeyserBaseItemDefinition base();

    boolean conflictsWith(Optional<Identifier> parentModel, GeyserItemDefinition other);

    @Override
    default int compareTo(GeyserItemMapping other) {
        if (other instanceof GeyserItemDefinition itemDefinition) {
            return base().bedrockIdentifier().compareTo(itemDefinition.base().bedrockIdentifier());
        }
        return -1; // Groups are always greater than individual mappings
    }
}
