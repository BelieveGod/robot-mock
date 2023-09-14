package cn.nannar.robotmock.fx.constant;

import java.util.Arrays;

/**
 * 机器人的状态枚举类
 * @author LTJ
 * @date 2022/3/16
 */
public enum RobotStatus {
    OFF_LINE(0,"离线"),
    IDLE(1,"空闲"),
    WAITING(2,"任务等待中"),
    WORKING(3, "任务执行中");


    private Integer code;
    private String message;

    RobotStatus(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    public Integer getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public static RobotStatus codeOf(Integer code){
        RobotStatus[] values = values();
        RobotStatus robotStatus = Arrays.stream(values).filter(e -> e.getCode().equals(code))
                .findFirst().orElse(null);
        return robotStatus;
    }

    public static RobotStatus messageOf(String message){
        RobotStatus[] values = values();
        RobotStatus robotStatus = Arrays.stream(values).filter(e -> e.getMessage().equals(message))
                .findFirst().orElse(null);
        return robotStatus;
    }
}
