package swarm.interfaces;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;

import swarm.common.RobotState;

public abstract class Manager {
    public abstract void move(RobotController rc) throws GameActionException;
}
