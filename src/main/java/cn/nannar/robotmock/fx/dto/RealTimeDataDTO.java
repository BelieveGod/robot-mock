package cn.nannar.robotmock.fx.dto;

import cn.nannar.robotmock.fx.tasksatus.TaskState;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * Rdps 实时推送的机器人相关信息 领域模型
 * @author LTJ
 * @date 2022/3/15
 */
@Data
public class RealTimeDataDTO {
    /**
     * 机器人的信息
     */
    private List< RobotInfo> robotInfos;
    /**
     * 升降台的信息
     */
    private List<LiftInfo> liftInfos;
    /**
     * 充电桩的信息
     */
    private List<ChargingPileInfo> chargingPileInfos;

    /**
     * 机器人的信息
     */
    @Data
    public static class RobotInfo{
        /**
         * 机器人编码
         */
        private Integer id;
        /**
         * 机器人的状态 参见枚举类
         * 0-离线 1-空闲 2-任务等待中 3-任务执行中
         */
        private Integer status;
        /**
         * 电量百分比
         */
        private Integer batteryPercent;
        /**
         * 电量剩余时间
         */
        private Integer batteryRemainTime;
        /**
         * 是否充电中
         */
        private Boolean charging;
        /**
         *  充电的电流大小 ，单位 A
         */
        private Double chargingCurrent;
        /**
         * 电压 单位V
         */
        private Double agvVoltage;
        /**
         * 预计充满时间 单位分钟
         */
        private Integer timeToFull;
        /**
         * 当前任务
         */
        private Task task;
        /**
         * 任务队列
         */
        private List<BriefTask> taskQueue;

        /**
         * 机器人坐标
         */
        private Position2 position;
        /**
         * 机器人的故障列表，可以多个故障同时上报
         */
        private List<TroubleCode> troubleCodeList;
        /**
         * AGV的数据
         */
        private Agv agv;
        /**
         * 电池的数据
         */
        private Battery battery;
        /**
         * 机械臂
         */
        private Arm arm;
        /**
         * 3d采集相机的数据
         */
        private CameraOf3d cameraOf3d;
        /**
         * 导航相机的数据
         */
        private CameraOfNavigation cameraOfNavigation;
    }

    /**
     * 升降台信息
     */
    @Data
    public static class LiftInfo{
        /**
         * 升降台id
         */
        private Integer id;
        /**
         * 升降台名字
         */
        private String name;
        /**
         * 升降台状态
         * healthStatusEnum
         */
        private Integer status;
        /**
         * 升降台的运动状态 1(静止) 2(上升) 3(下降)
         */
        private Integer moveStatus;
        /**
         * 故障编码
         */
        private List<TroubleCode> troubleCodeList;
    }

    /**
     * 充电桩信息
     */
    @Data
    public static class ChargingPileInfo{
        /**
         * 充电桩Id
         */
        private Integer id;
        /**
         * 充电桩名称
         */
        private String name;
        /**
         * 充电桩状态
         * healthStatusEnum
         */
        private Integer status;

        /**
         * 故障编码
         */
        private List<TroubleCode> troubleCodeList;
    }

    /**
     * 当前任务信息
     */
    @Data
    public static class Task{
        /**
         * 任务id
         */
        private Long id;
        /**
         * 股道编码
         */
        private Integer laneId;
        /**
         * 任务命令来源 |  string | 取值：rdps 或者 web
         */
        private String cmdSrc;
        /**
         * 当前检测的列车号
         */
        private String trainNo;

        /**
         * 端位 0（1端） 1（2端）
         * TrainEndPoint
         */
        private Integer direction;
        /**
         * 当前任务的位置
         */
        private Position position;
        /**
         * 当前任务百分比
         */
        private Integer percent;
        /**
         * 任务累计执行时间
         */
        private Integer accumulatedTime;

        /**
         * 任务状态 @see
         * 0（任务等待中） 1(任务执行中) 2（任务暂停） 3（任务完成） 4（任务终止） 5(未提交）
         */
        private Integer status;

        @JsonIgnore
        private Date taskStartTime;

        @JsonIgnore
        private List<String> picPointIdStr;
        @JsonIgnore
        private int curPicPointIdx;
        @JsonIgnore
        private List<String> landMarkList;
        @JsonIgnore
        private int curLandMarkIdx=0;
        @JsonIgnore
        private TaskState taskState;
        @JsonIgnore
        private String traceFile;
        public void doAction(){
            taskState.doAction();
        }
    }

    /**
     * 简略任务
     */
    @Data
    public static class BriefTask{
        /**
         * 任务id
         */
        private Long taskId;
        /**
         * 股道编号
         */
        private Integer laneId;
        /**
         * 任务命令来源 |  string | 取值：rdps 或者 web
         */
        private String cmdSrc;
    }

    @Data
    public static class Position2{
        /**
         * x轴坐标
         */
        private Double x;
        /**
         * y轴坐标
         */
        private Double y;
        /**
         * 角度
         */
        private Double angle;
    }

    /**
     * 位置信息
     */
    @Data
    public static class Position{

        /**
         * 拍照点唯一标识符,如果还没有在哪一个点，标识符传空字符串“”即可
         */
        private String picPointId;
        /**
         * 车厢编码
         */
        private Integer carriage;
        /**
         *  车厢位置百分比
         */
        private Integer agvPosPercent;
        /**
         * 任务路径中，上一个经过的基于机器人路径点的数组下标 | int |
         */
        private Integer prePassPointIdx;
    }

    /**
     * 健康信息
     */
    @Data
    public static class HealthInfo{
        /**
         * 对象名称
         */
        private String objName;
        /**
         * 健康状态
         */
        private Integer status;
        /**
         * 健康信息描述
         */
        private String desc;

        /**
         * 机器人故障编码
         */
        private String troubleCode;
    }

    @Data
    public static class TroubleCode{
        private String desc;
        private String troubleCode;
    }

    @Data
    public static class Agv {
        /**
         *  设备状态 | 类型： int | 备注：0（正常）1（异常）2（离线）
         */
        private Integer status;
        /**
         * 该对象的故障列表，可以多个故障同时上报
         */
        private List<TroubleCode> troubleCodeList;
    }

    @Data
    public static class Battery{
        /**
         *  设备状态 | 类型： int | 备注：0（正常）1（异常）2（离线）
         */
        private Integer status;
        /**
         * 该对象的故障列表，可以多个故障同时上报
         */
        private List<TroubleCode> troubleCodeList;
    }

    @Data
    public static class Arm{
        /**
         *  设备状态 | 类型： int | 备注：0（正常）1（异常）2（离线）
         */
        private Integer status;
        /**
         * 机械臂运动状态 1(静止中) 2（运动中）
         */
        private Integer moveStatus;
        /**
         * 该对象的故障列表，可以多个故障同时上报
         */
        private List<TroubleCode> troubleCodeList;
    }

    @Data
    public static class CameraOf3d{
        /**
         *  设备状态 | 类型： int | 备注：0（正常）1（异常）2（离线）
         */
        private Integer status;
        /**
         * 该对象的故障列表，可以多个故障同时上报
         */
        private List<TroubleCode> troubleCodeList;
    }

    @Data
    public static class CameraOfNavigation{
        /**
         *  设备状态 | 类型： int | 备注：0（正常）1（异常）2（离线）
         */
        private Integer status;
        /**
         * 该对象的故障列表，可以多个故障同时上报
         */
        private List<TroubleCode> troubleCodeList;
    }
}
