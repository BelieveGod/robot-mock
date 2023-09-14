package cn.nannar.robotmock.fx.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;

/**
 * 机器人信息的快照存储 领域模型
 * @author LTJ
 * @date 2022/3/21
 */
@Data
public class RobotStorageDO {
    /**
     * 机器人编码
     */
    private Integer id;
    /**
     * 机器人名字
     */
    private String name;

    /**
     *  机器人状态 | 类型：int | 备注：0（离线）1（空闲）2（任务等待中）3（任务执行中） |  要和任务状态区别
     *   robotStatusEnum
     */
    private Integer status;
    /**
     * 机器人电量百分比
     */
    private Integer batteryPercent;
    /**
     * 机器人剩余时间
     */
    private Integer batteryRemainTime;
    /**
     * 是否在充电 false 没充电 true 充电
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
     * 机器人的任务队列
     */
    private List<Task> taskList=new LinkedList<>();


    /**
     *
     */
    private List<Task> webTaskList=new LinkedList<>();

    /**
     * 本地未传冲的队列
     */
    private List<Task> localTaskList = new LinkedList<>();
    /**
     * 机器人的实时坐标
     */
    private RealTimeDataDTO.Position2 position;
    /**
     * 机器人的故障码
     */
    private List<RealTimeDataDTO.TroubleCode> troubleCodeList;

    /**
     * AGV的数据
     */
    private RealTimeDataDTO.Agv agv;
    /**
     * 电池的数据
     */
    private RealTimeDataDTO.Battery battery;
    /**
     * 机械臂
     */
    private RealTimeDataDTO.Arm arm;
    /**
     * 3d采集相机的数据
     */
    private RealTimeDataDTO.CameraOf3d cameraOf3d;
    /**
     * 导航相机的数据
     */
    private RealTimeDataDTO.CameraOfNavigation cameraOfNavigation;



    @Data
    public static class Task extends RealTimeDataDTO.Task{
        /**
         * 股道名称
         */
        private String laneName;
        /**
         * 这次任务的拍照点轨迹,初始是空集合
         */
        List<PicturePointInfo> PicturePointInfo = new LinkedList<>();
//        /**
//         * 已经完成的拍照点数组下标，初始下标-1
//         */
//        private int donePicPointIdx=-1;

        /**
         * 上一个接收到的拍照点。记录这个为了快速判断推送过来的拍照点是否已经处理过，加快处理时间
         */
        private String  prePicPointId="";

        //        public final transient ReentrantReadWriteLock taskRwLock = new ReentrantReadWriteLock();
        private LocalDateTime statusUpdateTime = LocalDateTime.now();

        /**
         * web端发起的任务才有
         */
        private Integer inspectSchemeId;

        /**
         * web端发起的任务才有
         */
        private List<String> picPointIdList;

        private List<String> mapPointList;

    }

    @Data
    public static class PicturePointInfo{
        /**
         * 拍照点唯一标识符
         */
        private String picPointId;
        /**
         * 是否已经上传完成
         */
        private Boolean uploadFinished=false;
        /**
         * 已上传完毕的拍照点
         */
        @NotNull
        private Integer photoSeq;
        /**
         * 所属于的停车点编号
         */
        @NotNull
        private Integer parkPointSeq;
        /**
         * 所属车厢编码
         */
        @NotNull
        private Integer carriage;
        /**
         * 停车点基于车厢的位置百分比
         */
        @NotNull
        private Integer agvPosPercent;

        /**
         * 告警框信息集合
         */
        private List<cn.nuoli.monitor.modular.robot.vo.rdps.ResultJsonSingleDO.ErrorInfo> partInfo;
        /**
         * 算法标记的框框信息
         */


//        /**
//         * 相对文件夹目录
//         */
        private String dir;
//
//        /**
//         * 是否完成整个任务
//         */
//        private Boolean finished;
    }

    @Data
    public static class MarkInfo{
        /**
         * //告警图片红框坐标 对角线坐标
         */
        private List<Double> picRect;
        /**
         * 分析结果描述
         */
        private String resultDesc;
        /**
         * 拍照点结果 0（正常） 1（异常）
         */
        private Integer result;
    }
}
