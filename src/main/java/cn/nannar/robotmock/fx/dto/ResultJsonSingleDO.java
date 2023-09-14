package cn.nannar.robotmock.fx.dto;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.util.Date;
import java.util.List;

/**
 * Web 后端与 C++ 对接的机器人巡检.单个拍照点的数据文件result.json的格式
 * @author LTJ
 * @date 2022/3/29
 */
@Data
public class ResultJsonSingleDO {
    /**
     * 所属于的任务id
     */
    private Long taskId;
    /**
     * 机器人巡检开始时间
     */
    private Date traceTime;

    /**
     * 任务命令来源
     */
    private String cmdSrc;

    /**
     * 巡检方向（端位）
     */
    private Integer direction;

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
     * 拍照点标识符
     */
    private String picPointId;
    /**
     * 告警数量
     */
    private Integer errorNum;
    /**
     * 暂时不知道该字段用处
     */
    private Integer result;

    /**
     * 位置点在车厢位置的百分比
     */
    private Integer agvPosPercent;
    /**
     * 停车点所在的车厢编码
     */
    private Integer carriage;
    /**
     * 故障数据点
     */
    private List<ErrorInfo> partInfo;



    @Data
    public static class ErrorInfo{
        /**
         * 暂时不知道用处
         */
        private Integer type;
        /**
         * 分析结果 0正常 1异常，但是出现在这里取值只能是1
         */
        private Integer result;
        /**
         * 异常描述
         */
        private String resultDesc;
        /**
         * 告警的标注框 x1,y1,x2,y2
         */
        @NotEmpty
        private List<Integer> picRect;
    }
}
