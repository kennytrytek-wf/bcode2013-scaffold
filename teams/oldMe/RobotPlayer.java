package oldMe;

import battlecode.common.RobotController;
import battlecode.common.RobotType;

import oldMe.artillery.ArtilleryManager;
import oldMe.interfaces.Manager;
import oldMe.hq.HeadquartersManager;
import oldMe.robot.NOOP;
import oldMe.robot.RobotManager;

public class RobotPlayer {
	public static void run(RobotController rc) {
	    RobotType type = rc.getType();
        if (type == RobotType.HQ) {
            RobotPlayer.move(new HeadquartersManager(), rc);
        } else if (type == RobotType.SOLDIER) {
            RobotPlayer.move(new RobotManager(), rc);
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
