package team031.interfaces;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;

public abstract class Manager {
    public abstract void move(RobotController rc) throws GameActionException;
}
