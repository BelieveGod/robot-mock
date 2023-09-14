package cn.nannar.robotmock.fx.constant;

import java.util.Arrays;

/**
 * 机器人执行任务的工作状态
 * @author LTJ
 * @date 2022/3/16
 */
public enum TaskStatus {
    WAITING(0,"任务等待中"),
    WORKING(1, "任务执行中"),
    SUSPENDING(2, "任务暂停中"),
    FINISHED(3, "任务已完成"),
    STOPPED(4, "任务已终止"),
    NOT_COMMIT(5, "未提交");


    private Integer code;
    private String message;

     TaskStatus(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    public Integer getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public static TaskStatus codeOf(Integer code){
        TaskStatus[] values = values();
        TaskStatus robotStatus = Arrays.stream(values).filter(e -> e.getCode().equals(code))
                .findFirst().orElse(null);
        return robotStatus;
    }

    public static TaskStatus messageOf(String message){
        TaskStatus[] values = values();
        TaskStatus robotStatus = Arrays.stream(values).filter(e -> e.getMessage().equals(message))
                .findFirst().orElse(null);
        return robotStatus;
    }
}
