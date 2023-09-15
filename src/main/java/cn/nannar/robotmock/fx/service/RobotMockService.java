package cn.nannar.robotmock.fx.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.nannar.robotmock.entity.BotAlarmLog;
import cn.nannar.robotmock.entity.BotInspectLog;
import cn.nannar.robotmock.entity.BotPicPosCfg;
import cn.nannar.robotmock.fx.bo.LandMarkBO;
import cn.nannar.robotmock.fx.bo.MapBO;
import cn.nannar.robotmock.fx.bo.PicPosRelPointBO;
import cn.nannar.robotmock.fx.constant.RobotStatus;
import cn.nannar.robotmock.fx.constant.TaskStatus;
import cn.nannar.robotmock.fx.dao.BotAlarmLogMapper;
import cn.nannar.robotmock.fx.dao.BotInspectLogMapper;
import cn.nannar.robotmock.fx.dao.BotPicPosCfgMapper;
import cn.nannar.robotmock.fx.dto.*;
import cn.nannar.robotmock.fx.tasksatus.SuspendState;
import cn.nannar.robotmock.fx.tasksatus.TaskState;
import cn.nannar.robotmock.fx.tasksatus.WaittingState;
import cn.nannar.robotmock.fx.tasksatus.WorkingState;
import cn.nannar.robotmock.fx.util.SenderHelper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import lombok.extern.slf4j.Slf4j;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.apache.activemq.command.ActiveMQTopic;
import org.apache.activemq.util.ByteSequence;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.jms.JMSException;
import javax.jms.Topic;
import java.io.File;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;

/**
 * @author LTJ
 * @date 2023/9/12
 */
@Service
@Slf4j
public class RobotMockService {
    public static final String GATHER_JSON = "result.json";
    public static final String R_1_IMG = "r_1.jpg";
    public static final String REF_IMG = "ref.jpg";
    public static final String SINGLE_JSON = "result.json";


    private Integer robotId=1;

    private RobotStatus robotStatus=RobotStatus.IDLE;

    public List<RealTimeDataDTO.Task> taskList = new LinkedList<>();

    private MockBattery mockBattery = new MockBattery(new Date());

    private MockAgv mockAgv = new MockAgv();

    private MockArm mockArm = new MockArm();

    private MockCameraOf3d mockCameraOf3d = new MockCameraOf3d();

    private MockCameraOfNavigation mockCameraOfNavigation = new MockCameraOfNavigation();

    private MockLift mockLift = new MockLift(1);

    private MapBO mapBO;

    public Map<String,PicPosRelPointBO> picMapLandmarkMap =Collections.emptyMap();
    public Map<String, BotPicPosCfg> botPicPosCfgMap = new HashMap<>();
    public List<RealTimePicPointDoneDTO> donePicPointList = new LinkedList<>();

    private List<String> robotTroubleCodeList = new LinkedList<>();

    @Value("${robot.mapFile}")
    private String mapFileStr;

    @Value(("${robot.mockDataFile}"))
    private String mockDataFileStr;

    @Value(("${robot.dcsData}"))
    private String dcsData;

    private Map<String, Function<RdpsFrame<?>,?>> cmdHandleMap = new HashMap<>();


    @Autowired
    private JmsTemplate jmsTemplate;

    private Topic ackTopic = new ActiveMQTopic("/rdps/ack");
    private Topic resultDoneTopic = new ActiveMQTopic("/rdps/realTime/resultDone");
    private Topic realTimeTopic = new ActiveMQTopic("/rdps/realTime/data");

    @Autowired
    private ThreadPoolTaskScheduler threadPoolTaskScheduler;

    @Autowired
    private ObjectMapper objectMapper;

    @Resource
    private BotPicPosCfgMapper botPicPosCfgMapper;

    @Resource
    private BotInspectLogMapper botInspectLogMapper;

    @Resource
    private BotAlarmLogMapper botAlarmLogMapper;

    public ObjectWriter objectWriter;


    @PostConstruct
    public void init(){
        File file = FileUtil.file(mapFileStr);
        try {
            mapBO = MapService.parseMapXml(file);
        } catch (Exception e) {
           log.error("解析地图错误");
        }

        // 查询数据库的拍照点配置

        LambdaQueryWrapper<BotPicPosCfg> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BotPicPosCfg::getMfrsCode, 0);
        wrapper.eq(BotPicPosCfg::getStatus, 1);
        wrapper.orderByAsc(BotPicPosCfg::getPicSeq);
        List<BotPicPosCfg> botPicPosCfgList = botPicPosCfgMapper.selectList(wrapper);
        List<LandMarkBO> pointBOList = mapBO.getLandMarkBOList();
        List<PicPosRelPointBO> picPosRelPointBOList = new LinkedList<>();
        Map<String, PicPosRelPointBO> picPointIdMapping = new HashMap<>();
        if (botPicPosCfgList.size()> pointBOList.size()) {
            // 拍照点按照地图路径点的段数平均分，得出每段（含头部，不含尾部）应该有的拍站点数，向下取整
            int pointPerSpan = botPicPosCfgList.size() / (pointBOList.size() - 1);
            int theLastBegin = (mapBO.getLandMarkBOList().size() - 2) * pointPerSpan;
            int remain = botPicPosCfgList.size() - theLastBegin;

            for(int i=0,size=botPicPosCfgList.size();i<size;i++){
                BotPicPosCfg botPicPosCfg = botPicPosCfgList.get(i);
                String picPointId = botPicPosCfg.getId();
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
                    picPosRelPointBO.setEndX(endPoint.getX());
                    picPosRelPointBO.setEndY(endPoint.getY());
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
                BotPicPosCfg botPicPosCfg = botPicPosCfgList.get(i);
                String picPointId = botPicPosCfg.getId();
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
                picPosRelPointBO.setEndX(endPoint.getX());
                picPosRelPointBO.setEndY(endPoint.getY());
                picPosRelPointBOList.add(picPosRelPointBO);
                picPointIdMapping.put(picPointId, picPosRelPointBO);
            }
        }
        picMapLandmarkMap = Collections.unmodifiableMap(picPointIdMapping);
        /* begin ==========命令响应============= */
        cmdHandleMap.put(CreateTaskCmd.CMD, this::handleCreateTask);
        cmdHandleMap.put(SuspendTaskCmd.CMD, this::handleSuspend);
        cmdHandleMap.put(ResumeTaskCmd.CMD, this::handleResume);
        cmdHandleMap.put(StopTaskCmd.CMD, this::handleStop);
        cmdHandleMap.put(ChangeQueuePosCmd.CMD, this::handleChangeQueuePos);
        cmdHandleMap.put(GoHomeCmd.CMD, this::handleGohome);
        /* end ============命令响应============ */

        /* begin ==========消息推送============= */
        threadPoolTaskScheduler.scheduleWithFixedDelay(this::sendRealTimeData, 5000);
        threadPoolTaskScheduler.scheduleWithFixedDelay(this::sendDoneData, 5000);
        /* end ============消息推送============ */
        objectWriter = objectMapper.writerWithDefaultPrettyPrinter();
    }

    /**
     * 发送完成任务的数据
     */
    private void sendDoneData(){
        RealTimePicPointDoneDTO remove=null;
        synchronized (donePicPointList){
            if(!donePicPointList.isEmpty()){
                 remove = donePicPointList.remove(0);
            }
        }
        // todo
        if(remove!=null){
            BotInspectLog botInspectLog = botInspectLogMapper.selectById(remove.getTaskId());
            /* begin ==========操作磁盘============= */
            String dir = remove.getDir();
            String picPointId = remove.getPicPointId();
            File traceFile = FileUtil.file(dcsData, dir);
            if (!traceFile.exists()) {
                log.info("创建目录：{}",traceFile);
                FileUtil.mkdir(traceFile);
            }

            ResultJsonGatherDO resultJsonGatherDO=null;
            File gatherJson = FileUtil.file(traceFile, GATHER_JSON);
            if(gatherJson.exists()){
                try {
                    resultJsonGatherDO= objectMapper.readValue(FileUtil.readUtf8String(gatherJson), ResultJsonGatherDO.class);
                } catch (Exception e) {
                    log.error("", e);
                }
            }
            if(resultJsonGatherDO==null){

                resultJsonGatherDO = new ResultJsonGatherDO();
                resultJsonGatherDO.setTaskId(remove.getTaskId());
                resultJsonGatherDO.setRobotId(botInspectLog.getBotCode());
                resultJsonGatherDO.setCmdSrc(remove.getCmdSrc());
                resultJsonGatherDO.setLaneId(botInspectLog.getParkingLaneCode());
                resultJsonGatherDO.setTraceTime(botInspectLog.getTraceTime());
                resultJsonGatherDO.setTrainNo(botInspectLog.getTrainNo());
            }
            ResultJsonSingleDO resultJsonSingleDO=null;
            if(!remove.getFinished()){
                List<ResultJsonSingleDO> photoData = resultJsonGatherDO.getPhotoData();
                if(photoData==null){
                    photoData = new LinkedList<>();
                    resultJsonGatherDO.setPhotoData(photoData);
                }
                 resultJsonSingleDO = new ResultJsonSingleDO();
                resultJsonSingleDO.setTrainNo(botInspectLog.getTrainNo());
                resultJsonSingleDO.setRobotId(botInspectLog.getBotCode());
                resultJsonSingleDO.setLaneId(botInspectLog.getParkingLaneCode());
                resultJsonSingleDO.setTraceTime(botInspectLog.getTraceTime());
                resultJsonSingleDO.setDirection(botInspectLog.getDirection());

                resultJsonSingleDO.setPicPointId(remove.getPicPointId());
                resultJsonSingleDO.setTaskId(remove.getTaskId());
                resultJsonSingleDO.setCmdSrc(remove.getCmdSrc());
                resultJsonSingleDO.setCarriage(remove.getCarriage());
                resultJsonSingleDO.setPartInfo(remove.getPartInfo());
                resultJsonSingleDO.setAgvPosPercent(remove.getAgvPosPercent());
                if(remove.getPartInfo()!=null){
                    resultJsonSingleDO.setErrorNum(remove.getPartInfo().size());
                }
                photoData.add(resultJsonSingleDO);
            }

            try {
                String s = objectWriter.writeValueAsString(resultJsonGatherDO);
                FileUtil.writeUtf8String(s, gatherJson);
            } catch (JsonProcessingException e) {
                log.error("", e);
            }

            if(!remove.getFinished()){
                File picPointDir = FileUtil.file(dcsData, dir, picPointId);
                if(!picPointDir.exists()){
                    FileUtil.mkdir(picPointDir);
                }
                File singleJson = FileUtil.file(picPointDir, SINGLE_JSON);
                try {
                    String s = objectWriter.writeValueAsString(resultJsonSingleDO);
                    FileUtil.writeUtf8String(s, singleJson);
                } catch (JsonProcessingException e) {
                    log.error("", e);
                }
                File srcR1Img = FileUtil.file(mockDataFileStr, R_1_IMG);
                if(srcR1Img.exists()){
                    FileUtil.copyFile(srcR1Img, FileUtil.file(picPointDir, R_1_IMG));
                }
                File srcRefImg = FileUtil.file(mockDataFileStr, REF_IMG);
                if(srcRefImg.exists()){
                    FileUtil.copyFile(srcRefImg,   FileUtil.file(picPointDir, REF_IMG));
                }
            }




            /* end ============操作磁盘============ */

            /* begin ==========上送告警============= */
            if(!remove.getFinished()){
                List<ResultJsonSingleDO.ErrorInfo> partInfo = remove.getPartInfo();
                if(partInfo!=null){
                    for(int i=0,size=partInfo.size();i<size;i++){
                        ResultJsonSingleDO.ErrorInfo errorInfo = partInfo.get(i);
                        BotAlarmLog botAlarmLog = new BotAlarmLog();
                        botAlarmLog.setPicPointId(remove.getPicPointId());
                        botAlarmLog.setBotInspectId(botInspectLog.getId());
                        botAlarmLog.setTrainNo(botInspectLog.getTrainNo());
                        botAlarmLog.setBotCode(botAlarmLog.getBotCode());
                        botAlarmLog.setCarriageCode(remove.getCarriage());
                        botAlarmLog.setArrIdx(i);
                        botAlarmLog.setVersion(0);
                        botAlarmLog.setFlagAlarm(3);
                        botAlarmLog.setDescription(errorInfo.getResultDesc());
                        botAlarmLog.setCreateTime(new Date());
                        botAlarmLog.setCheckId(19);
                        botAlarmLog.setAlarmLevel(1);
                        botAlarmLog.setAlarmStatus(0);
                        botAlarmLog.setAlarmValue(new BigDecimal("1"));
                        botAlarmLog.setDaAlarmValue(new BigDecimal("1"));
                        botAlarmLog.setDaAlarmLevel(1);
                        botAlarmLog.setAlarmMark(0);
                        botAlarmLog.setAlarmImgXy(CollUtil.join(errorInfo.getPicRect(), ","));
                        botAlarmLogMapper.insert(botAlarmLog);
                    }
                }
            }
            /* end ============上送告警============ */

            /* begin ==========发送信号============= */
            try {
                RdpsFrame<RealTimePicPointDoneDTO> rdpsFrame = new RdpsFrame<>();
                rdpsFrame.setMsgType("normal");
                rdpsFrame.setPublisher("mockrobot");
                rdpsFrame.setPublishTime(new Date());
                rdpsFrame.setSeq(SenderHelper.getSeqAndIncrement());
                rdpsFrame.setData(remove);
                String s = objectWriter.writeValueAsString(rdpsFrame);
                log.info("发送完成点：{}", picPointId);
                jmsTemplate.convertAndSend(resultDoneTopic,s);
            } catch (JsonProcessingException e) {
                log.error("", e);
            }
            /* end ============发送信号============ */
        }

    }

    /**
     * 发送实时信息数据
     */
    private void sendRealTimeData(){
        /* begin ==========推进任务先============= */
        stepTask();
        /* end ============推进任务先============ */

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

        try {
            String s = objectWriter.writeValueAsString(realTimeDataDTORdpsFrame);
            jmsTemplate.convertAndSend(realTimeTopic,s);
        } catch (JsonProcessingException e) {
            log.error("", e);
        }

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
        Function<RdpsFrame<?>, ?> rdpsFrameFunction = cmdHandleMap.get(cmd);
        if(rdpsFrameFunction!=null){
            rdpsFrameFunction.apply(rdpsFrame);
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
        if(position!=null){
            String picPointId = position.getPicPointId();
            PicPosRelPointBO picPosRelPointBO = picMapLandmarkMap.get(picPointId);
            if(picPosRelPointBO==null){
                position2.setX(mapBO.getMinPosX());
                position2.setY(mapBO.getMinPosY());
                position2.setAngle(0d);
                return position2;
            }else{
                position2.setX(picPosRelPointBO.getX());
                position2.setY(picPosRelPointBO.getY());
                double x1 = picPosRelPointBO.getX();
                double y1 = picPosRelPointBO.getY();
                double x2 = picPosRelPointBO.getEndX();
                double y2 = picPosRelPointBO.getEndY();
                double angle=0;
                if(x2-x1<0.0000000){
                    angle=Math.PI/2;
                }else{
                    angle = Math.atan((y2 - y1) / (x2 - x1));
                }

                position2.setAngle(angle);
                return position2;
            }
        }else{
            position2.setX(mapBO.getMinPosX());
            position2.setY(mapBO.getMinPosY());
            position2.setAngle(0d);
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
            PicPosRelPointBO picPosRelPointBO = picMapLandmarkMap.get(picPointId);
            if(picPosRelPointBO!=null){
                String beginPointName = picPosRelPointBO.getBeginPointName();
                if(Objects.equals(preLandMark,beginPointName)){
                    continue;
                }
                preLandMark=beginPointName;
                landMarkList.add(beginPointName);
            }
        }

        // todo 判断任务id有没有重复
        for (RealTimeDataDTO.Task task : taskList) {
            if(task.getId().equals(params.getTaskId())){
                CreateTaskAck createTaskAck = new CreateTaskAck();
                createTaskAck.setCmd(CreateTaskCmd.CMD);
                CreateTaskAck.Result result = new CreateTaskAck.Result();
                result.setSuccess(false);
                result.setMapPointList(null);
                createTaskAck.setResult(result);

                RdpsFrame<CreateTaskAck> ackRdpsFrame = new RdpsFrame<>();
                ackRdpsFrame.setMsgType("rsp");
                ackRdpsFrame.setPublisher("mockRdps");
                ackRdpsFrame.setPublishTime(new Date());
                ackRdpsFrame.setSeq(rdpsFrame.getSeq());
                ackRdpsFrame.setData(createTaskAck);
                try {
                    String s = objectWriter.writeValueAsString(ackRdpsFrame);
                    jmsTemplate.convertAndSend(ackTopic, s);
                } catch (JsonProcessingException e) {
                    log.error("",e);
                }
                return false;
            }
        }

        RealTimeDataDTO.Task task = new RealTimeDataDTO.Task();
        task.setId(params.getTaskId());
        task.setLaneId(params.getLaneId());
        task.setTrainNo(params.getTrainNo());
        task.setDirection(params.getDirection());
        task.setCmdSrc("web");
        task.setPercent(0);
        task.setAccumulatedTime(0);
        task.setPosition(null);
        task.setPicPointIdStr(params.getPicPointIdStrList());
        task.setLandMarkList(landMarkList);
        task.setStatus(TaskStatus.WAITING.getCode());
        task.setTaskState(new WaittingState(task));
        task.setTaskStartTime(new Date());


        taskList.add(task);
        /* begin ==========回填数据库============= */
        BotInspectLog botInspectLog = botInspectLogMapper.selectById(taskId);
        Date createTime = new Date();
        int year = DateUtil.year(createTime);
        int month = DateUtil.month(createTime) + 1;
        int dayOfMonth = DateUtil.dayOfMonth(createTime);
        String format = DateUtil.format(createTime, DatePattern.PURE_DATETIME_PATTERN);
        String dirStr = Paths.get(String.valueOf(year), String.valueOf(month), String.valueOf(dayOfMonth), format).toString();
        task.setTraceFile(dirStr);
        botInspectLog.setTraceFile(dirStr);
        botInspectLogMapper.updateById(botInspectLog);
        /* end ============回填数据库============ */

       /* begin ==========回复信息============= */
        CreateTaskAck createTaskAck = new CreateTaskAck();
        createTaskAck.setCmd(CreateTaskCmd.CMD);
        CreateTaskAck.Result result = new CreateTaskAck.Result();
        result.setSuccess(true);
        result.setMapPointList(landMarkList);
        createTaskAck.setResult(result);

        RdpsFrame<CreateTaskAck> ackRdpsFrame = new RdpsFrame<>();
        ackRdpsFrame.setMsgType("rsp");
        ackRdpsFrame.setPublisher("mockRdps");
        ackRdpsFrame.setPublishTime(new Date());
        ackRdpsFrame.setSeq(rdpsFrame.getSeq());
        ackRdpsFrame.setData(createTaskAck);
        try {
            String s = objectWriter.writeValueAsString(ackRdpsFrame);
            jmsTemplate.convertAndSend(ackTopic, s);
        } catch (JsonProcessingException e) {
            log.error("",e);
        }

       /* end ============回复信息============ */
        return true;
    }

    private Void handleSuspend(RdpsFrame rdpsFrame){
        SuspendTaskCmd suspendTaskCmd = BeanUtil.mapToBean(((Map) rdpsFrame.getData()), SuspendTaskCmd.class, false, null);
        SuspendTaskCmd.Params params = suspendTaskCmd.getParams();
        Long taskId = params.getTaskId();
        synchronized (taskList){
            for (RealTimeDataDTO.Task task : taskList) {
                if(task.getId().equals(taskId)){
                    if (TaskStatus.WORKING.getCode().equals(task.getStatus())) {
                        synchronized (task){
                            task.setStatus(TaskStatus.SUSPENDING.getCode());
                            task.setTaskState(new SuspendState());
                        }
                    }
                }
            }
        }
        RdpsBoolAck rdpsBoolAck = new RdpsBoolAck();
        rdpsBoolAck.setCmd(SuspendTaskCmd.CMD);
        RdpsBoolAck.Result result = new RdpsBoolAck.Result();
        result.setSuccess(true);
        rdpsBoolAck.setResult(result);
        RdpsFrame<RdpsBoolAck> ackRdpsFrame = new RdpsFrame<>();
        ackRdpsFrame.setMsgType("rsp");
        ackRdpsFrame.setPublisher("mockRdps");
        ackRdpsFrame.setPublishTime(new Date());
        ackRdpsFrame.setSeq(rdpsFrame.getSeq());
        ackRdpsFrame.setData(rdpsBoolAck);
        try {
            String s = objectWriter.writeValueAsString(ackRdpsFrame);
            jmsTemplate.convertAndSend(ackTopic, s);
        } catch (JsonProcessingException e) {
            log.error("",e);
        }
        return null;
    }

    private Void handleResume(RdpsFrame rdpsFrame){
        ResumeTaskCmd resumeTaskCmd = BeanUtil.mapToBean(((Map) rdpsFrame.getData()), ResumeTaskCmd.class, false, null);
        ResumeTaskCmd.Params params = resumeTaskCmd.getParams();
        Long taskId = params.getTaskId();
        synchronized (taskList){
            for (RealTimeDataDTO.Task task : taskList) {
                if(task.getId().equals(taskId)){
                    if (TaskStatus.SUSPENDING.getCode().equals(task.getStatus())) {
                        synchronized (task){
                            task.setStatus(TaskStatus.WORKING.getCode());
                            task.setTaskState(new WorkingState(task));
                        }
                    }
                }
            }
        }
        RdpsBoolAck rdpsBoolAck = new RdpsBoolAck();
        rdpsBoolAck.setCmd(ResumeTaskCmd.CMD);
        RdpsBoolAck.Result result = new RdpsBoolAck.Result();
        result.setSuccess(true);
        rdpsBoolAck.setResult(result);
        RdpsFrame<RdpsBoolAck> ackRdpsFrame = new RdpsFrame<>();
        ackRdpsFrame.setMsgType("rsp");
        ackRdpsFrame.setPublisher("mockRdps");
        ackRdpsFrame.setPublishTime(new Date());
        ackRdpsFrame.setSeq(rdpsFrame.getSeq());
        ackRdpsFrame.setData(rdpsBoolAck);
        try {
            String s = objectWriter.writeValueAsString(ackRdpsFrame);
            jmsTemplate.convertAndSend(ackTopic, s);
        } catch (JsonProcessingException e) {
            log.error("",e);
        }
        return null;
    }

    private Void handleStop(RdpsFrame rdpsFrame){
        StopTaskCmd stopTaskCmd = BeanUtil.mapToBean(((Map) rdpsFrame.getData()), StopTaskCmd.class, false, null);
        StopTaskCmd.Params params = stopTaskCmd.getParams();
        Long taskId = params.getTaskId();
        synchronized (taskList){
            Iterator<RealTimeDataDTO.Task> iterator = taskList.iterator();
            while (iterator.hasNext()){
                RealTimeDataDTO.Task task = iterator.next();
                if(task.getId().equals(taskId)){
                    synchronized (task) {
                        task.setStatus(TaskStatus.STOPPED.getCode());
                        task.setTaskState(new SuspendState());
                    }
                    iterator.remove();
                    BotInspectLog botInspectLog = botInspectLogMapper.selectById(taskId);
                    if(botInspectLog!=null){
                        botInspectLog.setTaskStatus(TaskStatus.STOPPED.getCode());
                        botInspectLog.setTranceStatus(100);
                        botInspectLogMapper.updateById(botInspectLog);
                    }
                }
            }
        }
        RdpsBoolAck rdpsBoolAck = new RdpsBoolAck();
        rdpsBoolAck.setCmd(StopTaskCmd.CMD);
        RdpsBoolAck.Result result = new RdpsBoolAck.Result();
        result.setSuccess(true);
        rdpsBoolAck.setResult(result);
        RdpsFrame<RdpsBoolAck> ackRdpsFrame = new RdpsFrame<>();
        ackRdpsFrame.setMsgType("rsp");
        ackRdpsFrame.setPublisher("mockRdps");
        ackRdpsFrame.setPublishTime(new Date());
        ackRdpsFrame.setSeq(rdpsFrame.getSeq());
        ackRdpsFrame.setData(rdpsBoolAck);
        try {
            String s = objectWriter.writeValueAsString(ackRdpsFrame);
            jmsTemplate.convertAndSend(ackTopic, s);
        } catch (JsonProcessingException e) {
            log.error("",e);
        }
        return null;
    }

    private Void handleChangeQueuePos(RdpsFrame rdpsFrame){
        ChangeQueuePosCmd changeQueuePosCmd = BeanUtil.mapToBean(((Map) rdpsFrame.getData()), ChangeQueuePosCmd.class, false, null);
        ChangeQueuePosCmd.Params params = changeQueuePosCmd.getParams();
        Integer pos = params.getPos();
        Long taskId = params.getTaskId();
        synchronized (taskList){
            RealTimeDataDTO.Task tagetTask=null;
            for (RealTimeDataDTO.Task task : taskList) {
                if(task.getId().equals(taskId)){
                    tagetTask=task;
                }
            }
            taskList.remove(tagetTask);
            taskList.add(pos, tagetTask);
        }
        RdpsBoolAck rdpsBoolAck = new RdpsBoolAck();
        rdpsBoolAck.setCmd(ChangeQueuePosCmd.CMD);
        RdpsBoolAck.Result result = new RdpsBoolAck.Result();
        result.setSuccess(true);
        rdpsBoolAck.setResult(result);
        RdpsFrame<RdpsBoolAck> ackRdpsFrame = new RdpsFrame<>();
        ackRdpsFrame.setMsgType("rsp");
        ackRdpsFrame.setPublisher("mockRdps");
        ackRdpsFrame.setPublishTime(new Date());
        ackRdpsFrame.setSeq(rdpsFrame.getSeq());
        ackRdpsFrame.setData(rdpsBoolAck);
        try {
            String s = objectWriter.writeValueAsString(ackRdpsFrame);
            jmsTemplate.convertAndSend(ackTopic, s);
        } catch (JsonProcessingException e) {
            log.error("",e);
        }
        return null;
    }

    private Void handleGohome(RdpsFrame rdpsFrame){
        GoHomeCmd goHomeCmd = BeanUtil.mapToBean(((Map) rdpsFrame.getData()), GoHomeCmd.class, false, null);
        GoHomeCmd.Params params = goHomeCmd.getParams();
        synchronized (taskList){
           taskList.clear();
        }
        mockBattery.setCharging(true);
        RdpsBoolAck rdpsBoolAck = new RdpsBoolAck();
        rdpsBoolAck.setCmd(GoHomeCmd.CMD);
        RdpsBoolAck.Result result = new RdpsBoolAck.Result();
        result.setSuccess(true);
        rdpsBoolAck.setResult(result);
        RdpsFrame<RdpsBoolAck> ackRdpsFrame = new RdpsFrame<>();
        ackRdpsFrame.setMsgType("rsp");
        ackRdpsFrame.setPublisher("mockRdps");
        ackRdpsFrame.setPublishTime(new Date());
        ackRdpsFrame.setSeq(rdpsFrame.getSeq());
        ackRdpsFrame.setData(rdpsBoolAck);
        try {
            String s = objectWriter.writeValueAsString(ackRdpsFrame);
            jmsTemplate.convertAndSend(ackTopic, s);
        } catch (JsonProcessingException e) {
            log.error("",e);
        }
        return null;
    }

    /**
     * 推进任务进度
     */
    private void stepTask(){
        if(taskList.isEmpty()){
            return;
        }
        RealTimeDataDTO.Task task = taskList.get(0);
        log.info("步进任务：{}", task.getId());
        task.doAction();
    }

}
