package oldMe;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.HashMap;
import java.util.Random;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.GameObject;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotType;
import battlecode.common.Robot;
import battlecode.common.Team;

public class RobotPlayer {
    public static HeadquartersManager hq = new HeadquartersManager();
    public static RobotManager rm = new RobotManager();

	public static void run(RobotController rc) {
        if (rc.getType() == RobotType.HQ) {
            RobotPlayer.move(hq, rc);
        } else if (rc.getType() == RobotType.SOLDIER) {
            RobotPlayer.move(rm, rc);
        }
    }

    private static void move(Manager m, RobotController rc) {
        while(true) {
            try {
                m.move(rc);
            } catch (Exception e) {
                e.printStackTrace();
            }
            // End turn
            rc.yield();
        }
    }
}

class MapInfo {
    final static int BIG = 0;
    final static int MEDIUM = 1;
    final static int SMALL = 2;

    int mapSize;
    public MapInfo(RobotController rc) {
        int area = rc.getMapHeight() * rc.getMapWidth();
        if (area < 1000) {
            this.mapSize = MapInfo.SMALL;
        } else if (area < 5000) {
            this.mapSize = MapInfo.MEDIUM;
        } else {
            this.mapSize = MapInfo.BIG;
        }
    }
}

class RobotState {
    final static int MOVE_NEAR_HQ = 0;
    final static int WAIT = 1;
    final static int RAID = 2;
    final static int ENEMY_PURSUIT = 3;

    int action;
    int robotID;
    GameObject pursuing;

    RobotState(int robotID) {
        this.robotID = robotID;
        this.action = RobotState.MOVE_NEAR_HQ;
    }
}

abstract class Manager {
    abstract void move(RobotController rc);
    abstract RobotState createRobotState(RobotController rc);
}

class HeadquartersManager extends Manager {
    MapInfo mapInfo;

    public RobotState createRobotState(RobotController rc) {
        return new RobotState(10);
    }

    public void move(RobotController rc) {
        try {
            if (this.mapInfo == null) {
                this.mapInfo = new MapInfo(rc);
            }
            if (rc.isActive()) {
                // Spawn a soldier
                MapLocation hqLoc = rc.getLocation();
                Direction origDir = hqLoc.directionTo(
                    rc.senseEnemyHQLocation()).rotateRight();

                Direction dir = origDir.rotateLeft();
                MapLocation spawnLoc = hqLoc.add(dir);
                boolean rotated = false;
                while (true) {
                    if (rc.canMove(dir) && (rc.senseMine(spawnLoc) == null)) {
                        rc.spawn(dir);
                        break;
                    } else if (dir == origDir) {
                        break;
                    } else {
                        dir = dir.rotateLeft();
                        spawnLoc = hqLoc.add(dir);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

class RobotManager extends Manager {
    MapInfo mapInfo;
    RobotState state;
    int raidDelay;
    int rallyPointDistance;

    public RobotState createRobotState(RobotController rc) {
        return new RobotState(rc.getRobot().getID());
    }

    public void changeRobotState(int newState) {
        this.state.action = newState;
    }

    public void move(RobotController rc) {
        try {
            if (this.state == null) {
                this.state = this.createRobotState(rc);
                this.mapInfo = new MapInfo(rc);
                if (this.mapInfo.mapSize == MapInfo.SMALL) {
                    this.raidDelay = 30;
                    this.rallyPointDistance = 2;
                } else if (this.mapInfo.mapSize == MapInfo.MEDIUM) {
                    this.raidDelay = 15;
                    this.rallyPointDistance = 3;
                } else {
                    this.raidDelay = 10;
                    this.rallyPointDistance = 4;
                }
            }
            if (rc.isActive()) {
                switch(this.state.action) {
                    case RobotState.RAID: this.raid(rc); break;
                    case RobotState.MOVE_NEAR_HQ: this.moveNearHQ(rc); break;
                    case RobotState.WAIT: this.waitPatiently(rc); break;
                    case RobotState.ENEMY_PURSUIT: this.pursueEnemy(rc); break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Direction[] getMoveDirections(RobotController rc, GameObject target)
            throws GameActionException {
        MapLocation loc = rc.getLocation();

        //Define possible directions to move
        Direction dir = loc.directionTo(rc.senseLocationOf(target));
        return this.randomizeDirection(rc, dir);
    }

    public Direction[] getMoveDirections(RobotController rc) {
        MapLocation loc = rc.getLocation();

        //Define possible directions to move
        Direction dir = loc.directionTo(rc.senseEnemyHQLocation());
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

    public void raid(RobotController rc, Direction[] dirArray) throws
            GameActionException {
        Team myTeam = rc.getTeam();
        MapLocation loc = rc.getLocation();
        GameObject[] go = rc.senseNearbyGameObjects(Robot.class, loc, 5, null);
        for (int i=0; i < go.length; i++) {
            GameObject obj = go[i];
            if (obj.getTeam() != myTeam) {
                this.state.action = RobotState.ENEMY_PURSUIT;
                this.state.pursuing = obj;
                this.pursueEnemy(rc);
                return;
            }
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
                    mineTeamOwner != myTeam)) {
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
            this.waitPatiently(rc);
        }
    }

    public void moveToTarget(RobotController rc, GameObject target) throws
            GameActionException {
        Team myTeam = rc.getTeam();
        MapLocation loc = rc.getLocation();
        Direction[] dirArray = this.getMoveDirections(rc, target);
        boolean canMove = false;
        Direction nextDir = null;
        MapLocation nextLoc = null;
        for (int i=0; i < dirArray.length; i++) {
            nextDir = dirArray[i];
            nextLoc = loc.add(nextDir);
            Team mineTeamOwner = rc.senseMine(nextLoc);
            if ((mineTeamOwner != null) && (
                    mineTeamOwner != myTeam)) {
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
            this.waitPatiently(rc);
            return;
        }
        Direction[] dirArray = this.getMoveDirections(rc);
        this.raid(rc, dirArray);
    }

    public void waitPatiently(RobotController rc) throws GameActionException {
        MapLocation loc = rc.getLocation();
        if (rc.senseMine(loc) == null) {
            rc.layMine();
            return;
        }
        //determine if raiding party is ready
        if ((Clock.getRoundNum() % this.raidDelay) == 0) {
            this.changeRobotState(RobotState.RAID);
        }
    }
}
