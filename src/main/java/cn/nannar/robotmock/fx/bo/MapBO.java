package cn.nannar.robotmock.fx.bo;

import lombok.Data;

import java.util.List;

/**
 * @author LTJ
 * @date 2023/4/23
 */
@Data
public class MapBO {
    private Double resolution;
    private Double minPosX;
    private Double minPosY;
    private Double maxPosX;
    private Double maxPosY;
    private Double reflectorDiameter;
    List<PointBO> pointBOList;
    List<ReflectCylinderBO> reflectCylinderBOList;
    List<LandMarkBO> landMarkBOList;
    List<PathBO> pathBOList;

}
