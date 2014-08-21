package team004.hq;

import java.util.Random;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.Team;

import team004.interfaces.Manager;
import team004.common.RobotState;
import team004.common.MapInfo;

public class HeadquartersManager extends Manager {
    MapInfo mapInfo;
    RobotState state;
    Random rand;

    public RobotState createRobotState(RobotController rc) {
        return new RobotState(rc.getRobot().getID());
    }

    public void move(RobotController rc) throws GameActionException {
        if (this.mapInfo == null) {
            this.state = this.createRobotState(rc);
            this.mapInfo = new MapInfo(rc);
            this.rand = new Random(this.state.robotID);
        }
        if (rc.isActive()) {
            // Spawn a soldier
            MapLocation hqLoc = rc.getLocation();
            Direction origDir = hqLoc.directionTo(
                rc.senseEnemyHQLocation());
            boolean rotateLeft = this.rand.nextInt(1) > 0;
            origDir = rotateLeft ? origDir.rotateRight() : origDir.rotateLeft();
            Direction dir = rotateLeft ? origDir.rotateLeft() : origDir.rotateRight();
            MapLocation spawnLoc = hqLoc.add(dir);
            while (true) {
                Team mineOwner = rc.senseMine(spawnLoc);
                if (rc.canMove(dir) && ((mineOwner == null) || (mineOwner == rc.getTeam()))) {
                    rc.spawn(dir);
                    break;
                } else if (dir == origDir) {
                    break;
                } else {
                    dir = rotateLeft ? dir.rotateLeft() : dir.rotateRight();
                    spawnLoc = hqLoc.add(dir);
                }
            }
        }
    }
}
