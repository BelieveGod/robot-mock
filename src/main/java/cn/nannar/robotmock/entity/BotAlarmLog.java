package cn.nannar.robotmock.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 机器人告警的实体类
 * @author LTJ
 * @date 2022/3/14
 */
@Data
@TableName("bot_alarm_log")
public class BotAlarmLog {
    /**
     * 告警id
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
     * 巡检的列车号
     */
    @TableField("train_no")
    private String trainNo;
    /**
     * 巡检的任务记录id
     */
    @TableField("bot_inspect_id")
    private Long botInspectId;
    /**
     * 车厢编码
     */
    @TableField("carriage_code")
    private Integer carriageCode;

    /**
     * 告警的拍照点序号
     */
    @TableField("pic_point_id")
    private String picPointId;

//    @TableField("bot_code")
//    private Integer partCode;
    /**
     * 检测项
     */
    @TableField("check_id")
    private Integer checkId;
    /**
     * 检测类型
     */
    @TableField("device_type_id")
    private Integer deviceTypeId;
    /**
     * 告警值
     */
    @TableField("alarm_value")
    private BigDecimal alarmValue;
    /**
     * da告警值
     */
    @TableField("da_alarm_value")
    private BigDecimal daAlarmValue;
    /**
     * 告警标记位
     */
    @TableField("flag_alarm")
    private Integer flagAlarm;
    /**
     * 告警处理状态 0（待确认）
     */
    @TableField("alarm_status")
    private Integer alarmStatus;

    @TableField("sound_alarm_status")
    private Integer soundAlarmStatus;
    /**
     * 告警等级
     */
    @TableField("alarm_level")
    private Integer alarmLevel;
    /**
     * da告警等级
     */
    @TableField("da_alarm_level")
    private Integer daAlarmLevel;
    /**
     * 告警描述
     */
    @TableField("description")
    private String description;
    /**
     * 创建时间
     */
    @TableField("create_time")
    private Date createTime;
    /**
     * 告警图片的标记框
     */
    @TableField("alarm_img_xy")
    private String alarmImgXy;
    /**
     * 标记框的类型
     * MarkTypeEnum
     */
    @TableField("alarm_mark")
    private Integer alarmMark;
    /**
     * 数据版本
     */
    @TableField("version")
    private Integer version;
    /**
     * 确认人名称
     */
    @TableField("confirm_name")
    private String confirmName;
    /**
     * 备注
     */
    @TableField("remark")
    private String remark;

    /**
     * 某个拍照点下的告警所在告警数组的下标，从0开始
     */
    @TableField("arr_idx")
    private Integer arrIdx;
}
