package cn.nannar.robotmock.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * @author LTJ
 * @date 2022/3/14
 */
@Data
@TableName("bot_inspect_log")
public class BotInspectLog {
    /**
     * 巡检id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    /**
     * 巡检的机器人编码
     */
    @TableField("bot_code")
    private Integer botCode;
    /**
     * 巡检时间
     */
    @TableField("trace_time")
    private Date traceTime;
    /**
     * 停车股道编码
     */
    @TableField("parking_lane_code")
    private Integer parkingLaneCode;
    /**
     * 列车号
     */
    @TableField("train_no")
    private String trainNo;

    /**
     * 端位
     */
    @TableField("direction")
    private Integer direction;
    /**
     * 过车目录
     */
    @TableField("trace_file")
    private String traceFile;
    /**
     * 该记录的状态
     */
    @TableField("trance_status")
    private Integer tranceStatus;

    /**
     * task_seq_num 任务执行序号从0开始,数值越小任务最优先
     */
    @TableField("task_seq_num")
    private Integer taskSeqNum;
    /**
     * 任务状态
     */
    @TableField("task_status")
    private Integer taskStatus;
    /**
     * 完成时间
     */
    @TableField("compelete_time")
    private Date compeleteTime;

    @TableField("is_del")
    private Integer isDel;

    @TableField("create_time")
    private Date createTime;

    @TableField("update_time")
    private Date updateTime;

    @TableField("update_by")
    private String updateBy;

    @TableField("version")
    private Integer version;

    @TableField("cmd_src")
    private String cmdSrc;


}
