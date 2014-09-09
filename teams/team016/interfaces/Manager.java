package team016.interfaces;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;

import team016.common.RobotState;

public abstract class Manager {
    public abstract void move(RobotController rc) throws GameActionException;
}
