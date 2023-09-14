package cn.nannar.robotmock.fx.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 用于与C++ 对接的 通用帧结构
 * @author LTJ
 * @date 2022/3/15
 */
@Data
public class RdpsFrame<T> {
    private String msgType;
    private Integer seq;

    private String publisher;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date publishTime;
    private T data;
}
