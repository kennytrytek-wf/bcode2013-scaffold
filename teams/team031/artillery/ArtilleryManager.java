package team031.artillery;

import java.util.Random;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameObject;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.Team;

import team031.interfaces.Manager;
import team031.common.Radio;
import team031.common.RobotState;
import team031.common.MapInfo;

public class ArtilleryManager extends Manager {
    public RobotState createRobotState(RobotController rc) {
        return new RobotState(rc.getRobot().getID());
    }

    public void move(RobotController rc) throws GameActionException {
        Radio.updateData(rc, Radio.NUM_ENCAMPMENTS, 1);
        if (rc.isActive()) {
            MapLocation enemyLoc = Radio.readLocation(rc, Radio.ENEMY);
            if ((enemyLoc != null) && rc.canAttackSquare(enemyLoc)) {
                rc.attackSquare(enemyLoc);
                return;
            }
            MapLocation loc = rc.getLocation();
            Team myTeam = rc.getTeam();
            GameObject[] go = rc.senseNearbyGameObjects(Robot.class, loc, 33 * 33, myTeam.opponent());
            for (int i=0; i < go.length; i++) {
                MapLocation attackLoc = rc.senseLocationOf(go[i]);
                if (rc.canAttackSquare(attackLoc)) {
                    rc.attackSquare(attackLoc);
                    break;
                }
            }
        }
    }
}
