package swarm.artillery;

import java.util.Random;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameObject;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.Team;

import swarm.interfaces.Manager;
import swarm.common.RobotState;
import swarm.common.MapInfo;

public class ArtilleryManager extends Manager {
    public RobotState createRobotState(RobotController rc) {
        return new RobotState(rc.getRobot().getID());
    }

    public void move(RobotController rc) throws GameActionException {
        if (rc.isActive()) {
            MapLocation loc = rc.getLocation();
            Team myTeam = rc.getTeam();
            GameObject[] go = rc.senseNearbyGameObjects(Robot.class, loc, 100, myTeam.opponent());
            if (go.length > 0) {
                rc.attackSquare(rc.senseLocationOf(go[0]));
            }
        }
    }
}
