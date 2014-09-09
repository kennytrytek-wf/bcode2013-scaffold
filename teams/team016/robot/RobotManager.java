package team016.robot;

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

import team016.common.Info;
import team016.common.Radio;
import team016.common.RobotState;
import team016.interfaces.Manager;
import team016.hq.HeadquartersManager;


public class RobotManager extends Manager {
    Info info;
    MapLocation myLoc;
    MapLocation myPreviousLoc;
    int roundsInSameLoc;
    GameObject currentEnemy;
    boolean isCanary;
    Random rand;
    int attackDir;

    public RobotManager(RobotController rc) throws GameActionException {
        this.initialize(rc);
    }

    private void initialize(RobotController rc) throws GameActionException {
        this.info = new Info(rc);
        this.roundsInSameLoc = 0;
        this.currentEnemy = null;
        this.isCanary = false;
        this.rand = new Random(rc.getRobot().getID());
        this.attackDir = this.rand.nextInt(3);
    }

    public void move(RobotController rc) throws GameActionException {
        this.info.update(rc);
        this.myLoc = rc.getLocation();
        if (this.establishCanary(rc)) {
            return;
        }
        if (this.establishOutpost(rc)) {
            return;
        }
        if (this.myLoc == this.myPreviousLoc) {
            this.roundsInSameLoc += 1;
        } else {
            this.roundsInSameLoc = 0;
            this.myPreviousLoc = this.myLoc;
        }
        MapLocation enemyLoc = this.getEnemyLocation(rc);
        if ((this.shouldGather() && this.info.round > 400)) {
            enemyLoc = null;
            this.currentEnemy = null;
        }
        if (enemyLoc == null) {
            enemyLoc = this.senseEnemy(rc);
            if (enemyLoc == null) {
                Radio.writeLocation(rc, Radio.ENEMY, new MapLocation(0, 0));
            }
        }
        if (enemyLoc != null) {
            rc.setIndicatorString(0, "Found enemy. Attacking: (" + enemyLoc.x + ", " + enemyLoc.y + ")");
            boolean defuse = false;
            if (this.roundsInSameLoc > 10) {
                defuse = this.rand.nextInt(15) == 1;
            }
            this.attack(rc, enemyLoc, defuse, true);
        } else if (this.shouldGather() && !(this.info.round > 200 && this.info.teamPower < 150)) {
            rc.setIndicatorString(0, "Gathering at round " + this.info.round + ". Gather point: (" + this.info.gatherPoint.x + ", " + this.info.gatherPoint.y + ")");
            this.gather(rc);
        } else {
            rc.setIndicatorString(0, ". Attacking at round " + this.info.round + ". Enemy HQ: (" + this.info.enemyHQLoc.x + ", " + this.info.enemyHQLoc.y + ")");
            this.attack(rc, this.info.enemyHQLoc, true, false);
        }
    }

    private boolean shouldGather() {
        return (this.info.round/250) % 2 == 0;
    }

    private boolean establishCanary(RobotController rc) throws GameActionException {
        if ((!this.isCanary) && (this.info.distance(this.myLoc, this.info.myHQLoc) < 3)) {
            if (!Radio.readStatus(rc, Radio.CANARY)) {
                this.isCanary = true;
                Radio.writeStatus(rc, Radio.CANARY, true);
                rc.setIndicatorString(1, "I've become the canary.");
            }
        }
        if (this.isCanary) {
            this.gather(rc);
            MapLocation enemyLoc = this.senseEnemy(rc);
            if (enemyLoc != null) {
                this.isCanary = false;
                Radio.writeStatus(rc, Radio.CANARY, false);
                Radio.writeLocation(rc, Radio.ENEMY, enemyLoc);
                rc.setIndicatorString(1, "No longer the canary.");
            }
            return true;
        }
        return false;
    }

    private boolean establishOutpost(RobotController rc) throws GameActionException {
        MapLocation outpost = Radio.readLocation(rc, Radio.OUTPOST);
        boolean onEncampment = rc.senseEncampmentSquare(this.myLoc);
        boolean onOutpostEncampment = (
            onEncampment && !(
                this.myLoc.x + 1 == rc.getMapWidth() ||
                this.myLoc.y + 1 == rc.getMapHeight() ||
                this.myLoc.x == 0 ||
                this.myLoc.y == 0)
        );
        int deltaDistance = 9999;
        if (outpost != null) {
            if (onOutpostEncampment) {
                int origOutpostDistance = this.info.distance(outpost, this.info.myHQLoc);
                int newOutpostDistance = this.info.distance(this.myLoc, this.info.myHQLoc);
                deltaDistance = newOutpostDistance - origOutpostDistance;
            }
            boolean outpostExists = true;
            GameObject outpostObj = null;
            try {
                outpostObj = rc.senseObjectAtLocation(outpost);
                outpostExists = outpostObj != null;
            } catch (GameActionException e) {
            }
            if (outpostExists && (deltaDistance >= 0 || deltaDistance == 9999)) {
                if (this.info.distance(outpost, this.info.myHQLoc) > 10) {
                    this.info.strategicPoint = outpost;
                    //this.info.locationBetween(
                    //    this.info.myHQLoc, outpost, 0.40, rc);
                } else {
                    this.info.strategicPoint = outpost;
                }

                return false;
            } else {
                Radio.writeLocation(rc, Radio.OUTPOST, new MapLocation(0, 0));
                this.info.strategicPoint = null;
            }
        }
        if (onEncampment) {
            if (this.info.distance(this.myLoc, this.info.myHQLoc) > 5) {
                if (this.info.teamPower < 150) {
                    rc.captureEncampment(RobotType.GENERATOR);
                    return true;
                } else if (this.info.teamPower > 500) {
                    rc.captureEncampment(RobotType.SUPPLIER);
                }
                if (onOutpostEncampment) {
                    Radio.writeLocation(rc, Radio.OUTPOST, this.myLoc);
                    switch (this.rand.nextInt(4)) {
                        case 0:
                        case 1:
                        case 2: rc.captureEncampment(RobotType.ARTILLERY); break;
                        case 3: rc.captureEncampment(RobotType.MEDBAY); break;
                    }
                    return true;
                }
            }
        }
        return false;
    }

    private MapLocation senseEnemy(RobotController rc) throws GameActionException {
        GameObject enemy = this.currentEnemy;
        MapLocation enemyLoc = new MapLocation(0, 0);
        if (enemy != null) {
            try {
                enemyLoc = rc.senseLocationOf(enemy);
            } catch (GameActionException e) {
                enemy = null;
                this.currentEnemy = null;
            }
        }
        if (enemy == null) {
            MapLocation senseLoc = this.myLoc;
            if (this.shouldGather()) {
                senseLoc = this.info.gatherPoint;
            }
            GameObject[] go = rc.senseNearbyGameObjects(
                Robot.class, senseLoc, 33 * 33, this.info.opponent);

            if (go.length == 0) {
                return null;
            }
            enemy = go[0];
            enemyLoc = rc.senseLocationOf(enemy);
        }
        Radio.writeLocation(rc, Radio.ENEMY, enemyLoc);
        return enemyLoc;
    }

    private MapLocation getEnemyLocation(RobotController rc) throws GameActionException {
        MapLocation enemyLoc = Radio.readLocation(rc, Radio.ENEMY);
        if (enemyLoc == null) {
            return null;
        }
        try {
            GameObject obj = rc.senseObjectAtLocation(enemyLoc);
            if (obj == null) {
                return null;
            }
            if (obj.getTeam() == this.info.opponent) {
                return enemyLoc;
            }
            return null;
        } catch (GameActionException e) { //too far away to sense it
            return enemyLoc;
        }
    }

    private Direction[] getDirectionsTo(MapLocation target) throws GameActionException {
        Direction targetDir = this.myLoc.directionTo(target);
        return new Direction[]{
            targetDir,
            targetDir.rotateLeft(),
            targetDir.rotateRight()
        };
    }

    private boolean hasEnemyMine(RobotController rc, MapLocation target) {
        Team mineTeamOwner = rc.senseMine(target);
        return ((mineTeamOwner != null) && (mineTeamOwner != this.info.myTeam));
    }

    private Direction getAttackDirection(RobotController rc, MapLocation target, Direction preferredDir) {
        //Get distance between me and enemy
        int distanceToEnemy = this.info.distance(this.myLoc, target);
        //If distance > 2, find any direction toward enemy
        Direction[] attackDirOptions = {
            preferredDir,
            preferredDir.rotateRight(),
            preferredDir.rotateLeft(),
            preferredDir.rotateRight().rotateRight(),
            preferredDir.rotateLeft().rotateLeft()
        };
        Direction retreatDir = this.myLoc.directionTo(this.info.myHQLoc);
        Direction[] retreatDirOptions = {
            retreatDir,
            retreatDir.rotateRight(),
            retreatDir.rotateLeft(),
            retreatDir.rotateRight().rotateRight(),
            retreatDir.rotateLeft().rotateLeft()
        };
        if (distanceToEnemy > 2) {
            for (int i=0; i < attackDirOptions.length; i++) {
                Direction dir = attackDirOptions[i];
                MapLocation nextLoc = this.myLoc.add(dir);
                if (rc.canMove(dir) && !this.hasEnemyMine(rc, nextLoc)) {
                    return dir;
                }
            }
            return null;
        }
        //If distance == 2, determine if I have enough friends to attack. If not, retreat.
        if (distanceToEnemy == 2) {
            MapLocation senseLoc = this.myLoc.add(attackDirOptions[0]);
            GameObject[] go = rc.senseNearbyGameObjects(
                Robot.class, senseLoc, 4, this.info.myTeam);

            if (go.length > 1) {
                for (int i=0; i < attackDirOptions.length; i++) {
                    Direction dir = attackDirOptions[i];
                    MapLocation nextLoc = this.myLoc.add(dir);
                    if (rc.canMove(dir) && !this.hasEnemyMine(rc, nextLoc)) {
                        return dir;
                    }
                }
                return null;
            } else {
                for (int i=0; i < retreatDirOptions.length; i++) {
                    Direction dir = retreatDirOptions[i];
                    MapLocation nextLoc = this.myLoc.add(dir);
                    if (rc.canMove(dir) && !this.hasEnemyMine(rc, nextLoc)) {
                        return dir;
                    }
                }
            }
        }
        //If distance == 1, determine if I have enough friends to attack. If not, retreat toward outpost or HQ. If so, attack.
        //If distance == 0, continue the attack
        return null;
    }

    private void gather(RobotController rc) throws GameActionException {
        int distanceFromGather = this.info.distance(this.myLoc, this.info.gatherPoint);
        int distanceFromHQ = this.info.distance(this.myLoc, this.info.myHQLoc);
        if (distanceFromHQ > 5) {
            if (distanceFromGather < 2) {
                //rc.layMine();
                return;
            }
        }
        this.attack(rc, this.info.gatherPoint, true, false);
    }

    private void attack(RobotController rc, MapLocation target, boolean defuseMines, boolean enemySighted) throws GameActionException {
        Direction[] dirArray = this.getDirectionsTo(target);
        boolean defuse = false;
        boolean canMove = false;
        Direction nextDir = null;
        MapLocation nextLoc = null;
        for (int i=0; i < dirArray.length; i++) {
            nextDir = dirArray[i];
            nextLoc = this.myLoc.add(nextDir);
            if (this.hasEnemyMine(rc, nextLoc)) {
                defuse = (!canMove && defuseMines);
            }
            else {
                if (enemySighted) {
                    nextDir = this.getAttackDirection(rc, target, nextDir);
                    canMove = true;
                    defuse = false;
                    break;
                } else if (rc.canMove(nextDir)) {
                    canMove = true;
                    defuse = false;
                    break;
                }
            }
        }
        if (defuse) {
            rc.defuseMine(nextLoc);
        } else if (canMove) {
            rc.move(nextDir);
        } else {
            if (rc.senseNearbyGameObjects(Robot.class, this.myLoc, 33*33, this.info.opponent).length == 0) {
                //rc.layMine();
            }
        }
    }
}
