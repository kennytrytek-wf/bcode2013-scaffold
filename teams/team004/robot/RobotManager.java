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
    int round;
    MapLocation enemyHQLoc;
    MapLocation myHQLoc;
    Team myTeam;
    Team opponent;
    MapLocation myLoc;
    MapLocation myPreviousLoc;
    int roundsInSameLoc;
    GameObject currentEnemy;
    MapLocation gatherPoint;

    public RobotManager(RobotController rc) throws GameActionException {
        this.initialize(rc);
    }

    private void initialize(RobotController rc) throws GameActionException {
        this.round = Clock.getRoundNum() - 1;
        this.enemyHQLoc = rc.senseEnemyHQLocation();
        this.myHQLoc = rc.senseHQLocation();
        this.myTeam = rc.getTeam();
        this.opponent = this.myTeam.opponent();
        this.roundsInSameLoc = 0;
        this.currentEnemy = null;
        this.gatherPoint = this.calculateGatherPoint();
    }

    private MapLocation calculateGatherPoint() {
        boolean minusX = this.enemyHQLoc.x < this.myHQLoc.x;
        int endX = minusX ? this.myHQLoc.x - 6 : this.myHQLoc.x + 6;
        if (this.enemyHQLoc.x == this.myHQLoc.x) {
            endX = this.myHQLoc.x;
        }
        boolean minusY = this.enemyHQLoc.y < this.myHQLoc.y;
        int endY = minusY ? this.myHQLoc.y - 6 : this.myHQLoc.y + 6;
        if (this.enemyHQLoc.y == this.myHQLoc.y) {
            endY = this.myHQLoc.y;
        }
        return new MapLocation(endX, endY);
    }

    public void move(RobotController rc) throws GameActionException {
        this.round += 1;
        this.myLoc = rc.getLocation();
        if (this.myLoc == this.myPreviousLoc) {
            this.roundsInSameLoc += 1;
        } else {
            this.roundsInSameLoc = 0;
            this.myPreviousLoc = this.myLoc;
        }
        MapLocation enemyLoc = this.readEnemyLocBroadcast(rc);
        if (enemyLoc == null) {
            enemyLoc = this.senseEnemy(rc);
            if (enemyLoc == null) {
                this.writeEnemyLocBroadcast(rc, new MapLocation(0, 0));
            }
        }
        if (enemyLoc != null) {
            rc.setIndicatorString(0, "Found enemy. Attacking: (" + enemyLoc.x + ", " + enemyLoc.y + ")");
            this.attack(rc, enemyLoc, this.roundsInSameLoc > 10);
        } else if (this.round < 200) {
            rc.setIndicatorString(0, "Gathering at round " + this.round + ". Gather point: (" + this.gatherPoint.x + ", " + this.gatherPoint.y + ")");
            this.gather(rc);
        } else if (this.round >= 200) {
            rc.setIndicatorString(0, ". Attacking at round " + this.round + ". Enemy HQ: (" + this.enemyHQLoc.x + ", " + this.enemyHQLoc.y + ")");
            this.attack(rc, this.enemyHQLoc, true);
        }
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
            GameObject[] go = rc.senseNearbyGameObjects(
                Robot.class, this.myLoc, 100, this.opponent);

            if (go.length == 0) {
                return null;
            }
            enemy = go[0];
            enemyLoc = rc.senseLocationOf(enemy);
        }
        this.writeEnemyLocBroadcast(rc, enemyLoc);
        return enemyLoc;
    }

    private int getBroadcastChannel() {
        return 58293;
    }

    private void writeEnemyLocBroadcast(RobotController rc, MapLocation loc) throws GameActionException {
        int channel = this.getBroadcastChannel();
        int x = ((loc.x << 8) & 0xFF00);
        int y = loc.y & 0x00FF;
        int encodedLocation = x | y;
        rc.broadcast(channel, encodedLocation);
        rc.setIndicatorString(1, "Broadcasting enemy location: " + encodedLocation);
    }

    private MapLocation readEnemyLocBroadcast(RobotController rc) throws GameActionException {
        int message = rc.readBroadcast(this.getBroadcastChannel());
        rc.setIndicatorString(2, "Read enemy location: " + message);
        if (message == 0) {
            return null;
        }
        int x = (message & 0xFF00) >> 8;
        int y = message & 0x00FF;
        MapLocation enemyLoc = new MapLocation(x, y);
        try {
            GameObject obj = rc.senseObjectAtLocation(enemyLoc);
            if (obj == null) {
                return null;
            }
            if (obj.getTeam() == this.opponent) {
                return enemyLoc;
            }
            return null;
        } catch (GameActionException e) { //too far away to sense it
            return enemyLoc;
        }
    }

    private int distance(MapLocation start, MapLocation end) {
        int x1 = start.x;
        int x2 = end.x;
        int y1 = start.y;
        int y2 = end.y;
        return ((int) Math.sqrt((x1-x2)*(x1-x2) + (y1-y2)*(y1-y2)));
    }

    private Direction[] getDirectionsTo(MapLocation target) throws GameActionException {
        Direction targetDir = this.myLoc.directionTo(target);
        return new Direction[]{
            targetDir,
            targetDir.rotateLeft(),
            targetDir.rotateRight()
        };
    }

    private void gather(RobotController rc) throws GameActionException {
        if (this.distance(this.myLoc, this.gatherPoint) <= 4) {
            return;
        }
        this.attack(rc, this.gatherPoint, true);
    }

    private void attack(RobotController rc, MapLocation target, boolean defuseMines) throws GameActionException {
        Direction[] dirArray = this.getDirectionsTo(target);
        boolean defuse = false;
        boolean canMove = false;
        Direction nextDir = null;
        MapLocation nextLoc = null;
        for (int i=0; i < dirArray.length; i++) {
            nextDir = dirArray[i];
            nextLoc = this.myLoc.add(nextDir);
            Team mineTeamOwner = rc.senseMine(nextLoc);
            if ((mineTeamOwner != null) && (
                    mineTeamOwner != this.myTeam)) {
                if (!canMove && defuseMines) {
                    defuse = true;
                }
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
        }
    }
}

/*
public class RobotManager extends Manager {
    public MapInfo mapInfo;
    RobotState state;
    int raidDelay;
    int rallyPointDistance;
    boolean init;
    Team myTeam;
    Team opponent;
    boolean enableRetreat;
    MapLocation currentLoc;
    int roundsAtCurrentLoc;
    boolean encamping;

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
                this.enableRetreat = false;
            } else if (this.mapInfo.mapSize == MapInfo.MEDIUM) {
                this.raidDelay = 30;
                this.rallyPointDistance = 4;
                this.enableRetreat = true;
            } else {
                this.raidDelay = 40;
                this.rallyPointDistance = 5;
                this.enableRetreat = true;
            }
            this.myTeam = rc.getTeam();
            this.opponent = this.myTeam.opponent();
            this.currentLoc = new MapLocation(0, 0);
            this.encamping = false;
            this.init = true;
        }
        if (rc.isActive() && !this.encamping) {
            MapLocation loc = rc.getLocation();
            if (loc == this.currentLoc) {
                this.roundsAtCurrentLoc += 1;
                if (this.roundsAtCurrentLoc > this.raidDelay) {
                    this.changeRobotState(RobotState.RAID);
                }
            } else {
                this.roundsAtCurrentLoc = 0;
            }
            this.currentLoc = loc;
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
        MapLocation loc = this.currentLoc;

        //Define possible directions to move
        Direction dir = loc.directionTo(target);
        return this.randomizeDirection(rc, dir);
    }

    public Direction[] getMoveDirections(RobotController rc, GameObject target)
            throws GameActionException {
        MapLocation loc = this.currentLoc;

        //Define possible directions to move
        MapLocation enemyLoc = rc.senseLocationOf(target);
        Direction dir = loc.directionTo(enemyLoc);
        return this.randomizeDirection(rc, dir);
    }

    public Direction[] getMoveDirections(RobotController rc) {
        MapLocation loc = this.currentLoc;

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
        int[] randIndexes = new int[]{rand.nextInt(2) + 1, rand.nextInt(2) + 1};
        Direction tmp = dirArray[randIndexes[0]].rotateLeft();
        dirArray[randIndexes[0]] = dirArray[randIndexes[1]];
        dirArray[randIndexes[1]] = tmp.rotateRight();
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
        if ((friendly.size() > 2) && (enemy.size() > 0)) {
            this.state.action = RobotState.ENEMY_PURSUIT;
            this.state.pursuing = enemy.get(0);
            this.pursueEnemy(rc);
            return RobotState.ENEMY_PURSUIT;
        } else if ((friendly.size() < 2) && (this.enableRetreat)) {
            return RobotState.RETREAT;
        } else {
            return RobotState.RAID;
        }
    }

    public void raid(RobotController rc, Direction[] dirArray) throws
            GameActionException {
        MapLocation loc = this.currentLoc;
        int chase = this.chaseEnemies(rc, loc);
        if (chase == RobotState.RETREAT) {
            this.retreat(rc);
            return;
        } else if (chase == RobotState.ENEMY_PURSUIT) {
            return;
        }
        if (rc.senseEncampmentSquare(loc)) {
            rc.captureEncampment(RobotType.ARTILLERY);
            this.encamping = true;
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
        MapLocation loc = this.currentLoc;
        GameObject[] go = rc.senseNearbyGameObjects(Robot.class, loc, 25, this.myTeam);
        if (go.length > 2) {
            this.changeRobotState(RobotState.RAID);
            this.raid(rc);
            return;
        }
        MapLocation hqLoc = rc.senseHQLocation();
        this.moveToTarget(rc, hqLoc);
    }

    public void moveToTarget(RobotController rc, MapLocation destination) throws GameActionException {
        MapLocation loc = this.currentLoc;
        Direction[] dirArray = this.getMoveDirections(rc, destination);
        this.moveInDirection(rc, loc, dirArray);
    }

    public void moveToTarget(RobotController rc, GameObject target) throws
            GameActionException {
        MapLocation loc = this.currentLoc;
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
        MapLocation loc = this.currentLoc;
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
        MapLocation loc = this.currentLoc;
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
*/
