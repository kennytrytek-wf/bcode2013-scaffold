package team004.hq;

import java.util.Random;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.Team;
import battlecode.common.Upgrade;

import team004.interfaces.Manager;
import team004.common.MapInfo;

public class HeadquartersManager extends Manager {
    MapLocation myLoc;
    Random rand;
    boolean init = false;

    public HeadquartersManager(RobotController rc) throws GameActionException {
        this.initialize(rc);
    }

    public void initialize(RobotController rc) throws GameActionException {
        this.myLoc = rc.getLocation();
        this.rand = new Random(rc.getRobot().getID());
        this.init = true;
    }

    public void move(RobotController rc) throws GameActionException {
        if (rc.isActive()) {
            this.spawn(rc);
        }
    }


    public void spawn(RobotController rc) throws GameActionException {
        // Spawn a soldier
        Direction origDir = this.myLoc.directionTo(
            rc.senseEnemyHQLocation());

        boolean rotateLeft = this.rand.nextInt(1) > 0;
        origDir = rotateLeft ? origDir.rotateRight() : origDir.rotateLeft();
        Direction dir = rotateLeft ? origDir.rotateLeft() : origDir.rotateRight();
        MapLocation spawnLoc = this.myLoc.add(dir);
        while (true) {
            Team mineOwner = rc.senseMine(spawnLoc);
            if (rc.canMove(dir) && ((mineOwner == null) || (mineOwner == rc.getTeam()))) {
                rc.spawn(dir);
                return;
            } else if (dir == origDir) {
                rc.researchUpgrade(Upgrade.NUKE);
                return;
            } else {
                dir = rotateLeft ? dir.rotateLeft() : dir.rotateRight();
                spawnLoc = this.myLoc.add(dir);
            }
        }
    }
}
