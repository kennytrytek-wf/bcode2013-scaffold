package team031.interfaces;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;

import team031.common.RobotState;

public abstract class Manager {
    public abstract void move(RobotController rc) throws GameActionException;
}
