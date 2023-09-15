package cn.nannar.robotmock.fx.dto;

import lombok.Data;
import lombok.Getter;

import java.util.List;

/**
 * 创建任务的指令
 * @author LTJ
 * @date 2022/3/16
 */
@Data
public class CreateTaskCmd {
    public static final String CMD = "createTask";
    private String cmd=CMD;

    private Params params=new Params();

    @Data
   public static class Params{
        /**
         * 任务id
         */
       private Long taskId;
        /**
         * 股道编码
         */
       private Integer laneId;
        /**
         * 机器人编码
         */
       private Integer robotId;
        /**
         * 列车号
         */
       private String trainNo;
        /**
         * 方向端位
         */
       private Integer direction;
        /**
         * 厂家（车型）编码
         */
       private Integer mfrsCode;
        /**
         * 要检查的拍照点标识符数组
         */
        private List<String> picPointIdStrList;
   }


   public void setTaskID(Long taskId){
       params.setTaskId(taskId);
   }

   public void setLaneId(Integer laneId){
        params.setLaneId(laneId);
   }

   public void setRobotId(Integer robotId){
        params.setRobotId(robotId);
   }

   public void setTrainNo(String trainNo){params.setTrainNo(trainNo);}

   public void setDirection(Integer direction){params.setDirection(direction);}

   public void setMfrsCode(Integer mfrsCode){
       params.setMfrsCode(mfrsCode);
   }

   public void setPicPointIdStrList(List<String> picPointIdStrList){
       params.setPicPointIdStrList(picPointIdStrList);
   }
}
