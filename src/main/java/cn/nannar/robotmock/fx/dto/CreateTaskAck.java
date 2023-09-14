package cn.nannar.robotmock.fx.dto;

import lombok.Data;

import java.util.List;

/**
 * @author LTJ
 * @date 2023/9/1
 */
@Data
public class CreateTaskAck {
    private String cmd;
    private Result result;
    @Data
    public static class Result{
        private Boolean success;
        /**
         * 执行任务要走的地图landmark点
         */
        private List<String> mapPointList;
    }
}
