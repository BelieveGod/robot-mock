package cn.nannar.robotmock.fx.service;

import cn.hutool.core.date.DateUtil;
import cn.nannar.robotmock.fx.dto.RealTimeDataDTO;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author LTJ
 * @date 2023/9/12
 */
public class MockBattery {
    private Date startTime;

    private Double percent;

    private Boolean charging=false;
    /**
     * 每秒的输出率
     */
    private double outputRate=1.0 / 36;
    /**
     * 每秒的输入率
     */
    private double intputRate=1.0/9;

    private Integer status=0;
    private List<String> troubleCodeList = new LinkedList<>();

    public MockBattery(Date startTime) {
        this.startTime = startTime;
        this.percent=100d;
    }

    /**
     * 模拟的电池，返回当前电量百分比
     * @return
     */
    public int getCurrentPercent(){
        Date now = new Date();
        long l = DateUtil.betweenMs(startTime, now);
        long secondDiff = l / 1000;
        if(!charging){
            int currentPercent = Long.valueOf(Math.round(percent - outputRate * secondDiff)).intValue();
            if(currentPercent<0){
                currentPercent=0;
            }
            return currentPercent;
        }else{
            int currentPercent = Long.valueOf(Math.round(percent + outputRate * secondDiff)).intValue();
            if(currentPercent>100){
                currentPercent=100;
                setCharging(false);
            }
            return currentPercent;
        }
    }

    public int getRemainTime(){
        return Double.valueOf(Math.floor(getCurrentPercent() / outputRate / 60)).intValue();
    }

    public Integer getTimeToFull(){
        if(!charging){
            return null;
        }
        return Double.valueOf(Math.ceil((100-getCurrentPercent() ) / intputRate /60)).intValue();
    }

    public Double getChargingCurrent(){
        if(!charging){
            return null;
        }
        return 1.0;
    }

    public Double getVoltage(){
        return 14.0;
    }



    public Boolean getCharging(){
        return charging;
    }

    public void setCharging(Boolean charging){
        percent =  Integer.valueOf(getCurrentPercent()).doubleValue();
        startTime = new Date();
        this.charging = charging;
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

    public RealTimeDataDTO.Battery getBattery(){
        RealTimeDataDTO.Battery battery = new RealTimeDataDTO.Battery();
        battery.setStatus(status);
        battery.setTroubleCodeList(getTroubleCodeList());
        return battery;
    }
}
