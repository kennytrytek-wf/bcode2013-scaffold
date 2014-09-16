package team031.robot;

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

import team031.common.Info;
import team031.common.Radio;
import team031.common.RobotState;
import team031.interfaces.Manager;
import team031.hq.HeadquartersManager;


public class RobotManager extends Manager {
    Info info;
    MapLocation myLoc;
    MapLocation myPreviousLoc;
    int roundsInSameLoc;
    GameObject currentEnemy;
    Random rand;
    int attackDir;
    int maxEncampments;
    int numEncampments;
    boolean laidMineLastRound;

    public RobotManager(RobotController rc) throws GameActionException {
        this.initialize(rc);
    }

    private void initialize(RobotController rc) throws GameActionException {
        this.info = new Info(rc);
        this.myLoc = rc.getLocation();
        this.myPreviousLoc = this.myLoc;
        this.roundsInSameLoc = 0;
        this.currentEnemy = null;
        this.rand = new Random(rc.getRobot().getID());
        this.attackDir = this.rand.nextInt(3);
        this.maxEncampments = 15;
        this.numEncampments = 0;
        this.laidMineLastRound = false;
    }

    public void move(RobotController rc) throws GameActionException {
        this.info.update(rc);
        if (!rc.isActive()) {
            return;
        }
        this.myLoc = rc.getLocation();
        if (this.establishOutpost(rc)) {
            this.numEncampments += 1;
            return;
        }
        if (this.myLoc == this.myPreviousLoc) {
            this.roundsInSameLoc += 1;
        } else {
            this.roundsInSameLoc = 0;
            this.myPreviousLoc = this.myLoc;
        }
        MapLocation movingTarget = null;
        if (this.shouldGather(rc)) {
            if (this.layMine(rc)) {
                return;
            }
            movingTarget = this.getEnemyOrFriend(rc, 0, this.info.gatherPoint);
            rc.setIndicatorString(0, "Gathering at round " + this.info.round + ". Gather point: (" + this.info.gatherPoint.x + ", " + this.info.gatherPoint.y + ")");
        } else {
            movingTarget = this.getEnemyOrFriend(rc, 2, null);
        }
        this.attack(rc, movingTarget, this.roundsInSameLoc > 3, true);
    }

    private boolean shouldGather(RobotController rc) throws GameActionException {
        return ((this.info.round/250) % 2 == 0);
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
                this.info.strategicPoint = outpost;
                return false;
            } else {
                Radio.writeLocation(rc, Radio.OUTPOST, new MapLocation(0, 0));
                this.info.strategicPoint = null;
            }
        }
        if (onEncampment && this.numEncampments < this.maxEncampments) {
            if (this.info.distance(this.myLoc, this.info.myHQLoc) > 3) {
                if (this.info.teamPower < 150) {
                    switch (this.rand.nextInt(3)) {
                        case 0:
                        case 1: rc.captureEncampment(RobotType.ARTILLERY); break;
                        case 2: rc.captureEncampment(RobotType.GENERATOR); break;
                    }
                } else if (this.info.teamPower > 500) {
                    switch (this.rand.nextInt(2)) {
                        case 0: rc.captureEncampment(RobotType.ARTILLERY); break;
                        case 1: rc.captureEncampment(RobotType.SUPPLIER); break;
                    }
                } else {
                    switch (this.rand.nextInt(4)) {
                        case 0:
                        case 1:
                        case 2: rc.captureEncampment(RobotType.ARTILLERY); break;
                        case 3: rc.captureEncampment(RobotType.MEDBAY); break;
                    }
                }
                if (onOutpostEncampment) {
                    Radio.writeLocation(rc, Radio.OUTPOST, this.myLoc);
                }
                return true;
            }
        }
        return false;
    }

    private MapLocation getEnemyOrFriend(RobotController rc, int minimumFriends, MapLocation defaultLocation) throws GameActionException {
        GameObject[] friends = rc.senseNearbyGameObjects(
            Robot.class, this.myLoc, 4 * 4, this.info.myTeam);

        GameObject[] enemies = rc.senseNearbyGameObjects(
            Robot.class, this.myLoc, 4 * 4, this.info.opponent);

        GameObject[] arr = null;
        MapLocation defaultLoc = defaultLocation;
        if ((friends.length > enemies.length) && (friends.length > minimumFriends)) {
            MapLocation publicEnemy = Radio.readLocation(rc, Radio.ENEMY);
            if (publicEnemy != null) {
                return publicEnemy;
            }
            arr = enemies;
            if (defaultLoc == null) {
                defaultLoc = this.info.enemyHQLoc;
            }
            rc.setIndicatorString(1, "Found enemy. Attacking. Friends: " + friends.length + ", Enemies: " + enemies.length);
        } else {
            arr = friends;
            if (defaultLoc == null) {
                defaultLoc = this.info.myHQLoc;
            }
            rc.setIndicatorString(1, "Need more friends! Friends: " + friends.length + ", Enemies: " + enemies.length);
        }
        MapLocation closest = defaultLoc;
        int minDistance = 999;
        for (int i=0; i < arr.length; i++) {
            MapLocation robotLoc = rc.senseLocationOf(arr[i]);
            int distance = this.info.distance(this.myLoc, robotLoc);
            if (distance < minDistance) {
                minDistance = distance;
                closest = robotLoc;
            }
        }
        Radio.writeLocation(rc, Radio.ENEMY, closest);
        return closest;
    }

    private boolean layMine(RobotController rc) throws GameActionException {
        MapLocation publicEnemy = Radio.readLocation(rc, Radio.ENEMY);
        GameObject[] enemies = rc.senseNearbyGameObjects(
            Robot.class, this.myLoc, 10 * 10, this.info.opponent);

        if (enemies.length > 0 ||
                publicEnemy != null ||
                this.info.distance(this.myLoc, this.info.myHQLoc) > 10) {
            return false;
        }
        if (!this.laidMineLastRound && rc.senseMine(this.myLoc) == null) {
            rc.layMine();
            this.laidMineLastRound = true;
            return true;
        }
        this.laidMineLastRound = false;
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
            rc.senseNearbyGameObjects(Robot.class, senseLoc, senseRadius * senseRadius, this.info.opponent).length +
            rc.senseNonAlliedMineLocations(senseLoc, ((senseRadius - 1) * (senseRadius - 1))).length);
    }

    private Direction[] getDirectionsTo(RobotController rc, MapLocation target) throws GameActionException {
        Direction targetDir = this.myLoc.directionTo(target);
        Direction[] dirLocs = new Direction[] {
            targetDir,
            targetDir.rotateRight(),
            targetDir.rotateLeft()
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
            //preferredDir.rotateRight().rotateRight(),
            //preferredDir.rotateLeft().rotateLeft()
        };
        Direction retreatDir = this.myLoc.directionTo(this.info.myHQLoc);
        Direction[] retreatDirOptions = {
            retreatDir,
            retreatDir.rotateRight(),
            retreatDir.rotateLeft(),
            //retreatDir.rotateRight().rotateRight(),
            //retreatDir.rotateLeft().rotateLeft()
        };
        if (distanceToEnemy > 2) {
            for (int i=0; i < attackDirOptions.length; i++) {
                Direction dir = attackDirOptions[i];
                MapLocation nextLoc = this.myLoc.add(dir);
                if (rc.canMove(dir) && !this.hasEnemyMine(rc, nextLoc)) {
                    return dir;
                }
            }
            return attackDirOptions[0];
        }
        //If distance == 2, determine if I have enough friends to attack. If not, retreat.
        else {
            MapLocation senseLoc = this.myLoc.add(attackDirOptions[0]);
            GameObject[] go = rc.senseNearbyGameObjects(
                Robot.class, senseLoc, 2, this.info.myTeam);

            if (go.length > 1) {
                for (int i=0; i < attackDirOptions.length; i++) {
                    Direction dir = attackDirOptions[i];
                    MapLocation nextLoc = this.myLoc.add(dir);
                    if (rc.canMove(dir) && !this.hasEnemyMine(rc, nextLoc)) {
                        return dir;
                    }
                }
                return attackDirOptions[0];
            } else {
                for (int i=0; i < retreatDirOptions.length; i++) {
                    Direction dir = retreatDirOptions[i];
                    MapLocation nextLoc = this.myLoc.add(dir);
                    if (rc.canMove(dir) && !this.hasEnemyMine(rc, nextLoc)) {
                        return dir;
                    }
                }
                return retreatDirOptions[0];
            }
        }
        //If distance == 1, determine if I have enough friends to attack. If not, retreat toward outpost or HQ. If so, attack.
        //If distance == 0, continue the attack
        //return null;
    }

    private void attack(RobotController rc, MapLocation target, boolean defuseMines, boolean enemySighted) throws GameActionException {
        Direction[] dirArray = this.getDirectionsTo(rc, target);
        boolean defuse = false;
        boolean canMove = false;
        Direction nextDir = dirArray[0];
        MapLocation nextLoc = this.myLoc.add(nextDir);
        for (int i=0; i < dirArray.length; i++) {
            nextDir = dirArray[i];
            nextLoc = this.myLoc.add(nextDir);
            if (this.hasEnemyMine(rc, nextLoc)) {
                defuse = (!canMove && defuseMines);
            }
            else {
                if (enemySighted) {
                    nextDir = this.getAttackDirection(rc, target, nextDir);
                    nextLoc = this.myLoc.add(nextDir);
                    if (this.hasEnemyMine(rc, nextLoc)) {
                        defuse = defuseMines;
                        canMove = false;
                        continue;
                    } else {
                        canMove = rc.canMove(nextDir);
                        defuse = false;
                    }
                    if (canMove) {
                        break;
                    }
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
        }
    }
}
