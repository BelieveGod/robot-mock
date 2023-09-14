package cn.nannar.robotmock.fx.dto;

import lombok.Data;
import lombok.Getter;

/**
 * 暂停机器人指令
 * @author LTJ
 * @date 2022/3/16
 */
@Getter
public class ResumeRobotCmd {
    public static final String CMD = "resumeRobot";
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
