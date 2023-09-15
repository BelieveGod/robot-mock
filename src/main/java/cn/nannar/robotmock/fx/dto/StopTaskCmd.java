package cn.nannar.robotmock.fx.dto;

import lombok.Data;
import lombok.Getter;

/**
 * 停止任务指令
 * @author LTJ
 * @date 2022/3/16
 */
@Data
public class StopTaskCmd {
    public static final String CMD = "stopTask";
    private String cmd=CMD;

    private Params params=new Params();

    @Data
   public static class Params{
       private Long taskId;



   }

   public void setTaskID(Long taskId){
       params.setTaskId(taskId);
   }

}
