package team004.common;

import battlecode.common.GameObject;

public class RobotState {
    final public static int MOVE_NEAR_HQ = 0;
    final public static int WAIT = 1;
    final public static int RAID = 2;
    final public static int ENEMY_PURSUIT = 3;

    public int action;
    public int robotID;
    public GameObject pursuing;

    public RobotState(int robotID) {
        this.robotID = robotID;
        this.action = RobotState.MOVE_NEAR_HQ;
    }
}
