package team004.robot;

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

import team004.common.MapInfo;
import team004.common.RobotState;
import team004.interfaces.Manager;
import team004.hq.HeadquartersManager;

public class RobotManager extends Manager {
    public MapInfo mapInfo;
    RobotState state;
    int raidDelay;
    int rallyPointDistance;
    boolean init;
    Team myTeam;
    Team opponent;

    public RobotState createRobotState(RobotController rc) {
        return new RobotState(rc.getRobot().getID());
    }

    public void changeRobotState(int newState) {
        this.state.action = newState;
    }

    public void move(RobotController rc) throws GameActionException {
        if (!this.init) {
            this.state = this.createRobotState(rc);
            int distanceToEnemyHQ = this.distance(
                rc.senseHQLocation(), rc.senseEnemyHQLocation());

            this.mapInfo = new MapInfo(rc);
            if (this.mapInfo.mapSize == MapInfo.SMALL) {
                this.raidDelay = 30;
                this.rallyPointDistance = 2;
            } else if (this.mapInfo.mapSize == MapInfo.MEDIUM) {
                this.raidDelay = 30;
                this.rallyPointDistance = 6;
            } else {
                this.raidDelay = 40;
                this.rallyPointDistance = 10;
            }
            this.myTeam = rc.getTeam();
            this.opponent = this.myTeam.opponent();
            this.init = true;
        }
        if (rc.isActive()) {
            switch(this.state.action) {
                case RobotState.RAID: this.raid(rc); break;
                case RobotState.MOVE_NEAR_HQ: this.moveNearHQ(rc); break;
                case RobotState.WAIT: this.waitPatiently(rc, false); break;
                case RobotState.ENEMY_PURSUIT: this.pursueEnemy(rc); break;
                case RobotState.RETREAT: this.retreat(rc); break;
            }
        }
    }

    public Direction[] getMoveDirections(RobotController rc, MapLocation target)
            throws GameActionException {
        MapLocation loc = rc.getLocation();

        //Define possible directions to move
        Direction dir = loc.directionTo(target);
        return this.randomizeDirection(rc, dir);
    }

    public Direction[] getMoveDirections(RobotController rc, GameObject target)
            throws GameActionException {
        MapLocation loc = rc.getLocation();

        //Define possible directions to move
        MapLocation enemyLoc = rc.senseLocationOf(target);
        Direction dir = loc.directionTo(enemyLoc);
        return this.randomizeDirection(rc, dir);
    }

    public Direction[] getMoveDirections(RobotController rc) {
        MapLocation loc = rc.getLocation();

        //Define possible directions to move
        MapLocation enemyLoc = rc.senseEnemyHQLocation();
        Direction dir = loc.directionTo(enemyLoc);
        return this.randomizeDirection(rc, dir);
    }

    public Direction[] randomizeDirection(RobotController rc, Direction dir) {
        int myID = rc.getRobot().getID();
        Random rand = new Random(myID);
        Direction dirLeft = dir.rotateLeft();
        Direction dirRight = dir.rotateRight();
        Direction[] dirArray = new Direction[]{dir, dirLeft, dirRight};

        //Randomize direction
        int[] randIndexes = new int[]{rand.nextInt(3), rand.nextInt(3)};
        Direction tmp = dirArray[randIndexes[0]].rotateLeft();
        dirArray[randIndexes[0]] = dirArray[randIndexes[1]];
        dirArray[randIndexes[1]] = tmp.rotateRight();
        Collections.shuffle(Arrays.asList(dirArray));
        return dirArray;
    }

    public int distance(MapLocation start, MapLocation end) {
        int x1 = start.x;
        int x2 = end.x;
        int y1 = start.y;
        int y2 = end.y;
        return ((int) Math.sqrt((x1-x2)*(x1-x2) + (y1-y2)*(y1-y2)));
    }

    public void raid(RobotController rc) throws GameActionException {
        Direction[] dirArray = this.getMoveDirections(rc);
        this.raid(rc, dirArray);
    }

    public int chaseEnemies(RobotController rc, MapLocation loc) throws GameActionException {
        GameObject[] go = rc.senseNearbyGameObjects(Robot.class, loc, 25, null);
        ArrayList<GameObject> enemy = new ArrayList<GameObject>();
        ArrayList<GameObject> friendly = new ArrayList<GameObject>();
        for (int i=0; i < go.length; i++) {
            GameObject obj = go[i];
            if (obj.getTeam() == this.myTeam) {
                friendly.add(obj);
            } else {
                enemy.add(obj);
            }
        }
        if ((friendly.size() > 1) && (enemy.size() > 0)) {
            this.state.action = RobotState.ENEMY_PURSUIT;
            this.state.pursuing = enemy.get(0);
            this.pursueEnemy(rc);
            return RobotState.ENEMY_PURSUIT;
        } else if (friendly.size() < 1) {
            return RobotState.RETREAT;
        } else {
            return RobotState.RAID;
        }
    }

    public void raid(RobotController rc, Direction[] dirArray) throws
            GameActionException {
        MapLocation loc = rc.getLocation();
        int chase = this.chaseEnemies(rc, loc);
        if (chase == RobotState.RETREAT) {
            this.retreat(rc);
            return;
        } else if (chase == RobotState.ENEMY_PURSUIT) {
            return;
        }
        boolean defuse = false;
        boolean canMove = false;
        Direction nextDir = null;
        MapLocation nextLoc = null;
        for (int i=0; i < dirArray.length; i++) {
            nextDir = dirArray[i];
            nextLoc = loc.add(nextDir);
            Team mineTeamOwner = rc.senseMine(nextLoc);
            if ((mineTeamOwner != null) && (
                    mineTeamOwner != this.myTeam)) {
                if (!canMove) {
                    defuse = true;
                }
                continue;
            } else if (rc.canMove(nextDir)) {
                canMove = true;
                defuse = false;
                break;
            }
        }
        if (defuse) {
            rc.defuseMine(nextLoc);
        } else if (canMove) {
            rc.move(nextDir);
        } else {
            this.waitPatiently(rc, false);
        }
    }

    public void retreat(RobotController rc) throws GameActionException {
        MapLocation loc = rc.getLocation();
        GameObject[] go = rc.senseNearbyGameObjects(Robot.class, loc, 25, this.myTeam);
        rc.setIndicatorString(0, "" + go.length);
        if (go.length > 1) {
            this.changeRobotState(RobotState.RAID);
            this.raid(rc);
            return;
        }
        MapLocation hqLoc = rc.senseHQLocation();
        this.moveToTarget(rc, hqLoc);
    }

    public void moveToTarget(RobotController rc, MapLocation destination) throws GameActionException {
        MapLocation loc = rc.getLocation();
        Direction[] dirArray = this.getMoveDirections(rc, destination);
        this.moveInDirection(rc, loc, dirArray);
    }

    public void moveToTarget(RobotController rc, GameObject target) throws
            GameActionException {
        MapLocation loc = rc.getLocation();
        Direction[] dirArray = this.getMoveDirections(rc, target);
        this.moveInDirection(rc, loc, dirArray);
    }


    public void moveInDirection(
            RobotController rc, MapLocation loc, Direction[] dirArray) throws GameActionException {
        boolean canMove = false;
        Direction nextDir = null;
        MapLocation nextLoc = null;
        for (int i=0; i < dirArray.length; i++) {
            nextDir = dirArray[i];
            nextLoc = loc.add(nextDir);
            Team mineTeamOwner = rc.senseMine(nextLoc);
            if ((mineTeamOwner != null) && (
                    mineTeamOwner != this.myTeam)) {
                continue;
            } else if (rc.canMove(nextDir)) {
                rc.move(nextDir);
                return;
            }
        }
    }

    public void pursueEnemy(RobotController rc) throws GameActionException {
        boolean enemyGone = false;
        MapLocation enemyLoc = null;
        try {
            enemyLoc = rc.senseLocationOf(this.state.pursuing);
        } catch (GameActionException e) {
            enemyGone = true;
        }
        if (enemyGone) {
            this.state.action = RobotState.RAID;
            this.raid(rc);
        } else {
            this.moveToTarget(rc, this.state.pursuing);
        }
    }

    public void moveNearHQ(RobotController rc) throws GameActionException {
        MapLocation loc = rc.getLocation();
        MapLocation hqLoc = rc.senseHQLocation();
        if (this.distance(loc, hqLoc) >= this.rallyPointDistance) {
            this.changeRobotState(RobotState.WAIT);
            this.waitPatiently(rc, false);
            return;
        }
        Direction[] dirArray = this.getMoveDirections(rc);
        this.raid(rc, dirArray);
    }

    public void waitPatiently(RobotController rc, boolean delaying) throws GameActionException {
        MapLocation loc = rc.getLocation();
        //determine if raiding party is ready
        if ((Clock.getRoundNum() % this.raidDelay) == 0) {
            this.changeRobotState(RobotState.RAID);
            this.raid(rc);
            return;
        }
        if (delaying) {
            return;
        }
        if (rc.senseMine(loc) == null) {
            rc.layMine();
        }
    }
}
