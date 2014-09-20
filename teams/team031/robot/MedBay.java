package team031.robot;

import java.util.Random;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameObject;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.Team;

import team031.interfaces.Manager;
import team031.common.Info;
import team031.common.Radio;
import team031.common.RobotState;

public class MedBay extends Manager {
    Info info;

    private void initialize(RobotController rc) throws GameActionException {
        this.info = new Info(rc);
    }

    public MedBay(RobotController rc) throws GameActionException {
        this.initialize(rc);
    }

    public RobotState createRobotState(RobotController rc) {
        return new RobotState(rc.getRobot().getID());
    }

    public void move(RobotController rc) throws GameActionException {
        Radio.updateData(rc, Radio.NUM_ENCAMPMENTS, 1);
        MapLocation myLoc = rc.getLocation();
        if (this.info.distance(myLoc, this.info.myHQLoc) <= 5) {
            Radio.writeLocation(rc, Radio.MEDBAY, myLoc);
        }
    }
}
