package team004;

import java.util.Arrays;
import java.util.Collections;
import java.util.Random;

import battlecode.common.Direction;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotType;
import battlecode.common.Team;

public class RobotPlayer {
	public static void run(RobotController rc) {
	    Random rand = new Random();
		while (true) {
			try {
				if (rc.getType() == RobotType.HQ) {
					if (rc.isActive()) {
						// Spawn a soldier
						MapLocation hqLoc = rc.getLocation();
						Direction origDir = hqLoc.directionTo(rc.senseEnemyHQLocation()).rotateRight();
						Direction dir = origDir.rotateLeft();
						MapLocation spawnLoc = hqLoc.add(dir);
						boolean rotated = false;
						while (true) {
						    if (rc.canMove(dir) && (rc.senseMine(spawnLoc) == null)) {
						        rc.spawn(dir);
						        break;
						    } else if (dir == origDir) {
						        break;
						    } else {
						        dir = dir.rotateLeft();
						        spawnLoc = hqLoc.add(dir);
						    }
						}
					}
				} else if (rc.getType() == RobotType.SOLDIER) {
					if (rc.isActive()) {
					    MapLocation loc = rc.getLocation();

					    //Define possible directions to move
						Direction dir = loc.directionTo(rc.senseEnemyHQLocation());
						Direction dirLeft = dir.rotateLeft();
						Direction dirRight = dir.rotateRight();
						Direction[] dirArray = new Direction[]{dir, dirLeft, dirRight};

						//Randomize direction
						int[] randIndexes = new int[]{rand.nextInt(3), rand.nextInt(3)};
						Direction tmp = dirArray[randIndexes[0]].rotateLeft();
						dirArray[randIndexes[0]] = dirArray[randIndexes[1]];
						dirArray[randIndexes[1]] = tmp.rotateRight();
						Collections.shuffle(Arrays.asList(dirArray));

						//Make a move
						boolean defuse = false;
						Direction nextDir = null;
                        MapLocation nextLoc = null;
                        for (int i=0; i < dirArray.length; i++) {
                            nextDir = dirArray[i];
                            nextLoc = loc.add(nextDir);
                            Team mineTeamOwner = rc.senseMine(nextLoc);
                            if ((mineTeamOwner != null) && (mineTeamOwner != rc.getTeam())) {
                                defuse = true;
                                continue;
                            } else if (rc.canMove(nextDir)) {
                                defuse = false;
                                break;
                            }
                        }
                        if (defuse) {
                            rc.defuseMine(nextLoc);
                        } else {
                            rc.move(nextDir);
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
