package team031.robot;

import java.util.Random;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.Team;

import team031.interfaces.Manager;

public class NOOP extends Manager {

    public void move(RobotController rc) throws GameActionException {
        if (rc.isActive()) {
        }
    }
}
