package cn.nannar.robotmock.fx.service;

import cn.nannar.robotmock.fx.dto.RealTimeDataDTO;

import java.util.LinkedList;
import java.util.List;

/**
 * @author LTJ
 * @date 2023/9/12
 */
public class MockAgv {

    private Integer status=0;
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

    public RealTimeDataDTO.Agv getAgv(){
        RealTimeDataDTO.Agv agv = new RealTimeDataDTO.Agv();
        agv.setStatus(status);
        agv.setTroubleCodeList(getTroubleCodeList());
        return agv;
    }
}
