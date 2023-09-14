package cn.nannar.robotmock.fx.controller;

import cn.nannar.robotmock.fx.bo.PointBO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.activemq.command.ActiveMQTopic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import javax.jms.Topic;

/**
 * @author LTJ
 * @date 2023/8/31
 */
@RestController
@Slf4j
public class RobotController {
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private JmsTemplate jmsTemplate;

    private Topic cmdTopic = new ActiveMQTopic("topic.foo");

    @RequestMapping("/hello")
    public PointBO hello(){
        jmsTemplate.convertAndSend(cmdTopic, "from hello controller");
        return new PointBO();
    }

    /**
     * 非持久型的订阅者
     *
     * @param msg
     * @throws InterruptedException
     */
    @JmsListener(containerFactory = "jmsTopicListenerContainerFactory", destination = "topic.foo")
    public void onCmd(String msg) throws InterruptedException {
        log.info("接收到消息：{}", msg);

    }
}
