package swarm.hq;

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

import swarm.interfaces.Manager;
import swarm.common.Info;
import swarm.common.Radio;

public class HeadquartersManager extends Manager {
    Info info;
    Random rand;
    int fusionResearch;
    int nukeResearch;
    boolean init = false;
    int prevNumSoldiers;
    int prevNumEncampments;

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
    }

    private void update(RobotController rc) throws GameActionException {
        this.info.update(rc);
        this.signalIfEnemies(rc);
        this.prevNumSoldiers = Radio.readData(rc, Radio.NUM_SOLDIERS);
        Radio.writeData(rc, Radio.NUM_SOLDIERS, 0);
        this.prevNumEncampments = Radio.readData(rc, Radio.NUM_ENCAMPMENTS);
        Radio.writeData(rc, Radio.NUM_ENCAMPMENTS, 0);
        rc.setIndicatorString(1, "Soldiers: " + this.prevNumSoldiers + ", Encampments: " + this.prevNumEncampments);
        Radio.writeOldLocation(rc, Radio.ENEMY, new MapLocation(0, 0), 2);
        Radio.writeOldLocation(rc, Radio.MEDBAY, new MapLocation(0, 0), 2);
    }

    public void move(RobotController rc) throws GameActionException {
        this.update(rc);
        if (rc.isActive()) {
            if ((this.info.round > 200) && (this.fusionResearch > 0) && (this.info.teamPower < 150)) {
                rc.researchUpgrade(Upgrade.FUSION);
                this.fusionResearch -= 1;
            } else {
                this.spawn(rc);
            }
        }
    }

    private void signalIfEnemies(RobotController rc) throws GameActionException {
        GameObject[] enemies = rc.senseNearbyGameObjects(
            Robot.class, this.info.myHQLoc, 15 * 15, this.info.opponent);

        if (enemies.length > 2) {
            Radio.writeData(rc, Radio.STRATEGY, 1);
            MapLocation enemyLoc = rc.senseLocationOf(enemies[0]);
            Radio.writeLocation(rc, Radio.ENEMY, enemyLoc);
        } else {
            Radio.writeData(rc, Radio.STRATEGY, 0);
        }
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
}
