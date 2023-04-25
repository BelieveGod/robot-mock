package cn.nannar.robotmock.fx.service;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.nannar.robotmock.fx.bo.*;
import lombok.extern.slf4j.Slf4j;
import org.dom4j.*;
import org.dom4j.io.SAXReader;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

/**
 * @author LTJ
 * @date 2023/4/23
 */
@Slf4j
public class MapService {
    public static MapBO parseMapXml(File mapSourceFile) throws DocumentException {
        String absolutePath = FileUtil.getAbsolutePath(mapSourceFile);
        log.info(absolutePath);
        SAXReader saxReader = new SAXReader();
        Document document = saxReader.read(mapSourceFile);
        Attribute resolution = ((Attribute) document.selectSingleNode("/MAP/header/@resolution"));
        Attribute diameter = (Attribute) document.selectSingleNode("/MAP/header/@reflector_diameter");
        Attribute minPosX = (Attribute) document.selectSingleNode("/MAP/header/min_pos/@x");
        Attribute minPosY = (Attribute) document.selectSingleNode("/MAP/header/min_pos/@y");
        Attribute maxPosx = (Attribute) document.selectSingleNode("/MAP/header/max_pos/@x");
        Attribute maxPosY = (Attribute) document.selectSingleNode("/MAP/header/max_pos/@y");
        String resolutionValue = resolution.getValue();
        String diameterValue = diameter.getValue();
        List<Node> pointList = document.selectNodes("/MAP/point");
        List<PointBO> pointBOList = new LinkedList<>();
        for (Node node : pointList) {
            PointBO pointBO = new PointBO();
            Element point = (Element) node;
            String x = point.attributeValue("x");
            String y = point.attributeValue("y");
            if(x==null || y==null){
                log.error("环境点缺数据");
                continue;
            }
            pointBO.setX(Convert.toDouble(x));
            pointBO.setY(Convert.toDouble(y));
            pointBOList.add(pointBO);
        }

        List<Node> nodes = document.selectNodes("/MAP/reflector");
        List<ReflectCylinderBO> reflectCylinderBOList = new LinkedList<>();
        for (Node node : nodes) {
            ReflectCylinderBO reflectCylinderBO = new ReflectCylinderBO();
            Element reflector = (Element) node;
            String id = reflector.attributeValue("id");
            if(StrUtil.isBlank(id)){
                log.error("反光住缺数据");
                continue;
            }
            String x = reflector.attributeValue("x");
            if(StrUtil.isBlank(x)){
                log.error("反光住缺数据");
                continue;
            }
            String y = reflector.attributeValue("y");
            if(StrUtil.isBlank(y)){
                log.error("反光住缺数据");
                continue;
            }
            reflectCylinderBO.setId(Convert.toInt(id));
            reflectCylinderBO.setX(Convert.toDouble(id));
            reflectCylinderBO.setY(Convert.toDouble(id));
            reflectCylinderBOList.add(reflectCylinderBO);
        }

        nodes = document.selectNodes("/MAP/advanced_point[@class_name='LandMark']");
        List<LandMarkBO> landMarkBOList = new LinkedList<>();
        for (Node node : nodes) {
            LandMarkBO landMarkBO = new LandMarkBO();

            Element landMark = (Element) node;
            String instanceName = landMark.attributeValue("instance_name");
            String id = landMark.attributeValue("id");
            Attribute x = ((Attribute) landMark.selectSingleNode("pos/@x"));
            String xValue = x.getValue();
            Attribute y = (Attribute) landMark.selectSingleNode("pos/@y");
            String yValue = y.getValue();
            if (StrUtil.isBlank(instanceName)) {
                log.error("路径点缺数据");
            }
            if (StrUtil.isBlank(id)) {
                log.error("路径点缺数据");
            }
            if (StrUtil.isBlank(xValue)) {
                log.error("路径点缺数据");
            }
            if (StrUtil.isBlank(yValue)) {
                log.error("路径点缺数据");
            }
            landMarkBO.setInstanceName(instanceName);
            landMarkBO.setId(Convert.toInt(id));
            landMarkBO.setX(Convert.toDouble(xValue));
            landMarkBO.setY(Convert.toDouble(yValue));
            landMarkBOList.add(landMarkBO);
        }

        nodes = document.selectNodes("/MAP/advanced_curve[@class_name='BezierPath']");
        List<PathBO> pathBOList = new LinkedList<>();
        for (Node node : nodes) {
            PathBO pathBO = new PathBO();
            Element path = (Element) node;
            Attribute id = path.attribute("id");
            Attribute roadWidth = path.attribute("RoadWidth");
            Attribute startPosId = (Attribute) path.selectSingleNode("start_pos/@id");
            Attribute endPosId = (Attribute) path.selectSingleNode("end_pos/@id");
            Attribute c1x = (Attribute) path.selectSingleNode("control1_pos/@x");
            Attribute c1y = (Attribute) path.selectSingleNode("control1_pos/@y");
            Attribute c2x = (Attribute) path.selectSingleNode("control2_pos/@x");
            Attribute c2y = (Attribute) path.selectSingleNode("control2_pos/@y");
            if(id==null){
                log.error("路径曲线缺数据");
            }
            if(roadWidth==null){
                log.error("路径曲线缺数据");
            }
            if(startPosId==null){
                log.error("路径曲线缺数据");
            }
            if(endPosId==null){
                log.error("路径曲线缺数据");
            }
            if(c1x==null){
                log.error("路径曲线缺数据");
            }
            if(c1y==null){
                log.error("路径曲线缺数据");
            }
            if(c2x==null){
                log.error("路径曲线缺数据");
            }
            if(c2y==null){
                log.error("路径曲线缺数据");
            }
            pathBO.setRoadWidth(Convert.toDouble(roadWidth.getValue()));
            pathBO.setControlX1(Convert.toDouble(c1x.getValue()));
            pathBO.setControlY1(Convert.toDouble(c1y.getValue()));
            pathBO.setControlX2(Convert.toDouble(c2x.getValue()));
            pathBO.setControlY2(Convert.toDouble(c2y.getValue()));
            LandMarkBO landMarkBO = landMarkBOList.stream().filter(e -> Objects.equals(e.getId(), Convert.toInt(startPosId.getValue())))
                    .findFirst().orElse(null);
            pathBO.setStartPos(landMarkBO);
            landMarkBO = landMarkBOList.stream().filter(e -> Objects.equals(e.getId(), Convert.toInt(endPosId.getValue())))
                    .findFirst().orElse(null);
            pathBO.setEndPos(landMarkBO);
            pathBOList.add(pathBO);
        }

        MapBO mapBO = new MapBO();
        mapBO.setResolution(Convert.toDouble(resolutionValue));
        mapBO.setReflectorDiameter(Convert.toDouble(diameterValue));
        mapBO.setMinPosX(Convert.toDouble(minPosX.getValue()));
        mapBO.setMinPosY(Convert.toDouble(minPosY.getValue()));
        mapBO.setMaxPosX(Convert.toDouble(maxPosx.getValue()));
        mapBO.setMaxPosY(Convert.toDouble(maxPosY.getValue()));
        mapBO.setPointBOList(pointBOList);
        mapBO.setReflectCylinderBOList(reflectCylinderBOList);
        mapBO.setLandMarkBOList(landMarkBOList);
        mapBO.setPathBOList(pathBOList);
        return mapBO;
    }
}
