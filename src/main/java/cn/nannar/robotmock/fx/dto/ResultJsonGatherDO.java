package cn.nannar.robotmock.fx.dto;

import cn.hutool.core.date.DatePattern;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * Web 后端与 C++ 对接的机器人巡检汇总数据文件的格式
 * result.json
 * @author LTJ
 * @date 2022/3/30
 */
@Data
public class ResultJsonGatherDO {
    /**
     * 任务发起来源
     */
    private String cmdSrc;
    /**
     * 股道id
     */
    private Integer laneId;
    /**
     * 拍照点的总数
     */
    private Integer photoPointsTotal;
    /**
     * 机器人的编码
     */
    private Integer robotId;
    /**
     * 任务的id (存储在sqlserver bot_inspect_log表的id)
     */
    private Long taskId;
    /**
     * 巡检开始时间
     */
    @JsonFormat(pattern = DatePattern.NORM_DATETIME_PATTERN)
    private Date traceTime;
    /**
     * 列车号
     */
    private String trainNo;
    /**
     * 拍照点详细数据
     */
    List<ResultJsonSingleDO> photoData;

}
