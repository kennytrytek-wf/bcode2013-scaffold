package swarm;

import battlecode.common.RobotController;
import battlecode.common.RobotType;

import swarm.artillery.ArtilleryManager;
import swarm.interfaces.Manager;
import swarm.hq.HeadquartersManager;
import swarm.robot.NOOP;
import swarm.robot.RobotManager;

public class RobotPlayer {
	public static void run(RobotController rc) {
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
