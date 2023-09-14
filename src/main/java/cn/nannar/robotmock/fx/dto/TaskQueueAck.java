package cn.nannar.robotmock.fx.dto;

import lombok.Data;

import java.util.List;

/**
 * 任务队列应答的 领域模型
 * @author LTJ
 * @date 2022/3/17
 */
@Data
public class TaskQueueAck {

    private String cmd;

    private Result result=new Result();

    @Data
    public static class Result{
        private List<Task> taskQueue;
    }

    @Data
    public static class Task{
        private Long taskId;

        private Integer laneId;
    }

}
