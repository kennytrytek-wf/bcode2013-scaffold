package team031.common;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.Team;

public class Info {
    public static int round;
    public static double teamPower;
    public static MapLocation enemyHQLoc;
    public static MapLocation myHQLoc;
    public static Team myTeam;
    public static Team opponent;
    public static MapLocation gatherPoint;
    public static MapLocation strategicPoint;

    public Info(RobotController rc) throws GameActionException {
        this.initialize(rc);
    }

    private void initialize(RobotController rc) throws GameActionException {
        this.round = Clock.getRoundNum() - 1;
        this.teamPower = rc.getTeamPower();
        this.enemyHQLoc = rc.senseEnemyHQLocation();
        this.myHQLoc = rc.senseHQLocation();
        this.myTeam = rc.getTeam();
        this.opponent = this.myTeam.opponent();
        this.gatherPoint = this.calculateGatherPoint(rc);
        this.strategicPoint = null;
    }

    public void update(RobotController rc) {
        this.round += 1;
        this.teamPower = rc.getTeamPower();
        this.gatherPoint = this.calculateGatherPoint(rc);
    }

    public int distance(MapLocation start, MapLocation end) {
        int x1 = start.x;
        int x2 = end.x;
        int y1 = start.y;
        int y2 = end.y;
        return ((int) Math.sqrt((x1-x2)*(x1-x2) + (y1-y2)*(y1-y2)));
    }

    private int[] calcDirIncrements(Direction dir) {
        int xInc = 0;
        int yInc = 0;
        switch (dir) {
            case EAST: xInc = 1; yInc = 0; break;
            case SOUTH_EAST: xInc = 1; yInc = 1; break;
            case SOUTH: xInc = 0; yInc = 1; break;
            case SOUTH_WEST: xInc = -1; yInc = 1; break;
            case WEST: xInc = -1; yInc = 0; break;
            case NORTH_WEST: xInc = -1; yInc = -1; break;
            case NORTH: xInc = 0; yInc = -1; break;
            case NORTH_EAST: xInc = 1; yInc = -1; break;
        }
        return new int[]{xInc, yInc};
    }

    public MapLocation locationInDir(MapLocation start, Direction dir, int distance, int offset) {
        int[] incrs = this.calcDirIncrements(dir);
        MapLocation distLoc = new MapLocation(start.x + incrs[0] * distance, start.y + incrs[1] * distance);
        if (offset == 0) {
            return distLoc;
        } else if (offset > 0) {
            incrs = this.calcDirIncrements(dir.rotateRight().rotateRight());
        } else {
            incrs = this.calcDirIncrements(dir.rotateLeft().rotateLeft());
        }
        return new MapLocation(distLoc.x + incrs[0] * offset, distLoc.y + incrs[1] * offset);
    }

    private int calcIncr(double incr) {
        int multiplier = this.round / 100;
        if (multiplier == 0) {
            multiplier = 1;
        }
        return (int)(incr * multiplier);
    }

    private MapLocation calculateGatherPoint(RobotController rc) {
        if (this.strategicPoint != null) {
            return this.strategicPoint;
        }
        Direction enemyDir = this.myHQLoc.directionTo(this.enemyHQLoc);
        int enemyDistance = this.distance(this.myHQLoc, this.enemyHQLoc);
        return this.locationInDir(this.myHQLoc, enemyDir, (int) (enemyDistance * 0.25), 0);
    }
}
