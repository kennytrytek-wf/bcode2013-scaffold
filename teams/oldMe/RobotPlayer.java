package oldMe;

import battlecode.common.RobotController;
import battlecode.common.RobotType;

import oldMe.interfaces.Manager;
import oldMe.hq.HeadquartersManager;
import oldMe.robot.RobotManager;

public class RobotPlayer {
    public static HeadquartersManager hq = new HeadquartersManager();
    public static RobotManager rm = new RobotManager();

	public static void run(RobotController rc) {
        if (rc.getType() == RobotType.HQ) {
            RobotPlayer.move(hq, rc);
        } else if (rc.getType() == RobotType.SOLDIER) {
            RobotPlayer.move(rm, rc);
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
