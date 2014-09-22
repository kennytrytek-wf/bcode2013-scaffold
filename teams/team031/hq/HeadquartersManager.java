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
    int fusionResearch;
    int nukeResearch;
    boolean init = false;
    int prevNumSoldiers;
    int prevNumEncampments;
    int strategy;

    public HeadquartersManager(RobotController rc) throws GameActionException {
        this.initialize(rc);
    }

    private void initialize(RobotController rc) throws GameActionException {
        this.info = new Info(rc);
        this.rand = new Random(rc.getRobot().getID());
        this.fusionResearch = 25;
        this.nukeResearch = 400;
        this.init = true;
        this.prevNumSoldiers = 0;
        this.prevNumEncampments = 0;
        this.strategy = 0;
    }

    private void update(RobotController rc) throws GameActionException {
        this.info.update(rc);
        this.signalIfEnemies(rc);
        this.prevNumSoldiers = Radio.readData(rc, Radio.NUM_SOLDIERS);
        Radio.writeData(rc, Radio.NUM_SOLDIERS, 0);
        this.prevNumEncampments = Radio.readData(rc, Radio.NUM_ENCAMPMENTS);
        Radio.writeData(rc, Radio.NUM_ENCAMPMENTS, 0);
        rc.setIndicatorString(1, "Soldiers: " + this.prevNumSoldiers + ", Encampments: " + this.prevNumEncampments);
        this.setStrategy(rc);
        Radio.writeOldLocation(rc, Radio.ENEMY, new MapLocation(0, 0), 2);
        Radio.writeOldLocation(rc, Radio.MEDBAY, new MapLocation(0, 0), 2);
    }

    public void move(RobotController rc) throws GameActionException {
        this.update(rc);
        if (rc.isActive()) {
            if (this.strategy == 1 || this.nukeResearch < 50) {
                if (this.prevNumSoldiers > 10) {
                    rc.researchUpgrade(Upgrade.NUKE);
                    this.nukeResearch -= 1;
                    return;
                }
            }
            if ((this.info.round > 200) && (this.fusionResearch > 0) && (this.info.teamPower < 150) && (this.strategy != 1)) {
                rc.researchUpgrade(Upgrade.FUSION);
                this.fusionResearch -= 1;
            } else if ((this.fusionResearch == 0) && (this.info.teamPower < 250)) {
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

        if (go.length == 0) {
            return;
        }
        MapLocation enemyLoc = rc.senseLocationOf(go[0]);
        Radio.writeLocation(rc, Radio.ENEMY, enemyLoc);
        Radio.writeLocation(rc, Radio.OUTPOST, this.info.myHQLoc);
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

    private void setStrategy(RobotController rc) throws GameActionException {
        if (this.nukeResearch > 200 && rc.senseEnemyNukeHalfDone()) {
            Radio.writeData(rc, Radio.STRATEGY, 3);
            this.strategy = 3;
            this.nukeResearch = 999;
            return;
        }
        if (this.info.round > 800) {
            Radio.writeData(rc, Radio.STRATEGY, 1);
            Radio.writeLocation(rc, Radio.OUTPOST, this.info.myHQLoc);
            this.strategy = 1;
            return;
        }
        int enemyDistance = this.info.distance(
            this.info.myHQLoc, this.info.enemyHQLoc);

        MapLocation[] encampments = rc.senseEncampmentSquares(
            this.info.myHQLoc, 5 * 5, null);

        MapLocation[] allEncampments = rc.senseAllEncampmentSquares();
        rc.setIndicatorString(2, "Strategy: " + this.strategy + ", Enemy distance: " + enemyDistance + ", Close Encampments: " + encampments.length + ", All Encampments: " + allEncampments.length);
        if (enemyDistance < 30) {
            Radio.writeData(rc, Radio.STRATEGY, 0);
            this.strategy = 0;
            return;
        }
        if (encampments.length > 5) {
            Radio.writeData(rc, Radio.STRATEGY, 1);
            this.strategy = 1;
            return;
        }
        if (allEncampments.length > 75) {
            Radio.writeData(rc, Radio.STRATEGY, 2);
            this.strategy = 2;
            return;
        }
        if (enemyDistance > 40) {
            Radio.writeData(rc, Radio.STRATEGY, 1);
            this.strategy = 1;
            return;
        }
        Radio.writeData(rc, Radio.STRATEGY, 0);
        this.strategy = 0;
    }
}
