package cn.nannar.robotmock.fx.controller;

import cn.hutool.core.io.FileUtil;
import cn.nannar.robotmock.fx.event.ProgressEvent;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventTarget;
import javafx.event.EventType;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;

/**
 * @author LTJ
 * @date 2023/4/21
 */
@Controller
@Slf4j
public class MainController implements Initializable {
    private FileChooser fileChooser;
    private Alert alert;
    private ProgressIndicator progressIndicator = new ProgressIndicator();

    @FXML
    private TextField textField;
    @FXML
    private Canvas canvas;
    @FXML
    private StackPane mainPane;

    @Override

    public void initialize(URL location, ResourceBundle resources) {
        if (location != null) {
            String path = location.getPath();
            log.info("path:{}", path);
        }
        fileChooser = new FileChooser();
        FileChooser.ExtensionFilter extensionFilter = new FileChooser.ExtensionFilter("机器人地图文件", "*.xmap");
        FileChooser.ExtensionFilter extensionFilter2 = new FileChooser.ExtensionFilter("图片文件", "*.png");
        fileChooser.getExtensionFilters().addAll(extensionFilter, extensionFilter2);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(Color.GREY);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        alert = new Alert(Alert.AlertType.WARNING, "请选择文件夹", ButtonType.OK);

        mainPane.addEventHandler(ProgressEvent.LOADING,event -> {
            mainPane.getChildren().add(progressIndicator);
        });

        mainPane.addEventHandler(ProgressEvent.LOADED,event -> {
            mainPane.getChildren().remove(progressIndicator);
        });
    }

    @FXML
    private void OnSelectFile(ActionEvent event) {
        fileChooser.setTitle("选择地图xml文件");
        Node target = (Node) event.getTarget();
        Window window = target.getScene().getWindow();

        File file = fileChooser.showOpenDialog(window);
        if (file != null) {
            textField.setText(FileUtil.getAbsolutePath(file));
            ProgressEvent progressEvent = new ProgressEvent(ProgressEvent.LOADING);
            target.fireEvent(progressEvent);
        }
        Task<Boolean> task = new Task<Boolean>() {
            @Override
            protected Boolean call() throws Exception {
                log.info("正在执行解析...");
                Thread.sleep(2000);
                log.info("解析完成");
                return true;
            }
        };
        task.setOnSucceeded(workerStateEvent->{
            ProgressEvent progressEvent = new ProgressEvent(ProgressEvent.LOADED);
            Event.fireEvent(mainPane,progressEvent);
        });

        CompletableFuture.runAsync(task);

    }

    @FXML
    private void OnSnapshot(ActionEvent event) {

        WritableImage snapshot = canvas.snapshot(null, null);
        BufferedImage bufferedImage = SwingFXUtils.fromFXImage(snapshot, null);

        fileChooser.setTitle("保存图片");
        Window window = ((Node) event.getTarget()).getScene().getWindow();
        File file = fileChooser.showSaveDialog(window);
        if (file != null) {
            try {
                ImageIO.write(bufferedImage, "png", file);
            } catch (IOException e) {
                alert.setContentText("保存失败！");
                alert.show();
            }
        } else {
            alert.setContentText("请选择保存的位置！");
            alert.show();
        }

    }

}
