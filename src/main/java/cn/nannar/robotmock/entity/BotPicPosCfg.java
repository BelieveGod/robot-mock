package cn.nannar.robotmock.entity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;
import java.util.regex.Pattern;

/**
 * @author LTJ
 * @date 2022/3/18
 */
@Data
@TableName("bot_pic_pos_cfg")
public class BotPicPosCfg {

    public static final Pattern PIC_POINT_ID_PATTERN = Pattern.compile("^(?<mfrsCode>\\d+)-c\\d+s\\d+_\\d+$");

    @TableId(value = "id")
    private String id;

    /**
     * 车型
     */
    @TableField("mfrs_code")
    private Integer mfrsCode;
    /**
     * 车厢编码
     */
    @TableField("carriage_code")
    private Integer carriageCode;
    /**
     * 车轴编码
     */
    @TableField("axle_code")
    private Integer axleCode;

    /**
     * 拍照点序号
     */
    @TableField("pic_seq")
    private Integer picSeq;
    /**
     * 拍照点名称
     */
    @TableField("pic_seq_name")
    private String picSeqName;
    /**
     * X 偏移量
     */
    @TableField("x_offset")
    private Double xOffset;
    /**
     * y偏移量
     */
    @TableField("y_offset")
    private Double yOffset;
    /**
     * 停车点编号
     */
    @TableField("agv_parking_seq")
    private Integer agvParkingSeq;

    @TableField("agv_pos_percent")
    private Integer agvPosPercent;
    /**
     * 检测项
     */
    @TableField("point_type_id")
    private Integer pointTypeId;
    /**
     * 检测类型
     */
    @TableField("device_type_id")
    private Integer deviceTypeId;
    /**
     * 启用状态 0（禁用）1（启用）
     */
    @TableField("status")
    private Integer status;

    /**
     * 创建时间
     */
    @TableField("creat_time")
    private Date createTime;

    /**
     * 扫描部件的分类编码 参考sys_dict中的scan_component_class
     */
    @TableField("scan_component_class")
    private Integer scanComponentClass;
}
