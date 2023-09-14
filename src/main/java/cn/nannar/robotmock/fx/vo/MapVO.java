package cn.nannar.robotmock.fx.vo;

import lombok.Data;

import java.util.LinkedList;
import java.util.List;

/**
 * @author LTJ
 * @date 2023/7/27
 */
@Data
public class MapVO {

    private Integer width;
    private Integer height;

    List<PointVO> pointVOList = new LinkedList<>();
    List<PointVO> landMarkVOList = new LinkedList<>();
    List<PathVO> pathVOList = new LinkedList<>();
    @Data
    public static class PointVO{
        private Double x;
        private Double y;
    }

    @Data
    public static class PathVO{
        public PointVO startPoint;
        public PointVO endPoint;
        private Double controlX1;
        private Double controlY1;
        private Double controlX2;
        private Double controlY2;
    }
}
