package cn.nannar.robotmock.fx.dto;

import lombok.Data;

/**
 * 对接C++ ,应答成功与否的结构体
 * @author LTJ
 * @date 2022/3/17
 */
@Data
public class RdpsBoolAck {

    private String cmd;

    private Result result=new Result();

    @Data
    public static class Result{
        private Boolean success;
    }

}
