package cn.nannar.robotmock.fx.dto;

import lombok.Data;
import lombok.Getter;

/**
 * 回家的指令
 * @author LTJ
 * @date 2022/3/16
 */
@Data
public class GoHomeCmd {
    public static final String CMD = "goHome";
    private String cmd=CMD;

    private Params params=new Params();

    @Data
   public static class Params{


       private Integer robotId;

   }



   public void setRobotId(Integer robotId){
        params.setRobotId(robotId);
   }
}
