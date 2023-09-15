package cn.nannar.robotmock.fx.tasksatus;

import cn.nannar.robotmock.entity.BotAlarmLog;
import cn.nannar.robotmock.entity.BotInspectLog;
import cn.nannar.robotmock.fx.constant.TaskStatus;
import cn.nannar.robotmock.fx.dao.BotAlarmLogMapper;
import cn.nannar.robotmock.fx.dao.BotInspectLogMapper;
import cn.nannar.robotmock.fx.dto.RealTimeDataDTO;
import cn.nannar.robotmock.fx.dto.RealTimePicPointDoneDTO;
import cn.nannar.robotmock.fx.service.RobotMockService;
import cn.nannar.robotmock.fx.util.SpringContextHolder;

/**
 * @author LTJ
 * @date 2023/9/15
 */
public class FinishedState implements TaskState{
    private RealTimeDataDTO.Task  task;

    public FinishedState(RealTimeDataDTO.Task task) {
        this.task = task;
    }
    @Override
    public void doAction() {
        /* begin ==========触发当前节点的完成============= */
        RealTimePicPointDoneDTO realTimePicPointDoneDTO = new RealTimePicPointDoneDTO();
        realTimePicPointDoneDTO.setPicPointId(null);
        realTimePicPointDoneDTO.setTaskId(task.getId());
        realTimePicPointDoneDTO.setCarriage(null);
        realTimePicPointDoneDTO.setAgvPosPercent(null);
        realTimePicPointDoneDTO.setCmdSrc(task.getCmdSrc());
        realTimePicPointDoneDTO.setDir(task.getTraceFile());
        realTimePicPointDoneDTO.setPartInfo(null);
        realTimePicPointDoneDTO.setFinished(true);
        RobotMockService robotMockService = SpringContextHolder.getBean(RobotMockService.class);
        synchronized (robotMockService.donePicPointList){
            robotMockService.donePicPointList.add(realTimePicPointDoneDTO);
        }
        /* end ============触发当前节点的完成============ */
        synchronized (robotMockService.taskList){
            robotMockService.taskList.remove(task);
        }
        BotInspectLogMapper botInspectLogMapper = SpringContextHolder.getBean(BotInspectLogMapper.class);
        BotInspectLog botInspectLog = botInspectLogMapper.selectById(task.getId());
        botInspectLog.setTranceStatus(100);
        botInspectLog.setTaskStatus(TaskStatus.FINISHED.getCode());
        botInspectLogMapper.updateById(botInspectLog);
    }
}
