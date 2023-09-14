package cn.nannar.robotmock.fx.dto;

import lombok.Data;
import lombok.Getter;

/**
 * 恢复任务指令
 * @author LTJ
 * @date 2022/3/16
 */
@Getter
public class ResumeTaskCmd {
    public static final String CMD = "resumeTask";
    private String cmd=CMD;

    private Params params=new Params();

    @Data
   private static class Params{
       private Long taskId;



   }

   public void setTaskID(Long taskId){
       params.setTaskId(taskId);
   }

}
