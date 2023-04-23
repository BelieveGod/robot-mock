package cn.nannar.robotmock.util;

import javafx.fxml.FXMLLoader;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * @author LTJ
 * @date 2023/1/14
 */
public class SpringFxmlLoaderFactory {
    private static ConfigurableApplicationContext ctx;

    public static void setCtx(ConfigurableApplicationContext applicationContext){
        ctx=applicationContext;
    }

    public static FXMLLoader getFxmlLoader(String resouce){
        FXMLLoader fxmlLoader = new FXMLLoader(SpringFxmlLoaderFactory.class.getResource(resouce));
        fxmlLoader.setControllerFactory(ctx::getBean);
        return fxmlLoader;
    }
}
