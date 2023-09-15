package cn.nannar.robotmock.fx.tasksatus;

import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.nannar.robotmock.entity.BotPicPosCfg;
import cn.nannar.robotmock.fx.constant.TaskStatus;
import cn.nannar.robotmock.fx.dto.RealTimeDataDTO;
import cn.nannar.robotmock.fx.service.RobotMockService;
import cn.nannar.robotmock.fx.util.SpringContextHolder;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author LTJ
 * @date 2023/9/15
 */
public class WaittingState implements TaskState{
    private RealTimeDataDTO.Task  task;

    public WaittingState(RealTimeDataDTO.Task task) {
        this.task = task;
    }

    @Override
    public void doAction() {
        synchronized (task){
            List<String> picPointIdStrList = task.getPicPointIdStr();
            List<String> landMarkList = task.getLandMarkList();

            int curPicPointIdx =0;
            int curLandMarkIdx=0;
            task.setCurPicPointIdx(curPicPointIdx);
            String picPointId = picPointIdStrList.get(curPicPointIdx);
            RobotMockService robotMockService = SpringContextHolder.getBean(RobotMockService.class);
            BotPicPosCfg botPicCfg=robotMockService.botPicPosCfgMap.get(picPointId);
            task.setStatus(TaskStatus.WORKING.getCode());
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
            WorkingState workingState = new WorkingState(task);
            task.setTaskState(workingState);
        }

    }
}
