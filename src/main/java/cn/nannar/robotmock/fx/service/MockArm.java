package cn.nannar.robotmock.fx.service;

import cn.nannar.robotmock.fx.dto.RealTimeDataDTO;

import java.util.LinkedList;
import java.util.List;

/**
 * @author LTJ
 * @date 2023/9/12
 */
public class MockArm {
    private Integer status=0;
    private Integer moveStatus=1;
    private List<String> troubleCodeList = new LinkedList<>();


    public Integer getStatus() {
        return status;
    }

    public List<RealTimeDataDTO.TroubleCode> getTroubleCodeList() {
        List<RealTimeDataDTO.TroubleCode> list = new LinkedList<>();
        for (String s : troubleCodeList) {
            RealTimeDataDTO.TroubleCode troubleCode = new RealTimeDataDTO.TroubleCode();
            troubleCode.setTroubleCode(s);
            list.add(troubleCode);
        }
        return list;
    }

    public RealTimeDataDTO.Arm getArm(){
        RealTimeDataDTO.Arm arm = new RealTimeDataDTO.Arm();
        arm.setStatus(status);
        arm.setMoveStatus(moveStatus);
        arm.setTroubleCodeList(getTroubleCodeList());
        return arm;
    }
}
