package team004.robot;

import java.util.Random;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.Team;

import team004.interfaces.Manager;
import team004.common.RobotState;
import team004.common.MapInfo;

public class NOOP extends Manager {
    public RobotState createRobotState(RobotController rc) {
        return new RobotState(rc.getRobot().getID());
    }

    public void move(RobotController rc) throws GameActionException {
        if (rc.isActive()) {
        }
    }
}