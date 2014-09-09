package team016.common;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class Radio {
    public static final int ENEMY = 0;
    public static final int CANARY = 1;
    public static final int OUTPOST = 2;

    private static int getBroadcastChannel(int purpose) {
        switch(purpose) {
            case Radio.ENEMY: return 58293;
            case Radio.CANARY: return 57487;
            case Radio.OUTPOST: return 25998;
            default: return 58293;
        }
    }

    public static boolean readStatus(RobotController rc, int channel) throws GameActionException {
        int status = rc.readBroadcast(Radio.getBroadcastChannel(channel));
        return status == 1;
    }

    public static void writeStatus(RobotController rc, int channel, boolean status) throws GameActionException {
        int message = status ? 1 : 0;
        rc.broadcast(Radio.getBroadcastChannel(channel), message);
    }

    public static void writeLocation(RobotController rc, int channel, MapLocation loc) throws GameActionException {
        int x = ((loc.x << 8) & 0xFF00);
        int y = loc.y & 0x00FF;
        int encodedLocation = x | y;
        rc.broadcast(Radio.getBroadcastChannel(channel), encodedLocation);
    }

    public static MapLocation readLocation(RobotController rc, int channel) throws GameActionException {
        int message = rc.readBroadcast(Radio.getBroadcastChannel(channel));
        if (message == 0) {
            return null;
        }
        int x = (message & 0xFF00) >> 8;
        int y = message & 0x00FF;
        return new MapLocation(x, y);
    }
}
