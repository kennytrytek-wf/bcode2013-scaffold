package team004;

import java.util.Arrays;
import java.util.Collections;

import battlecode.common.Direction;
import battlecode.common.GameConstants;
import battlecode.common.RobotController;
import battlecode.common.RobotType;
import battlecode.common.MapLocation;

public class RobotPlayer {
	public static void run(RobotController rc) {
		while (true) {
			try {
				if (rc.getType() == RobotType.HQ) {
					if (rc.isActive()) {
						// Spawn a soldier
						Direction origDir = rc.getLocation().directionTo(rc.senseEnemyHQLocation());
						Direction dir = origDir.rotateLeft();
						boolean rotated = false;
						while (!rc.canMove(dir) && dir != origDir) {
						    dir = origDir.rotateLeft();
						    rotated = true;
						}
						if (!(rotated && dir == origDir)) {
                            rc.spawn(dir);
						}
					}
				} else if (rc.getType() == RobotType.SOLDIER) {
					if (rc.isActive()) {
					    MapLocation loc = rc.getLocation();
						Direction dir = loc.directionTo(rc.senseEnemyHQLocation());
						Direction dirLeft = dir.rotateLeft();
						Direction dirRight = dir.rotateRight();
						Direction[] dirArray = new Direction[]{dir, dirLeft, dirRight};
						Collections.shuffle(Arrays.asList(dirArray));
						boolean defuse = false;
						Direction nextDir = null;
                        MapLocation nextLoc = null;
                        for (int i=0; i < dirArray.length; i++) {
                            nextDir = dirArray[i];
                            nextLoc = loc.add(nextDir);
                            if (rc.senseMine(nextLoc) != null) {
                                defuse = true;
                                continue;
                            } else if (rc.canMove(nextDir)) {
                                rc.move(nextDir);
                                defuse = false;
                                break;
                            }
                        }
                        if (defuse) {
                            rc.defuseMine(nextLoc);
                        }
					}
				}

				// End turn
				rc.yield();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
