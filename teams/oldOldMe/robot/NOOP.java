package oldOldMe.robot;

import java.util.Random;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.Team;

import oldOldMe.interfaces.Manager;
import oldOldMe.common.RobotState;
import oldOldMe.common.MapInfo;

public class NOOP extends Manager {
    public RobotState createRobotState(RobotController rc) {
        return new RobotState(rc.getRobot().getID());
    }

    public void move(RobotController rc) throws GameActionException {
        if (rc.isActive()) {
        }
    }
}
