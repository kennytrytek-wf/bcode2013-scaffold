package swarm.robot;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameObject;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotType;
import battlecode.common.Robot;
import battlecode.common.Team;

import swarm.common.Info;
import swarm.common.Radio;
import swarm.common.RobotState;
import swarm.interfaces.Manager;
import swarm.hq.HeadquartersManager;


public class RobotManager extends Manager {
    Info info;
    MapLocation myLoc;
    Random rand;
    MapLocation encampLoc;
    int id;
    double initialEnergon;
    Direction toEnemyHQ;
    int samePosition;

    public RobotManager(RobotController rc) throws GameActionException {
        this.initialize(rc);
    }

    private void initialize(RobotController rc) throws GameActionException {
        this.info = new Info(rc);
        this.myLoc = rc.getLocation();
        this.encampLoc = null;
        this.id = rc.getRobot().getID();
        this.rand = new Random(this.id);
        this.initialEnergon = rc.getEnergon();
        this.samePosition = 0;
    }

    public void move(RobotController rc) throws GameActionException {
        this.info.update(rc);
        Radio.updateData(rc, Radio.NUM_SOLDIERS, 1);
        if (!rc.isActive()) {
            return;
        }
        MapLocation newLoc = rc.getLocation();
        if (newLoc == this.myLoc) {
            this.samePosition = this.samePosition + 1;
        } else {
            this.samePosition = 0;
        }
        this.myLoc = newLoc;
        this.toEnemyHQ = this.myLoc.directionTo(this.info.enemyHQLoc);
        int strategy = Radio.readOldData(rc, Radio.STRATEGY);
        switch(strategy) {
            case 0: this.econ(rc); break;
            case 1: this.defend(rc); break;
            default: this.econ(rc); break;
        }
    }

    private void econ(RobotController rc) throws GameActionException {
        int numSoldiers = Radio.readOldData(rc, Radio.NUM_SOLDIERS);
        MapLocation gatherPoint = this.info.myHQLoc.add(this.info.toEnemyHQ, 5);
        if (this.id % 8 == 0) {
            if (this.moveOrEncamp(rc)) {
                return;
            }
        }
        if (numSoldiers < 15) {
            if (this.samePosition < 15) {
                this.movement(rc, gatherPoint);
            } else {
                this.moveOrDefuse(rc, gatherPoint);
            }
            return;
        }
        GameObject[] enemies = rc.senseNearbyGameObjects(
            Robot.class, this.myLoc, 6 * 6, this.info.opponent);
        GameObject[] friends = rc.senseNearbyGameObjects(
            Robot.class, this.myLoc, 3 * 3, this.info.myTeam);
        if (enemies.length > friends.length) {
            if (friends.length > 7) {
                this.movement(rc, this.getClosest(rc, friends));
            } else {
                this.movement(rc, gatherPoint);
            }
        } else {
            if (enemies.length == 0) {
                this.moveOrDefuse(rc, this.info.enemyHQLoc);
            } else {
                if (this.samePosition < 20) {
                    this.movement(rc, this.getClosest(rc, enemies));
                } else {
                    this.moveOrDefuse(rc, this.getClosest(rc, enemies));
                }
            }
        }
    }

    private void defend(RobotController rc) throws GameActionException {
        MapLocation enemyLoc = Radio.readLocation(rc, Radio.ENEMY);
        if (enemyLoc == null) {
            enemyLoc = this.info.myHQLoc;
        }
        this.moveOrDefuse(rc, enemyLoc);
    }

    private void moveOrDefuse(RobotController rc, MapLocation target) throws GameActionException {
        if (this.movement(rc, target)) {
            return;
        } else if (this.defuse(rc, target)) {
            return;
        }
    }

    private boolean moveOrEncamp(RobotController rc) throws GameActionException {
        if (Radio.readOldData(rc, Radio.NUM_ENCAMPMENTS) >= this.info.maxEncampments) {
            return false;
        }
        if (this.encampLoc == null) {
            MapLocation[] encamps = rc.senseEncampmentSquares(this.myLoc, 10 * 10, Team.NEUTRAL);
            if (encamps.length == 0) {
                return false;
            }
            this.encampLoc = encamps[this.rand.nextInt(encamps.length)];
        }
        boolean onEncampment = rc.senseEncampmentSquare(this.myLoc);
        if (!onEncampment) {
            if ((this.info.distance(this.myLoc, this.encampLoc) == 1) && (rc.senseObjectAtLocation(this.encampLoc) != null)) {
                this.encampLoc = null;
                MapLocation[] encamps = rc.senseEncampmentSquares(this.myLoc, 15 * 15, Team.NEUTRAL);
                if (encamps.length == 0) {
                    return false;
                }
                this.encampLoc = encamps[this.rand.nextInt(encamps.length)];
            }
            this.moveOrDefuse(rc, this.encampLoc);
        } else {
            if (this.info.teamPower < 200) {
                rc.captureEncampment(RobotType.GENERATOR);
            } else {
                rc.captureEncampment(RobotType.SUPPLIER);
            }
        }
        return true;
    }

    private boolean movement(RobotController rc, MapLocation target) throws GameActionException {
        Direction[] dirOptions = this.getDirectionsTo(rc, target);
        int len = dirOptions.length;
        for (int i=0; i < len; i++) {
            Direction nextDir = dirOptions[i];
            MapLocation nextLoc = this.myLoc.add(nextDir);
            if (this.hasEnemyMine(rc, nextLoc)) {
                continue;
            }
            if (this.canMove(rc, nextDir)) {
                rc.move(nextDir);
                return true;
            }
        }
        return false;
    }

    private boolean defuse(RobotController rc, MapLocation target) throws GameActionException {
        Direction[] dirOptions = this.getDirectionsTo(rc, target);
        int len = dirOptions.length;
        for (int i=0; i < len; i++) {
            Direction nextDir = dirOptions[i];
            MapLocation nextLoc = this.myLoc.add(nextDir);
            if (this.hasEnemyMine(rc, nextLoc)) {
                rc.defuseMine(nextLoc);
                return true;
            }
        }
        return false;
    }

    private int getMoveCost(RobotController rc, MapLocation senseLoc, int senseRadius) {
        if (senseLoc.x > this.info.mapWidth ||
                senseLoc.x < 0 ||
                senseLoc.y > this.info.mapHeight ||
                senseLoc.y < 0) {
            return 999;
        }
        return (
            rc.senseNearbyGameObjects(Robot.class, senseLoc, senseRadius * senseRadius, this.info.opponent).length * 2 +
            rc.senseNonAlliedMineLocations(senseLoc, ((senseRadius - 1) * (senseRadius - 1))).length);
    }

    private Direction[] getDirectionsTo(RobotController rc, MapLocation target) throws GameActionException {
        Direction targetDir = this.myLoc.directionTo(target);
        boolean rotateLeft = this.rand.nextInt(1) > 0;
        Direction[] dirLocs = new Direction[] {
            targetDir,
            rotateLeft ? targetDir.rotateRight() : targetDir.rotateLeft(),
            rotateLeft ? targetDir.rotateLeft() : targetDir.rotateRight()
        };
        MapLocation centerSenseLoc = this.info.locationInDir(this.myLoc, dirLocs[0], 4, 0);
        int numCenter = this.getMoveCost(rc, centerSenseLoc, 5);
        MapLocation rightSenseLoc = this.info.locationInDir(this.myLoc, dirLocs[1], 4, 4);
        int numRight = this.getMoveCost(rc, rightSenseLoc, 5);
        MapLocation leftSenseLoc = this.info.locationInDir(this.myLoc, dirLocs[2], 4, -4);
        int numLeft = this.getMoveCost(rc, leftSenseLoc, 5);
        int min = 9999;
        int[] counts = {numCenter, numRight, numLeft};
        for (int i=0; i < counts.length; i++) {
            if (counts[i] < min) {
                min = counts[i];
                targetDir = dirLocs[i];
            }
        }
        rotateLeft = this.rand.nextInt(1) > 0;
        return new Direction[]{
            targetDir,
            rotateLeft ? targetDir.rotateRight() : targetDir.rotateLeft(),
            rotateLeft ? targetDir.rotateLeft() : targetDir.rotateRight()
        };
    }

    private MapLocation getClosest(RobotController rc, GameObject[] objects) throws GameActionException {
        int len = objects.length;
        MapLocation[] locs = new MapLocation[len];
        for (int i=0; i < objects.length; i++) {
            locs[i] = rc.senseLocationOf(objects[i]);
        }
        return getClosest(rc, locs);
    }

    private MapLocation getClosest(RobotController rc, MapLocation[] objects) throws GameActionException {
        MapLocation closest = null;
        int minDistance = 999;
        int len = objects.length;
        for (int i=0; i < len; i++) {
            MapLocation robotLoc = objects[i];
            int distance = this.info.distance(this.myLoc, robotLoc);
            if (distance < minDistance) {
                minDistance = distance;
                closest = robotLoc;
            }
        }
        return closest;
    }

    private boolean hasEnemyMine(RobotController rc, MapLocation target) {
        Team mineTeamOwner = rc.senseMine(target);
        return ((mineTeamOwner != null) && (mineTeamOwner != this.info.myTeam));
    }

    private boolean canMove(RobotController rc, Direction d) {
        if (d == Direction.OMNI || d == Direction.NONE) {
            return false;
        }
        return rc.canMove(d);
    }
}
