package nukebot;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotType;
import battlecode.common.Upgrade;

public class RobotPlayer {
	public static void run(RobotController rc) throws GameActionException {
	    RobotType type = rc.getType();
        if (type == RobotType.HQ) {
            while(true) {
                rc.researchUpgrade(Upgrade.NUKE);
                rc.yield();
            }
        }
    }
}
