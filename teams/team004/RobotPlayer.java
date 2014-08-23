package team004;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

import team004.artillery.ArtilleryManager;
import team004.interfaces.Manager;
import team004.hq.HeadquartersManager;
import team004.robot.NOOP;
import team004.robot.RobotManager;

public class RobotPlayer {
	public static void run(RobotController rc) throws GameActionException {
	    RobotType type = rc.getType();
        if (type == RobotType.HQ) {
            RobotPlayer.move(new HeadquartersManager(rc), rc);
        } else if (type == RobotType.SOLDIER) {
            RobotPlayer.move(new RobotManager(rc), rc);
        } else if (type == RobotType.ARTILLERY) {
            RobotPlayer.move(new ArtilleryManager(), rc);
        } else {
            RobotPlayer.move(new NOOP(), rc);
        }
    }

    private static void move(Manager m, RobotController rc) {
        while(true) {
            try {
                m.move(rc);
            } catch (Exception e) {
                e.printStackTrace();
            }
            // End turn
            rc.yield();
        }
    }
}
