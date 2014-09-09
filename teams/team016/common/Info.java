package team016.common;

import battlecode.common.Clock;
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

    public MapLocation locationBetween(MapLocation start, MapLocation end, double percentDistance, RobotController rc) {
        double rawX = (end.x - start.x) * percentDistance;
        double rawY = (end.y - start.y) * percentDistance;
        double incrX = Math.copySign(Math.sqrt(Math.abs(rawX)), rawX);
        double incrY = Math.copySign(Math.sqrt(Math.abs(rawY)), rawY);
        int endX = start.x + this.calcIncr(incrX);
        int endY = start.y + this.calcIncr(incrY);
        MapLocation point = new MapLocation(endX, endY);
        rc.setIndicatorString(2, "gatherPoint: " + point + ", incrX: " + this.calcIncr(incrX) + ", incrY: " + this.calcIncr(incrY) + ", %: " + percentDistance);
        return point;
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
        return this.locationBetween(this.myHQLoc, this.enemyHQLoc, 0.75, rc);
    }
}
