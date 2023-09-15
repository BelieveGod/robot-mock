package cn.nannar.robotmock.fx.dto;

import lombok.Data;
import lombok.Getter;

/**
 * 改变队列顺序的指令
 * @author LTJ
 * @date 2022/3/16
 */
@Data
public class ChangeQueuePosCmd {
    public static final String CMD = "changeQueuePos";
    private String cmd=CMD;

    private Params params=new Params();

    @Data
   public static class Params{
       private Long taskId;

       private Integer pos;

       private Integer robotId;

   }

   public void setTaskID(Long taskId){
       params.setTaskId(taskId);
   }

   public void setPos(Integer pos){
        params.setPos(pos);
   }

   public void setRobotId(Integer robotId){
        params.setRobotId(robotId);
   }
}
