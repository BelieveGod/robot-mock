package cn.nannar.robotmock.fx.service;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.StopWatch;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.nannar.robotmock.fx.bo.*;
import lombok.extern.slf4j.Slf4j;
import org.dom4j.*;
import org.dom4j.io.SAXReader;

import java.io.File;
import java.util.*;

/**
 * @author LTJ
 * @date 2023/4/23
 */
@Slf4j
public class MapService {
    public static MapBO parseMapXml(File mapSourceFile) throws DocumentException {
        HashSet<String> curveIdSet = new HashSet<>();
        HashMap<String, LandMarkBO> landMarkBOHashMap = new HashMap<>();
        StopWatch stopWatch = new StopWatch();
        String absolutePath = FileUtil.getAbsolutePath(mapSourceFile);
        log.info(absolutePath);
        SAXReader saxReader = new SAXReader();
        stopWatch.start("读取xml文件");
        Document document = saxReader.read(mapSourceFile);
        Element rootElement = document.getRootElement();
        stopWatch.stop();
        stopWatch.start("解析头部信息");
        Element header = rootElement.element("header");
        Attribute resolution = header.attribute("resolution");
        String resolutionValue = resolution.getValue();
        Attribute diameter = header.attribute("reflector_diameter");
        String diameterValue = diameter.getValue();
        Element min_pos = header.element("min_pos");
        Attribute minPosX = min_pos.attribute("x");
        Attribute minPosY = min_pos.attribute("y");
        Element max_pos = header.element("max_pos");
        Attribute maxPosx = max_pos.attribute("x");
        Attribute maxPosY = max_pos.attribute("y");
        stopWatch.stop();

        List<PointBO> pointBOList = new LinkedList<>();
        List<LandMarkBO> landMarkBOList = new LinkedList<>();
        List<PathBO> pathBOList = new LinkedList<>();
        int nodeCount = rootElement.nodeCount();
        stopWatch.start("fast loop 大循环");
        for(int i=0;i<nodeCount;i++){
            Node node = rootElement.node(i);
            if(! (node instanceof Element)){
                continue;
            }
            Element element = (Element) node;
            if("point".equals(element.getName())  ){
                Element point=element;
                String x = point.attributeValue("x");
                String y = point.attributeValue("y");
                if (x == null || y == null) {
                    log.error("环境点缺数据");
                    continue;
                }
                PointBO pointBO = new PointBO();
                pointBO.setX(Double.parseDouble(x));
                pointBO.setY(Double.parseDouble(y));
                pointBOList.add(pointBO);
            }else if("advanced_point".equals(element.getName())){
                Element landMark = (Element) node;
                if(!"LandMark".equals(landMark.attributeValue("class_name"))){
                    continue;
                }
                LandMarkBO landMarkBO = new LandMarkBO();
                String id = landMark.attributeValue("id");
                Element pos = landMark.element("pos");
                String xValue = pos.attributeValue("x");
                String yValue= pos.attributeValue("y");
                if (StrUtil.isBlank(id)) {
                    log.error("路径点缺数据");
                }
                if (StrUtil.isBlank(xValue)) {
                    log.error("路径点缺数据");
                }
                if (StrUtil.isBlank(yValue)) {
                    log.error("路径点缺数据");
                }
                landMarkBO.setId(id);
                landMarkBO.setX(Double.parseDouble(xValue));
                landMarkBO.setY(Double.parseDouble(yValue));
                landMarkBOHashMap.put(id, landMarkBO);
                landMarkBOList.add(landMarkBO);
            }else if("advanced_curve".equals(element.getName())){
                Element path = (Element) node;
                if(!"BezierPath".equals(path.attributeValue("class_name"))){
                    continue;
                }
                Attribute id = path.attribute("id");
                String curveId = id.getValue();
                if (curveIdSet.contains(curveId)) {
                    continue;
                }else{
                    curveIdSet.add(curveId);
                }
                Attribute roadWidth = path.attribute("RoadWidth");
                Element start_pos = path.element("start_pos");
                Element end_pos = path.element("end_pos");
                String startPosId = start_pos.attributeValue("id");
                String endPosId = end_pos.attributeValue("id");
                Element control1_pos = path.element("control1_pos");
                Attribute c1x=control1_pos.attribute("x");
                Attribute c1y = control1_pos.attribute("y");
                Element control2_pos = path.element("control2_pos");
                Attribute c2x = control2_pos.attribute("x");
                Attribute c2y =control2_pos.attribute("y");
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
                PathBO pathBO = new PathBO();
                pathBO.setRoadWidth(Double.parseDouble(roadWidth.getValue()));
                pathBO.setControlX1(Double.parseDouble(c1x.getValue()));
                pathBO.setControlY1(Double.parseDouble(c1y.getValue()));
                pathBO.setControlX2(Double.parseDouble(c2x.getValue()));
                pathBO.setControlY2(Double.parseDouble(c2y.getValue()));
                LandMarkBO landMarkBO = landMarkBOHashMap.get(startPosId);
                pathBO.setStartPos(landMarkBO);
                landMarkBO = landMarkBOHashMap.get(endPosId);
                pathBO.setEndPos(landMarkBO);
                pathBOList.add(pathBO);
            }
        }
        stopWatch.stop();

        stopWatch.start("封装到mapBo");
        MapBO mapBO = new MapBO();
        mapBO.setResolution(Double.parseDouble(resolutionValue));
        mapBO.setReflectorDiameter(Double.parseDouble(diameterValue));
        mapBO.setMinPosX(Double.parseDouble(minPosX.getValue()));
        mapBO.setMinPosY(Double.parseDouble(minPosY.getValue()));
        mapBO.setMaxPosX(Double.parseDouble(maxPosx.getValue()));
        mapBO.setMaxPosY(Double.parseDouble(maxPosY.getValue()));
        mapBO.setPointBOList(pointBOList);
        mapBO.setReflectCylinderBOList(new LinkedList<>());
        mapBO.setLandMarkBOList(landMarkBOList);
        mapBO.setPathBOList(pathBOList);
        stopWatch.stop();
        StopWatch.TaskInfo[] taskInfoArr = stopWatch.getTaskInfo();
        for (StopWatch.TaskInfo taskInfo : taskInfoArr) {
            log.info("{}:{}ms",taskInfo.getTaskName(),taskInfo.getTimeMillis());
        }
        log.info("parseMapXml 总耗时{}ms",stopWatch.getTotalTimeMillis());
        return mapBO;
    }

    public static MapBO parseMapXml2(File mapSourceFile) throws DocumentException {
        HashSet<String> curveIdSet = new HashSet<>();
        HashMap<String, LandMarkBO> landMarkBOHashMap = new HashMap<>();
        StopWatch stopWatch = new StopWatch();
        String absolutePath = FileUtil.getAbsolutePath(mapSourceFile);
        log.info(absolutePath);
        SAXReader saxReader = new SAXReader();
        stopWatch.start("读取xml文件");
        Document document = saxReader.read(mapSourceFile);
        Element rootElement = document.getRootElement();
        stopWatch.stop();
        stopWatch.start("解析头部信息");
        Element header = rootElement.element("header");
        Attribute resolution = header.attribute("resolution");
        String resolutionValue = resolution.getValue();
        Attribute diameter = header.attribute("reflector_diameter");
        String diameterValue = diameter.getValue();
        Element min_pos = header.element("min_pos");
        Attribute minPosX = min_pos.attribute("x");
        Attribute minPosY = min_pos.attribute("y");
        Element max_pos = header.element("max_pos");
        Attribute maxPosx = max_pos.attribute("x");
        Attribute maxPosY = max_pos.attribute("y");
        stopWatch.stop();

        List<PointBO> pointBOList = new LinkedList<>();
        List<LandMarkBO> landMarkBOList = new LinkedList<>();
        List<PathBO> pathBOList = new LinkedList<>();

        stopWatch.start("筛选point元素element");
        List<Element> pointList = rootElement.elements("point");
        stopWatch.stop();
        stopWatch.start("循环解析<point>");
        for (Node node : pointList) {
            PointBO pointBO = new PointBO();
            Element point = (Element) node;
            String x = point.attributeValue("x");
            String y = point.attributeValue("y");
            if(x==null || y==null){
                log.error("环境点缺数据");
                continue;
            }
            pointBO.setX(Double.parseDouble(x));
            pointBO.setY(Double.parseDouble(y));
            pointBOList.add(pointBO);
        }
        stopWatch.stop();

        stopWatch.start("筛选<advanced_point>");
        pointList = rootElement.elements("advanced_point");
        stopWatch.stop();
        stopWatch.start("循环处理advanced_point");
        for (Node node : pointList) {
            Element landMark = (Element) node;
            if(!"LandMark".equals(landMark.attributeValue("class_name"))){
                continue;
            }
            LandMarkBO landMarkBO = new LandMarkBO();
            String id = landMark.attributeValue("id");
            Element pos = landMark.element("pos");
            String xValue = pos.attributeValue("x");
            String yValue= pos.attributeValue("y");
            if (StrUtil.isBlank(id)) {
                log.error("路径点缺数据");
            }
            if (StrUtil.isBlank(xValue)) {
                log.error("路径点缺数据");
            }
            if (StrUtil.isBlank(yValue)) {
                log.error("路径点缺数据");
            }
            landMarkBO.setId(id);
            landMarkBO.setX(Double.parseDouble(xValue));
            landMarkBO.setY(Double.parseDouble(yValue));
            landMarkBOHashMap.put(id, landMarkBO);
            landMarkBOList.add(landMarkBO);
        }
        stopWatch.stop();

        stopWatch.start("选取<advanced_curve>列表");
        pointList = rootElement.elements("advanced_curve");
        stopWatch.stop();
        stopWatch.start("循环advanced_curve");
        for (Node node : pointList) {
            Element path = (Element) node;
            if(!"BezierPath".equals(path.attributeValue("class_name"))){
                continue;
            }
            Attribute id = path.attribute("id");
            String curveId = id.getValue();
            if (curveIdSet.contains(curveId)) {
                continue;
            }else{
                curveIdSet.add(curveId);
            }
            Attribute roadWidth = path.attribute("RoadWidth");
            Element start_pos = path.element("start_pos");
            Element end_pos = path.element("end_pos");
            String startPosId = start_pos.attributeValue("id");
            String endPosId = end_pos.attributeValue("id");
            int sid = Integer.parseInt(startPosId);
            int eid = Integer.parseInt(endPosId);
            Element control1_pos = path.element("control1_pos");
            Attribute c1x=control1_pos.attribute("x");
            Attribute c1y = control1_pos.attribute("y");
            Element control2_pos = path.element("control2_pos");
            Attribute c2x = control2_pos.attribute("x");
            Attribute c2y =control2_pos.attribute("y");
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
            PathBO pathBO = new PathBO();
            pathBO.setRoadWidth(Double.parseDouble(roadWidth.getValue()));
            pathBO.setControlX1(Double.parseDouble(c1x.getValue()));
            pathBO.setControlY1(Double.parseDouble(c1y.getValue()));
            pathBO.setControlX2(Double.parseDouble(c2x.getValue()));
            pathBO.setControlY2(Double.parseDouble(c2y.getValue()));
            LandMarkBO landMarkBO = landMarkBOHashMap.get(startPosId);
            pathBO.setStartPos(landMarkBO);
            landMarkBO = landMarkBOHashMap.get(endPosId);
            pathBO.setEndPos(landMarkBO);
            pathBOList.add(pathBO);
        }
        stopWatch.stop();
        stopWatch.start("封装到mapBo");
        MapBO mapBO = new MapBO();
        mapBO.setResolution(Double.parseDouble(resolutionValue));
        mapBO.setReflectorDiameter(Double.parseDouble(diameterValue));
        mapBO.setMinPosX(Double.parseDouble(minPosX.getValue()));
        mapBO.setMinPosY(Double.parseDouble(minPosY.getValue()));
        mapBO.setMaxPosX(Double.parseDouble(maxPosx.getValue()));
        mapBO.setMaxPosY(Double.parseDouble(maxPosY.getValue()));
        mapBO.setPointBOList(pointBOList);
        mapBO.setReflectCylinderBOList(new LinkedList<>());
        mapBO.setLandMarkBOList(landMarkBOList);
        mapBO.setPathBOList(pathBOList);
        stopWatch.stop();
        StopWatch.TaskInfo[] taskInfoArr = stopWatch.getTaskInfo();
        for (StopWatch.TaskInfo taskInfo : taskInfoArr) {
            log.info("{}:{}ms",taskInfo.getTaskName(),taskInfo.getTimeMillis());
        }
        log.info("parseMapXml 总耗时{}ms",stopWatch.getTotalTimeMillis());
        return mapBO;
    }
}
