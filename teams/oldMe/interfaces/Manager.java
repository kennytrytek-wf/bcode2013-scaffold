package oldMe.interfaces;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;

import oldMe.common.RobotState;

public abstract class Manager {
    public abstract void move(RobotController rc) throws GameActionException;
    public abstract RobotState createRobotState(RobotController rc);
}
