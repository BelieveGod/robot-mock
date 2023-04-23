package cn.nannar.robotmock.fx;

import cn.nannar.robotmock.RobotMockApplication;
import cn.nannar.robotmock.util.SpringFxmlLoaderFactory;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * @author LTJ
 * @date 2023/4/21
 */
public class MapPaint extends Application {

    public static void main(String[] args) {
        Application.launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader fxmlLoader = SpringFxmlLoaderFactory.getFxmlLoader("/fxml/Main.fxml");
        StackPane borderPane = fxmlLoader.load();
        Scene scene = new Scene(borderPane);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    @Override
    public void init() throws Exception {
        SpringApplication springApplication = new SpringApplication(RobotMockApplication.class);
        ConfigurableApplicationContext context = springApplication.run();
        SpringFxmlLoaderFactory.setCtx(context);
    }

    @Override
    public void stop() throws Exception {
        super.stop();
    }
}
