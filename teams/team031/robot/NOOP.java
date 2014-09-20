package team031.robot;

import java.util.Random;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.Team;

import team031.interfaces.Manager;
import team031.common.Radio;
import team031.common.RobotState;
import team031.common.MapInfo;

public class NOOP extends Manager {
    public RobotState createRobotState(RobotController rc) {
        return new RobotState(rc.getRobot().getID());
    }

    public void move(RobotController rc) throws GameActionException {
        Radio.updateData(rc, Radio.NUM_ENCAMPMENTS, 1);
        if (rc.isActive()) {
        }
    }
}
