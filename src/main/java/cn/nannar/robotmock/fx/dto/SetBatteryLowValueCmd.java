package cn.nannar.robotmock.fx.dto;

import lombok.Data;
import lombok.Getter;

/**
 * 设置低电量指令
 * @author LTJ
 * @date 2023/4/18
 */
@Getter
public class SetBatteryLowValueCmd {
    public static final String CMD = "setBatteryLowValue";
    private String cmd=CMD;

    private Params params=new Params();

    @Data
    private static class Params{
        /**
         * 机器人id
         */
        private Integer robotId;

        /**
         * 低电量阈值
         */
        private Integer lowValue;
    }

    public void setRobotId(Integer robotId){
        params.setRobotId(robotId);
    }

    public void setLowValue(Integer lowValue){
        params.setLowValue(lowValue);
    }
}
