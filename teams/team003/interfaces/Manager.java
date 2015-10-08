package team003.interfaces;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;

import team003.common.RobotState;

public abstract class Manager {
    public abstract void move(RobotController rc) throws GameActionException;
}
