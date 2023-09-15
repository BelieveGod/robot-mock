package cn.nannar.robotmock.fx.dto;

import lombok.Data;

import java.util.List;


/**
 * 与C++端 rdps 实时通讯过程中，拍照点的完成信号部分
 * @author LTJ
 * @date 2023/4/18
 */
@Data
public class RealTimePicPointDoneDTO {
    /**
     * 任务id
     */
    private Long taskId;
    /**
     * 任务来源 {@link cn.nuoli.monitor.modular.robot.constant.CmdSrcEnum}
     */
    private String cmdSrc;
    /**
     * 拍照点标识符
     */
    private String picPointId;
    /**
     * 车厢编号
     */
    private Integer carriage;
    /**
     *停车点基于车厢的位置百分比
     */
    private Integer agvPosPercent;
    /**
     * 故障信息集合
     */
    private List<ResultJsonSingleDO.ErrorInfo> partInfo;
    /**
     * 巡检任务的数据根路径
     */
    private String dir;
    /**
     * 是否完成整个任务
     */
    private Boolean finished;
}
