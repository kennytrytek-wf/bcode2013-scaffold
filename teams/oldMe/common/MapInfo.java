package oldMe.common;

import battlecode.common.RobotController;

public class MapInfo {
    final public static int BIG = 0;
    final public static int MEDIUM = 1;
    final public static int SMALL = 2;

    public int mapSize;

    public MapInfo(RobotController rc) {
        int area = rc.getMapHeight() * rc.getMapWidth();
        if (area < 1000) {
            this.mapSize = MapInfo.SMALL;
        } else if (area < 5000) {
            this.mapSize = MapInfo.MEDIUM;
        } else {
            this.mapSize = MapInfo.BIG;
        }
    }
}
