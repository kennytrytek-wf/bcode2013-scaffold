package team004.hq;

import java.util.Random;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.Team;
import battlecode.common.Upgrade;

import team004.interfaces.Manager;
import team004.common.RobotState;
import team004.common.MapInfo;

public class HeadquartersManager extends Manager {
    MapInfo mapInfo;
    RobotState state;
    Random rand;
    Upgrade[] upgradeList;
    int upgradeIndex;
    int upgradeRoundsLeft;
    boolean spawned;
    int researchDelay;
    int researchChunkSize;
    int currentResearchCount;
    boolean allUpgradesResearched;
    boolean init = false;

    public RobotState createRobotState(RobotController rc) {
        return new RobotState(rc.getRobot().getID());
    }

    public void move(RobotController rc) throws GameActionException {
        if (!this.init) {
            this.initialize(rc);
        }
        if (rc.isActive()) {
            if (this.shouldSpawn()) {
                this.spawn(rc);
            } else {
                this.research(rc);
            }
        }
    }

    public boolean shouldSpawn() {
        if (this.allUpgradesResearched) {
            return true;
        }
        int round = Clock.getRoundNum();
        if (round < this.researchDelay) {
            return true;
        }
        if (this.spawned) {
            this.spawned = false;
            return false;
        }
        if ((this.currentResearchCount < this.researchChunkSize) &&
               (this.currentResearchCount > 0)) {
            return false;
        }
        return true;
    }

    public void research(RobotController rc) throws GameActionException {
        if (this.upgradeRoundsLeft == 0) {
            this.upgradeIndex += 1;
            if (this.upgradeIndex == this.upgradeList.length) {
                this.allUpgradesResearched = true;
                this.spawn(rc);
                return;
            }
            this.upgradeRoundsLeft = this.upgradeList[this.upgradeIndex].numRounds;
            this.currentResearchCount = -1;
        }
        rc.researchUpgrade(this.upgradeList[this.upgradeIndex]);
        this.upgradeRoundsLeft -= 1;
        this.currentResearchCount -= 1;
        if (this.currentResearchCount == 0) {
            this.currentResearchCount = this.researchChunkSize;
        }
    }

    public void initialize(RobotController rc) {
        this.state = this.createRobotState(rc);
        this.mapInfo = new MapInfo(rc);
        if (this.mapInfo.mapSize == MapInfo.SMALL) {
            this.researchDelay = 30;
            this.researchChunkSize = 2;
        } else if (this.mapInfo.mapSize == MapInfo.MEDIUM) {
            this.researchDelay = 20;
            this.researchChunkSize = 4;
        } else {
            this.researchDelay = 0;
            this.researchChunkSize = 8;
        }
        this.rand = new Random(this.state.robotID);
        this.upgradeList = new Upgrade[]{Upgrade.FUSION};
        this.upgradeIndex = 0;
        this.upgradeRoundsLeft = this.upgradeList[this.upgradeIndex].numRounds;
        this.currentResearchCount = this.researchChunkSize;
        this.spawned = false;
        this.allUpgradesResearched = false;
        this.init = true;
    }

    public void spawn(RobotController rc) throws GameActionException {
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
                this.spawned = true;
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
