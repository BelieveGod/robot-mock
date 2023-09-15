package cn.nannar.robotmock.fx.bo;

import lombok.Data;

/**
 * @author LTJ
 * @date 2023/9/12
 */
@Data
public class PicPosRelPointBO {
    private String beginPointName;
    private String endPointName;
    private Integer segmentNum;
    private double x;
    private double y;
    private double endX;
    private double endY;
    private String picPointId;
}
