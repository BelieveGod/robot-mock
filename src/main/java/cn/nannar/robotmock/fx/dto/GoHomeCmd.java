package cn.nannar.robotmock.fx.dto;

import lombok.Data;
import lombok.Getter;

/**
 * 回家的指令
 * @author LTJ
 * @date 2022/3/16
 */
@Getter
public class GoHomeCmd {
    public static final String CMD = "goHome";
    private String cmd=CMD;

    private Params params=new Params();

    @Data
   private static class Params{


       private Integer robotId;

   }



   public void setRobotId(Integer robotId){
        params.setRobotId(robotId);
   }
}
