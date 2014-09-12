package team031.hq;

import java.util.Random;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.GameObject;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.Team;
import battlecode.common.Upgrade;

import team031.interfaces.Manager;
import team031.common.Info;
import team031.common.Radio;

public class HeadquartersManager extends Manager {
    Info info;
    Random rand;
    int visionResearch;
    int fusionResearch;
    boolean init = false;

    public HeadquartersManager(RobotController rc) throws GameActionException {
        this.initialize(rc);
    }

    private void initialize(RobotController rc) throws GameActionException {
        this.info = new Info(rc);
        this.rand = new Random(rc.getRobot().getID());
        this.visionResearch = 25;
        this.fusionResearch = 25;
        this.init = true;
    }

    public void move(RobotController rc) throws GameActionException {
        this.info.update(rc);
        this.signalIfEnemies(rc);
        if (rc.isActive()) {
            if ((this.info.round > 200) && (this.fusionResearch > 0) && (this.info.teamPower < 100)) {
                rc.researchUpgrade(Upgrade.FUSION);
                this.fusionResearch -= 1;
            } else if ((this.info.round > 250) && (this.info.teamPower < 100)) {
                rc.researchUpgrade(Upgrade.NUKE);
            } else {
                this.spawn(rc);
            }
        }
    }

    private void signalIfEnemies(RobotController rc) throws GameActionException {
        MapLocation senseLoc = this.info.myHQLoc;
        rc.setIndicatorString(0, "Sensing enemies at (" + senseLoc.x + ", " + senseLoc.y + ")");
        GameObject[] go = rc.senseNearbyGameObjects(
            Robot.class, senseLoc, 33 * 33, this.info.opponent);

        rc.setIndicatorString(1, go.length + " enemies.");
        if (go.length == 0) {
            return;
        }
        MapLocation enemyLoc = rc.senseLocationOf(go[0]);
        Radio.writeLocation(rc, Radio.ENEMY, enemyLoc);
    }


    private void spawn(RobotController rc) throws GameActionException {
        // Spawn a soldier
        Direction origDir = this.info.myHQLoc.directionTo(
            rc.senseEnemyHQLocation());

        boolean rotateLeft = this.rand.nextInt(1) > 0;
        origDir = rotateLeft ? origDir.rotateRight() : origDir.rotateLeft();
        Direction dir = rotateLeft ? origDir.rotateLeft() : origDir.rotateRight();
        MapLocation spawnLoc = this.info.myHQLoc.add(dir);
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
                spawnLoc = this.info.myHQLoc.add(dir);
            }
        }
    }
}
