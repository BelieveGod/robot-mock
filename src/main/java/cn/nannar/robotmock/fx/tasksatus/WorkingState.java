package cn.nannar.robotmock.fx.tasksatus;

import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.nannar.robotmock.entity.BotPicPosCfg;
import cn.nannar.robotmock.fx.bo.PicPosRelPointBO;
import cn.nannar.robotmock.fx.constant.TaskStatus;
import cn.nannar.robotmock.fx.dto.RealTimeDataDTO;
import cn.nannar.robotmock.fx.dto.RealTimePicPointDoneDTO;
import cn.nannar.robotmock.fx.service.RobotMockService;
import cn.nannar.robotmock.fx.util.SpringContextHolder;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * @author LTJ
 * @date 2023/9/15
 */
@Slf4j
public class WorkingState implements TaskState{
    private RealTimeDataDTO.Task  task;

    public WorkingState(RealTimeDataDTO.Task task) {
        this.task = task;
    }
    @Override
    public void doAction() {
        RobotMockService robotMockService = SpringContextHolder.getBean(RobotMockService.class);
        synchronized (task) {
            List<String> picPointIdStrList = task.getPicPointIdStr();
            List<String> landMarkList = task.getLandMarkList();
            int curPicPointIdx = task.getCurPicPointIdx();
            int curLandMarkIdx = task.getCurLandMarkIdx();

            /* begin ==========触发当前节点的完成============= */
            RealTimePicPointDoneDTO realTimePicPointDoneDTO = new RealTimePicPointDoneDTO();
            realTimePicPointDoneDTO.setPicPointId(task.getPosition().getPicPointId());
            realTimePicPointDoneDTO.setTaskId(task.getId());
            realTimePicPointDoneDTO.setCarriage(task.getPosition().getCarriage());
            realTimePicPointDoneDTO.setAgvPosPercent(task.getPosition().getAgvPosPercent());
            realTimePicPointDoneDTO.setCmdSrc(task.getCmdSrc());
            realTimePicPointDoneDTO.setDir(task.getTraceFile());
            realTimePicPointDoneDTO.setPartInfo(null);
            realTimePicPointDoneDTO.setFinished(false);
            synchronized (robotMockService.donePicPointList) {
                robotMockService.donePicPointList.add(realTimePicPointDoneDTO);
            }
            /* end ============触发当前节点的完成============ */

            if(curPicPointIdx+1==picPointIdStrList.size()){ // 判断是否已经是最后一个拍照点了

                task.setStatus(TaskStatus.FINISHED.getCode());
                task.setAccumulatedTime(Long.valueOf(DateUtil.between(task.getTaskStartTime(), new Date(), DateUnit.MINUTE)).intValue());
                double v = (curPicPointIdx + 1.0) / picPointIdStrList.size();
                v=v*100;
                task.setPercent(Double.valueOf(v).intValue());
                FinishedState finishedState = new FinishedState(task);
                task.setTaskState(finishedState);
                return;
            }

            curPicPointIdx++;
            task.setCurPicPointIdx(curPicPointIdx);
            String picPointId = picPointIdStrList.get(curPicPointIdx);
            BotPicPosCfg botPicCfg=robotMockService.botPicPosCfgMap.get(picPointId);
            PicPosRelPointBO picPosRelPointBO = robotMockService.picMapLandmarkMap.get(picPointId);
            String beginPointName = picPosRelPointBO.getBeginPointName();
            String landMarkName = landMarkList.get(curLandMarkIdx);
            if (Objects.equals(beginPointName, landMarkName)) {
                log.info("地图路径点:{}相同,无需移动landMark序号",beginPointName);
            }else{
                log.info("地图路径点:{}移动到{}相同,",landMarkName,beginPointName);
                task.setCurLandMarkIdx(++curLandMarkIdx);
            }
            RealTimeDataDTO.Position position = new RealTimeDataDTO.Position();
            position.setPrePassPointIdx(curLandMarkIdx);
            Integer carriageCode = botPicCfg.getCarriageCode();
            position.setCarriage(carriageCode);
            position.setPicPointId(picPointId);
            position.setAgvPosPercent(curPicPointIdx %10 *10);
            task.setPosition(position);
            task.setAccumulatedTime(Long.valueOf(DateUtil.between(task.getTaskStartTime(), new Date(), DateUnit.MINUTE)).intValue());
            double v = (task.getCurPicPointIdx() + 1.0) / task.getPicPointIdStr().size();
            v=v*100;
            task.setPercent(Double.valueOf(v).intValue());
        }
    }
}
