package cn.nannar.robotmock.fx.bo;

import lombok.Data;

/**
 * @author LTJ
 * @date 2023/4/23
 */
@Data
public class PathBO {
    private Double roadWidth;
    private LandMarkBO startPos;
    private LandMarkBO endPos;
    private Double controlX1;
    private Double controlY1;
    private Double controlX2;
    private Double controlY2;
}
