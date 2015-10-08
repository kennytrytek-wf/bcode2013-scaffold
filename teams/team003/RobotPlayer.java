package team003;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

import team003.artillery.ArtilleryManager;
import team003.interfaces.Manager;
import team003.hq.HeadquartersManager;
import team003.robot.MedBay;
import team003.robot.NOOP;
import team003.robot.RobotManager;

public class RobotPlayer {
	public static void run(RobotController rc) throws GameActionException {
	    RobotType type = rc.getType();
        if (type == RobotType.HQ) {
            RobotPlayer.move(new HeadquartersManager(rc), rc);
        } else if (type == RobotType.SOLDIER) {
            RobotPlayer.move(new RobotManager(rc), rc);
        } else if (type == RobotType.ARTILLERY) {
            RobotPlayer.move(new ArtilleryManager(), rc);
        } else if (type == RobotType.MEDBAY) {
            RobotPlayer.move(new MedBay(rc), rc);
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
