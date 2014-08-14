package team004;

import java.util.Arrays;
import java.util.Collections;

import battlecode.common.Direction;
import battlecode.common.GameConstants;
import battlecode.common.RobotController;
import battlecode.common.RobotType;
import battlecode.common.MapLocation;

/** The example funcs player is a player meant to demonstrate basic usage of the most common commands.
 * Robots will move around randomly, occasionally mining and writing useless messages.
 * The HQ will spawn soldiers continuously.
 */
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
						boolean defuse = true;
						Direction nextDir = null;
                        MapLocation nextLoc = null;
                        for (int i=0; i < dirArray.length; i++) {
                            nextDir = dirArray[i];
                            nextLoc = loc.add(nextDir);
                            if (rc.senseMine(nextLoc) != null) {
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

package team004;

import battlecode.common.Direction;
import battlecode.common.GameConstants;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

/** The example funcs player is a player meant to demonstrate basic usage of the most common commands.
 * Robots will move around randomly, occasionally mining and writing useless messages.
 * The HQ will spawn soldiers continuously.
 */
public class RobotPlayer {
    public static void run(RobotController rc) {
        while (true) {
            try {
                if (rc.getType() == RobotType.HQ) {
                    if (rc.isActive()) {
                        // Spawn a soldier
                        Direction dir = rc.getLocation().directionTo(rc.senseEnemyHQLocation());
                        if (rc.canMove(dir))
                            rc.spawn(dir);
                    }
                } else if (rc.getType() == RobotType.SOLDIER) {
                    if (rc.isActive()) {
                        if (Math.random()<0.005) {
                            // Lay a mine
                            if(rc.senseMine(rc.getLocation())==null)
                                rc.layMine();
                        } else {
                            // Choose a random direction, and move that way if possible
                            Direction dir = rc.getLocation().directionTo(rc.senseEnemyHQLocation());
                            if(rc.canMove(dir)) {
                                rc.move(dir);
                                rc.setIndicatorString(0, "Moving to enemy base: "+dir.toString());
                            }
                        }
                    }

                    if (Math.random()<0.01 && rc.getTeamPower()>5) {
                        // Write the number 5 to a position on the message board corresponding to the robot's ID
                        rc.broadcast(rc.getRobot().getID()%GameConstants.BROADCAST_MAX_CHANNELS, 5);
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
