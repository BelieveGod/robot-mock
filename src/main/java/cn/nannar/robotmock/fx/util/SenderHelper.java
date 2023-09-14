package cn.nannar.robotmock.fx.util;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 发送MQ 的帮助类
 * @author LTJ
 * @date 2022/3/17
 */
@Slf4j
public class SenderHelper {
    /**
     * 整个应用程序共用的发送序列号
     */
    private static Integer seq;

    private static AtomicInteger localTaskId;
    static {
        seq = 0;
        localTaskId = new AtomicInteger(1);
    }

    /**
     * 原子获取序列号
     * @return
     */
    public static synchronized Integer getSeqAndIncrement(){
        int i = seq % 65536;
        seq++;
        if(seq>65535){
            seq=0;
        }
        return i;
    }

    public static Integer generateLocalTaskId(){
        return localTaskId.getAndIncrement();
    }

    /**
     * 重试流程的模板方法
     * @param supplier
     * @param function
     * @param <T>
     * @return
     */
    public static <T> T retry(Supplier<T> supplier, Function<T, Boolean> function){
        // 重试次数
        int cnt=3;
        boolean flag=false;
        T t=null;
        do{
            cnt--;
            try {
                t = supplier.get();
                flag = function.apply(t);
            } catch (Exception e) {
                flag=false;
                log.error("发生异常，任务结果设为false",e);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException interruptedException) {
                    Thread.interrupted();
                }
            }catch (Error e){
                log.error("致命错误", e);
                throw e;
            }
        }while (!flag && cnt>0);

        return t;
    }
}
