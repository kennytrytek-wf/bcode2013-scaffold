package team004;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.HashMap;
import java.util.Random;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotType;
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

class RobotState {
    final static int MOVE_NEAR_HQ = 0;
    final static int WAIT = 1;
    final static int RAID = 2;

    int action;
    int group;
    int robotID;

    RobotState(int robotID, int group) {
        this.robotID = robotID;
        this.group = group;
        this.action = RobotState.MOVE_NEAR_HQ;
    }
}

abstract class Manager {
    abstract void move(RobotController rc);
    abstract RobotState getOrCreateRobotState(int robotID);
}

class HeadquartersManager extends Manager {
    public RobotState getOrCreateRobotState(int robotID) {
        return new RobotState(10, 0);
    }

    public void move(RobotController rc) {
        try {
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
    static int currentGroup;
    static int groupCount;

    RobotManager() {
        this.currentGroup = 0;
        this.groupCount = 0; //HQ exists before all Robots
    }

    public RobotState getOrCreateRobotState(int robotID) {
        RobotState state = this.states.get(robotID);
        if (state == null) {
            state = new RobotState(robotID, this.currentGroup);
            this.states.put(robotID, state);
            //this.groups.get(currentGroup).add(state);
        }
        return state;
    }

    public void changeGroupState(RobotController rc, int newState) {
        RobotState currentState = this.getOrCreateRobotState(
                rc.getRobot().getID());

        Iterator<RobotState> groupIterator = this.groups.get(
            currentState.group).iterator();

        while (groupIterator.hasNext()) {
            RobotState s = groupIterator.next();
            s.action = newState;
            this.states.get(s.robotID).action = newState;
        }
    }

    public void changeRobotState(RobotController rc, int newState) {
        int myID = rc.getRobot().getID();
        RobotState state = this.getOrCreateRobotState(myID);
        state.action = newState;
    }

    public void move(RobotController rc) {
        try {
            this.moveNumber += 1;
            if (rc.isActive()) {
                int myID = rc.getRobot().getID();
                RobotState state = this.getOrCreateRobotState(myID);
                switch(state.action) {
                    case RobotState.RAID: this.raid(rc); break;
                    case RobotState.MOVE_NEAR_HQ: this.moveNearHQ(rc); break;
                    case RobotState.WAIT: this.waitPatiently(rc); break;
                }
            }
            if (this.moveNumber > 50) {
                this.moveNumber = 0;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Direction[] getMoveDirections(RobotController rc) {
        MapLocation loc = rc.getLocation();
        int myID = rc.getRobot().getID();
        Random rand = new Random(myID);

        //Define possible directions to move
        Direction dir = loc.directionTo(rc.senseEnemyHQLocation());
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
        MapLocation loc = rc.getLocation();
        Direction[] dirArray = this.getMoveDirections(rc);

        //Make a move
        boolean defuse = false;
        Direction nextDir = null;
        MapLocation nextLoc = null;
        for (int i=0; i < dirArray.length; i++) {
            nextDir = dirArray[i];
            nextLoc = loc.add(nextDir);
            Team mineTeamOwner = rc.senseMine(nextLoc);
            if ((mineTeamOwner != null) && (
                    mineTeamOwner != rc.getTeam())) {
                defuse = true;
                continue;
            } else if (rc.canMove(nextDir)) {
                defuse = false;
                break;
            }
        }
        if (defuse) {
            rc.defuseMine(nextLoc);
        } else {
            rc.move(nextDir);
        }
    }

    public void moveNearHQ(RobotController rc) throws GameActionException {
        MapLocation loc = rc.getLocation();
        MapLocation hqLoc = rc.senseHQLocation();
        if (this.distance(loc, hqLoc) >= 5) {
            this.changeRobotState(rc, RobotState.WAIT);
            this.waitPatiently(rc);
            return;
        }

        this.raid(rc);
    }

    public void waitPatiently(RobotController rc) throws GameActionException {
        MapLocation loc = rc.getLocation();

        /*if (rc.senseMine(loc) == null) {
            rc.layMine();
            return;
        }*/
        //determine if raiding party is ready
        if ((Clock.getRoundNum() % 50) == 0) {
            //this.changeGroupState(rc, RobotState.RAID);
            this.changeRobotState(rc, RobotState.RAID);
        }
    }
}
