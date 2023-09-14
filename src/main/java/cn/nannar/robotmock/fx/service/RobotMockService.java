package cn.nannar.robotmock.fx.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.nannar.robotmock.fx.bo.LandMarkBO;
import cn.nannar.robotmock.fx.bo.MapBO;
import cn.nannar.robotmock.fx.bo.PicPosRelPointBO;
import cn.nannar.robotmock.fx.constant.RobotStatus;
import cn.nannar.robotmock.fx.constant.TaskStatus;
import cn.nannar.robotmock.fx.dto.CreateTaskCmd;
import cn.nannar.robotmock.fx.dto.RdpsFrame;
import cn.nannar.robotmock.fx.dto.RealTimeDataDTO;
import cn.nannar.robotmock.fx.util.SenderHelper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.apache.activemq.command.ActiveMQTopic;
import org.apache.activemq.util.ByteSequence;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.jms.JMSException;
import javax.jms.Topic;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;

/**
 * @author LTJ
 * @date 2023/9/12
 */
@Service
@Slf4j
public class RobotMockService {



    private Integer robotId=1;

    private RobotStatus robotStatus=RobotStatus.IDLE;

    private List<RealTimeDataDTO.Task> taskList = new LinkedList<>();

    private MockBattery mockBattery = new MockBattery(new Date());

    private MockAgv mockAgv = new MockAgv();

    private MockArm mockArm = new MockArm();

    private MockCameraOf3d mockCameraOf3d = new MockCameraOf3d();

    private MockCameraOfNavigation mockCameraOfNavigation = new MockCameraOfNavigation();

    private MockLift mockLift = new MockLift();

    private MapBO mapBO;

    private Map<String,PicPosRelPointBO> picPointIdMap=Collections.emptyMap();
    private Map<String, Map<String, Object>> botPicPosCfgMap = new HashMap<>();

    private List<String> robotTroubleCodeList = new LinkedList<>();

    @Value("${robot.mapFile}")
    private String mapFileStr;

    private Map<String, Function<RdpsFrame<?>,?>> cmdHandleMap = new HashMap<>();

    @Autowired
    private JdbcTemplate jdbcTemplate;
    private Topic cmdTopic = new ActiveMQTopic("/web/cmd");
    private Topic ackTopic = new ActiveMQTopic("/rdps/ack");

    @Autowired
    private ThreadPoolTaskScheduler threadPoolTaskScheduler;

    @Autowired
    private ObjectMapper objectMapper;



    private String sql = "SELECT id,agv_parking_seq parkPoint,pic_seq picPoint,carriage_code carriageCode,axle_code axleCode " +
            "FROM bot_pic_pos_cfg " +
            "WHERE mfrs_code=0 and status=1 " +
            "ORDER BY pic_seq";
    @PostConstruct
    public void init(){
        File file = FileUtil.file(mapFileStr);
        try {
            mapBO = MapService.parseMapXml(file);
        } catch (Exception e) {
           log.error("解析地图错误");
        }

        // 查询数据库的拍照点配置
        List<Map<String, Object>> botPicPosCfgList = jdbcTemplate.queryForList(sql);
        List<LandMarkBO> pointBOList = mapBO.getLandMarkBOList();
        List<PicPosRelPointBO> picPosRelPointBOList = new LinkedList<>();
        Map<String, PicPosRelPointBO> picPointIdMapping = new HashMap<>();
        if (botPicPosCfgList.size()> pointBOList.size()) {
            // 拍照点按照地图路径点的段数平均分，得出每段（含头部，不含尾部）应该有的拍站点数，向下取整
            int pointPerSpan = botPicPosCfgList.size() / (pointBOList.size() - 1);
            int theLastBegin = (mapBO.getLandMarkBOList().size() - 2) * pointPerSpan;
            int remain = botPicPosCfgList.size() - theLastBegin;

            for(int i=0,size=botPicPosCfgList.size();i<size;i++){
                Map<String, Object> botPicPosCfg = botPicPosCfgList.get(i);
                String picPointId = (String) botPicPosCfg.get("id");
                botPicPosCfgMap.put(picPointId, botPicPosCfg);
                if(i<theLastBegin){
                    int segmentNum = i / pointPerSpan;
                    int numInSegment = i % pointPerSpan;
                    LandMarkBO startPoint = pointBOList.get(segmentNum);
                    LandMarkBO endPoint = pointBOList.get(segmentNum + 1);
                    double deltaX = (endPoint.getX() - startPoint.getX()) / (pointPerSpan + 1);
                    double deltaY = (endPoint.getY() - startPoint.getY()) / (pointPerSpan + 1);
                    PicPosRelPointBO picPosRelPointBO = new PicPosRelPointBO();
                    picPosRelPointBO.setSegmentNum(segmentNum);
                    picPosRelPointBO.setPicPointId(picPointId);
                    picPosRelPointBO.setBeginPointName(startPoint.getId());
                    picPosRelPointBO.setEndPointName(endPoint.getId());
                    picPosRelPointBO.setX(startPoint.getX()+deltaX*numInSegment);
                    picPosRelPointBO.setY(startPoint.getY() + deltaY * numInSegment);
                    picPosRelPointBOList.add(picPosRelPointBO);
                    picPointIdMapping.put(picPointId, picPosRelPointBO);
                }else{
                    int segmentNum = pointBOList.size()-2;
                    int numInSegment = (i-theLastBegin) % remain;
                    LandMarkBO startPoint = pointBOList.get(segmentNum);
                    LandMarkBO endPoint = pointBOList.get(segmentNum + 1);
                    double deltaX = (endPoint.getX() - startPoint.getX()) / (remain);
                    double deltaY = (endPoint.getY() - startPoint.getY()) / (remain);
                    PicPosRelPointBO picPosRelPointBO = new PicPosRelPointBO();
                    picPosRelPointBO.setSegmentNum(segmentNum);
                    picPosRelPointBO.setPicPointId(picPointId);
                    picPosRelPointBO.setBeginPointName(startPoint.getId());
                    picPosRelPointBO.setEndPointName(endPoint.getId());
                    picPosRelPointBO.setX(startPoint.getX()+deltaX*numInSegment);
                    picPosRelPointBO.setY(startPoint.getY() + deltaY * numInSegment);
                    picPosRelPointBOList.add(picPosRelPointBO);
                    picPointIdMapping.put(picPointId, picPosRelPointBO);
                }
            }

        }else{
            for(int i=0,size=botPicPosCfgList.size();i<size;i++) {
                Map<String, Object> botPicPosCfg = botPicPosCfgList.get(i);
                String picPointId = (String) botPicPosCfg.get("id");
                botPicPosCfgMap.put(picPointId, botPicPosCfg);
                LandMarkBO landMarkBO = pointBOList.get(i);
                int nextIdx=i+1;
                boolean isTheLast=false;
                if(i>pointBOList.size()-2){
                    nextIdx = pointBOList.size() - 1;
                    isTheLast=true;
                }
                LandMarkBO startPoint = pointBOList.get(i);
                LandMarkBO endPoint = pointBOList.get(nextIdx);


                PicPosRelPointBO picPosRelPointBO = new PicPosRelPointBO();
                picPosRelPointBO.setSegmentNum(i);
                picPosRelPointBO.setPicPointId(picPointId);
                picPosRelPointBO.setBeginPointName(startPoint.getId());
                picPosRelPointBO.setEndPointName(endPoint.getId());
                picPosRelPointBO.setX(startPoint.getX());
                picPosRelPointBO.setY(startPoint.getY());
                picPosRelPointBOList.add(picPosRelPointBO);
                picPointIdMapping.put(picPointId, picPosRelPointBO);
            }
        }
        picPointIdMap = Collections.unmodifiableMap(picPointIdMapping);
        cmdHandleMap.put(CreateTaskCmd.CMD, this::handleCreateTask);
        threadPoolTaskScheduler.scheduleWithFixedDelay(this::sendRealTimeData, 5000);

    }


    /**
     * 发送实时信息数据
     */
    private void sendRealTimeData(){
        /* begin ==========推进任务先============= */
        /* end ============xxx============ */

        RealTimeDataDTO realTimeDataDTO = new RealTimeDataDTO();


        RealTimeDataDTO.RobotInfo robotInfo = new RealTimeDataDTO.RobotInfo();
        robotInfo.setId(robotId);
        robotInfo.setCharging(mockBattery.getCharging());
        robotInfo.setStatus(judgeRobotStatus().getCode());
        robotInfo.setBatteryPercent(mockBattery.getCurrentPercent());
        robotInfo.setBatteryRemainTime(mockBattery.getRemainTime());
        robotInfo.setChargingCurrent(mockBattery.getChargingCurrent());
        robotInfo.setAgvVoltage(mockBattery.getVoltage());
        robotInfo.setTimeToFull(mockBattery.getTimeToFull());
        robotInfo.setTask(getTask());
        robotInfo.setTaskQueue(getTaskQueue());
        robotInfo.setPosition(getPosition2());
        robotInfo.setTroubleCodeList(getRobotTroubleCodeList());
        robotInfo.setAgv(mockAgv.getAgv());
        robotInfo.setBattery(mockBattery.getBattery());
        robotInfo.setArm(mockArm.getArm());
        robotInfo.setCameraOf3d(mockCameraOf3d.getCamera3d());
        robotInfo.setCameraOfNavigation(mockCameraOfNavigation.getCameraOfNavigation());

        ArrayList<RealTimeDataDTO.RobotInfo> robotInfoArrayList = CollUtil.newArrayList(robotInfo);
        ArrayList<RealTimeDataDTO.LiftInfo> liftInfos = CollUtil.newArrayList(mockLift.getLift());
        realTimeDataDTO.setLiftInfos(liftInfos);
        realTimeDataDTO.setRobotInfos(robotInfoArrayList);

        RdpsFrame<RealTimeDataDTO> realTimeDataDTORdpsFrame = new RdpsFrame<>();
        realTimeDataDTORdpsFrame.setPublishTime(new Date());
        realTimeDataDTORdpsFrame.setMsgType("normal");
        realTimeDataDTORdpsFrame.setPublisher("mockRdps");
        final int seq = SenderHelper.getSeqAndIncrement();
        realTimeDataDTORdpsFrame.setSeq(seq);
        realTimeDataDTORdpsFrame.setData(realTimeDataDTO);

    }


    @JmsListener(containerFactory = "jmsTopicListenerContainerFactory", destination = "/web/cmd")
    public void onCmd(ActiveMQTextMessage message) throws InterruptedException {
        String msg=null;
        try {
            msg=message.getText();
        } catch (JMSException e) {
            log.error("onCmd 接收文本错误",e);
            ByteSequence content = message.getContent();
            String s = new String(content.getData(), content.getOffset(), content.getLength(), StandardCharsets.UTF_8);
            log.error("s:{}", s);
        }
        if(msg==null){
            return;
        }
        if(log.isDebugEnabled()){
            log.debug("指令接收：{}", msg);
        }
        RdpsFrame rdpsFrame = null;
        try {
             rdpsFrame = objectMapper.readValue(msg, RdpsFrame.class);
        } catch (Exception e) {
            String buildLog = StrUtil.format("解析json失败:{}", msg);
            log.error(buildLog,e);
            return;
        }
        JsonNode jsonNode = objectMapper.valueToTree(rdpsFrame.getData());
        String cmd = jsonNode.get("cmd").asText();
        if("createTask".equals(cmd)){
            Class<?> createTaskCmdClass = CreateTaskCmd.class;
            try {
                Object o = objectMapper.treeToValue(jsonNode, createTaskCmdClass);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }else{

        }

        System.out.println("rdpsFrame = " + rdpsFrame);
//        ObjectNode cmdWrapper = (ObjectNode) rdpsFrame.getData();
//        String cmd = cmdWrapper.get("cmd");
//        // 根据不同的指令转化成不同的类
//        Class<?> structClass = ackStructMap.get(cmd);
//        if(structClass==null){
//            log.error("不存在cmd:{}对应的结构体解析体", cmd);
//        }
//        Object ackStruct = BeanUtil.mapToBean(cmdWrapper, structClass, false, null);
//        rdpsFrame.setData(ackStruct);
//        log.info("接收到应答ack:{},seq:{}",cmd,rdpsFrame.getSeq());
////        msgPipeMap.put(cmd, rdpsFrame);
//        if (rdpsBuffer.couldOperate(rdpsFrame.getSeq())) {
//            rdpsBuffer.add(rdpsFrame);
//        }else{
//            log.warn("seq:{} 对应空间没有操作许可,丢弃接收的数据",rdpsFrame.getSeq());
//        }

    }

    /**
     * 判断当前模拟机器人的状态
     * @return
     */
    private RobotStatus judgeRobotStatus(){
        if(RobotStatus.OFF_LINE.equals(robotStatus)){
            return RobotStatus.OFF_LINE;
        }
        if(taskList.isEmpty()){
            return RobotStatus.IDLE;
        }else{
            return RobotStatus.WORKING;
        }
    }

    private RealTimeDataDTO.Task getTask(){
        if(taskList.isEmpty()){
            return null;
        }
        RealTimeDataDTO.Task task = taskList.get(0);
        return task;
    }

    private List<RealTimeDataDTO.BriefTask> getTaskQueue(){
        if(taskList.isEmpty()){
            return null;
        }
        List<RealTimeDataDTO.BriefTask> briefTaskList = new LinkedList<>();
        for (RealTimeDataDTO.Task task : taskList) {
            RealTimeDataDTO.BriefTask briefTask = new RealTimeDataDTO.BriefTask();
            briefTask.setTaskId(task.getId());
            briefTask.setCmdSrc(task.getCmdSrc());
            briefTask.setLaneId(task.getLaneId());
            briefTaskList.add(briefTask);
        }
        return briefTaskList;
    }

    /**
     * todo
     * 要根据读取的地图进行坐标模拟
     * @return
     */
    private RealTimeDataDTO.Position2 getPosition2(){
        RealTimeDataDTO.Position2 position2 = new RealTimeDataDTO.Position2();

        if(taskList.isEmpty()){
            position2.setX(mapBO.getMinPosX());
            position2.setY(mapBO.getMinPosY());
            position2.setAngle(0d);
            return position2;
        }

        RealTimeDataDTO.Task task = taskList.get(0);
        RealTimeDataDTO.Position position = task.getPosition();
        String picPointId = position.getPicPointId();
        PicPosRelPointBO picPosRelPointBO = picPointIdMap.get(picPointId);
        if(picPosRelPointBO==null){
            position2.setX(mapBO.getMinPosX());
            position2.setY(mapBO.getMinPosY());
            position2.setAngle(0d);
            return position2;
        }else{
            position2.setX(picPosRelPointBO.getX());
            position2.setY(picPosRelPointBO.getY());
            position2.setAngle(null);
            return position2;
        }
    }

    /**
     * 获取机器人的故障编码列表
     * @return
     */
    private  List<RealTimeDataDTO.TroubleCode> getRobotTroubleCodeList(){
        List<RealTimeDataDTO.TroubleCode> list = new LinkedList<>();
        for (String troubleCodeStr : robotTroubleCodeList) {
            RealTimeDataDTO.TroubleCode troubleCode = new RealTimeDataDTO.TroubleCode();
            troubleCode.setTroubleCode(troubleCodeStr);
            list.add(troubleCode);
        }
        return list;
    }

    private Object handleCreateTask(RdpsFrame rdpsFrame){
        CreateTaskCmd createTaskCmd = BeanUtil.mapToBean(((Map) rdpsFrame.getData()), CreateTaskCmd.class, false, null);
        CreateTaskCmd.Params params = createTaskCmd.getParams();
        Long taskId = params.getTaskId();
        List<String> picPointIdStrList = params.getPicPointIdStrList();
        if(picPointIdStrList==null){
            picPointIdStrList = Collections.emptyList();
        }
        List<String> landMarkList = new LinkedList<>();
        String preLandMark = "";
        for (String picPointId : picPointIdStrList) {
            PicPosRelPointBO picPosRelPointBO = picPointIdMap.get(picPointId);
            if(picPosRelPointBO!=null){
                String beginPointName = picPosRelPointBO.getBeginPointName();
                if(Objects.equals(preLandMark,beginPointName)){
                    continue;
                }
                preLandMark=beginPointName;
                landMarkList.add(beginPointName);
            }
        }

        RealTimeDataDTO.Task task = new RealTimeDataDTO.Task();
        task.setId(params.getTaskId());
        task.setAccumulatedTime(0);
        task.setCmdSrc("web");
        task.setPercent(0);
        task.setStatus(0);
        task.setPosition(null);
        task.setPicPointIdStr(params.getPicPointIdStrList());
        task.setLandMarkList(landMarkList);
        // todo 判断任务id有没有重复

        taskList.add(task);
        // 回复成功
        return true;
    }

    /**
     * 推进任务进度
     */
    private void stepTask(){
        if(taskList.isEmpty()){
            return;
        }
        RealTimeDataDTO.Task task = taskList.get(0);
        synchronized (task){}
        Integer status = task.getStatus();
        if(TaskStatus.WAITING.getCode().equals(status)){    // 任务等待中
            List<String> picPointIdStrList = task.getPicPointIdStr();
            List<String> landMarkList = task.getLandMarkList();
            if(picPointIdStrList.isEmpty()){
                return;
            }
            // 当前要去的拍照点
            int curPicPointIdx =0;
            int curLandMarkIdx=0;
            String picPointId=null;
            Map<String, Object> botPicCfg=null;
            for(int i=curPicPointIdx,size=picPointIdStrList.size();i<size;i++){
                picPointId=  picPointIdStrList.get(curPicPointIdx);
                botPicCfg = botPicPosCfgMap.get(picPointId);
                curPicPointIdx=i;
                if(botPicCfg!=null){
                    break;
                }
            }
            if(botPicCfg==null){ // 找不到一个有效的拍照点则任务完成
                task.setStatus(TaskStatus.FINISHED.getCode());
                task.setCurPicPointIdx(curPicPointIdx);
            }else{  // 找到拍照点
                Integer carriageCode = (Integer) botPicCfg.get("carriageCode");
                task.setStatus(TaskStatus.WORKING.getCode());
                RealTimeDataDTO.Position position = new RealTimeDataDTO.Position();
                position.setPicPointId(picPointId);
                PicPosRelPointBO picPosRelPointBO = picPointIdMap.get(picPointId);
                if(picPosRelPointBO!=null){
                    String landMarkName = landMarkList.get(curLandMarkIdx);
                    if (!Objects.equals(landMarkName, picPosRelPointBO.getBeginPointName())) {
                        curLandMarkIdx++;
                        task.setCurLandMarkIdx(curLandMarkIdx);
                    }
                    position.setPrePassPointIdx(curLandMarkIdx);
                }else{
                    position.setPrePassPointIdx(0);
                }

                position.setCarriage(carriageCode);
                position.setAgvPosPercent(curPicPointIdx %10 *10);

                task.setPosition(position);
                task.setCurPicPointIdx(curPicPointIdx);
            }
        }else if(TaskStatus.WORKING.getCode().equals(status)){
            List<String> picPointIdStr = task.getPicPointIdStr();
            if(picPointIdStr.isEmpty()){
                return;
            }
            int curPicPoint=task.getCurPicPointIdx()+1;
            if(curPicPoint>=picPointIdStr.size()){

            }else{

            }
            String picPointId=  picPointIdStr.get(curPicPoint);
            Map<String, Object> botPicCfg = botPicPosCfgMap.get(picPointId);
            if(botPicCfg==null){
                return;
            }
            Integer carriageCode = (Integer) botPicCfg.get("carriageCode");
            Integer axleCode = (Integer) botPicCfg.get("axleCode");

            task.setStatus(TaskStatus.WORKING.getCode());
            RealTimeDataDTO.Position position = new RealTimeDataDTO.Position();
            position.setPicPointId(picPointId);
            position.setPrePassPointIdx(curPicPoint);
            position.setCarriage(carriageCode);
            position.setAgvPosPercent(curPicPoint%10 *10);

            task.setPosition(position);
            task.setCurPicPointIdx(curPicPoint);
        }

        task.setAccumulatedTime(Long.valueOf(DateUtil.between(task.getTaskStartTime(), new Date(), DateUnit.MINUTE)).intValue());
        double v = (task.getCurPicPointIdx() + 1.0) / task.getPicPointIdStr().size();
        task.setPercent(Double.valueOf(v).intValue());

    }

}
