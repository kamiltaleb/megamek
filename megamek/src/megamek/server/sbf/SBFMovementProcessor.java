/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.server.sbf;

import megamek.common.strategicBattleSystems.SBFFormation;
import megamek.common.strategicBattleSystems.SBFMovePath;
import org.apache.logging.log4j.LogManager;

record SBFMovementProcessor(SBFGameManager gameManager) implements SBFGameManagerHelper {

    void processMovement(SBFMovePath movePath, SBFFormation formation) {
        if (!validatePermitted(movePath, formation)) {
            return;
        }

        formation.setPosition(movePath.getLastPosition());
        formation.setDone(true);
        gameManager.sendUnitUpdate(formation);
        gameManager.endCurrentTurn(formation);
    }

    private boolean validatePermitted(SBFMovePath movePath, SBFFormation formation) {
        if (!game().getPhase().isMovement()) {
            LogManager.getLogger().error("Server got movement packet in wrong phase!");
            return false;
        } else if (movePath.isIllegal()) {
            LogManager.getLogger().error("Illegal move path!");
            return false;
        } else if (formation.isDone()) {
            LogManager.getLogger().error("Formation already done!");
            return false;
        }
        return true;
    }

}
