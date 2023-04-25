package cn.nannar.robotmock.fx.controller;

import cn.hutool.core.io.FileUtil;
import cn.nannar.robotmock.fx.bo.*;
import cn.nannar.robotmock.fx.event.ProgressEvent;
import cn.nannar.robotmock.fx.service.MapService;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.transform.NonInvertibleTransformException;
import javafx.scene.transform.Scale;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
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
    @FXML
    private ScrollPane scrollPane;

    private double zoomCount=0;
    private Scale scale;
    private double canvasPressedX;
    private double canvasPressedY;
    private double canvasTranslateX;
    private double canvasTranslateY;

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
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        alert = new Alert(Alert.AlertType.WARNING, "请选择文件夹", ButtonType.OK);

        mainPane.addEventHandler(ProgressEvent.LOADING,event -> {
            mainPane.getChildren().add(progressIndicator);
        });

        mainPane.addEventHandler(ProgressEvent.LOADED,event -> {
            mainPane.getChildren().remove(progressIndicator);
        });

        mainPane.setOnScroll(scrollEvent->{
            double deltaY = scrollEvent.getDeltaY();
            double x = scrollEvent.getX();
            double y = scrollEvent.getY();
            if(deltaY>0){
                zoomCount++;
            }else{
                zoomCount--;
            }
            Bounds boundsInParent = canvas.getBoundsInParent();
            printBound(boundsInParent);
            double minX = boundsInParent.getMinX();
            double minY = boundsInParent.getMinY();

            if(scale!=null){
                scale.setX(1+0.1*zoomCount);
                scale.setY(1+0.1*zoomCount);
                scale.setPivotX(x);
                scale.setPivotY(y);
                    try {
                        Point2D point2D = scale.inverseTransform(minX, minY);
                        log.info("1 reversed x:{}   ,y:{}  ",point2D.getX(),point2D.getY());
                        Point2D point2D1 = scale.transform(minX, minY);
                        log.info("1 delta x:{}   ,y:{}  ",point2D1.getX(),point2D1.getY());
                    } catch (NonInvertibleTransformException e) {
                        e.printStackTrace();
                    }
            }else{
                scale = new Scale(1+0.1*zoomCount, 1+0.1*zoomCount,x, y);
                if(scale!=null){
                    try {
                        Point2D point2D = scale.inverseTransform(minX, minY);
                        log.info("1 reversed x:{}   ,y:{}  ",point2D.getX(),point2D.getY());
                        Point2D point2D1 = scale.transform(minX, minY);
                        log.info("1 delta x:{}   ,y:{}  ",point2D1.getX(),point2D1.getY());
                    } catch (NonInvertibleTransformException e) {
                        e.printStackTrace();
                    }
                }
                canvas.getTransforms().add(scale);
            }
             boundsInParent = canvas.getBoundsInParent();
            double layoutX2 = canvas.getLayoutX();
            double layoutY2 = canvas.getLayoutY();
            printBound(boundsInParent);
            try {
                Point2D point2D = scale.inverseTransform(boundsInParent.getMinX(), boundsInParent.getMinY());
                log.info(" 2 reversed x:{}   ,y:{}  ",point2D.getX(),point2D.getY());
                Point2D point2D1 = scale.transform(boundsInParent.getMinX(), boundsInParent.getMinY());
                log.info(" 2 delta x:{}   ,y:{}  ",point2D1.getX(),point2D1.getY());
            } catch (NonInvertibleTransformException e) {
                e.printStackTrace();
            }
        });

        canvas.setOnMousePressed(mouseEvent->{
            if (mouseEvent.getButton().equals(MouseButton.PRIMARY)) {
                canvas.setCursor(Cursor.MOVE);
                canvasPressedX = mouseEvent.getSceneX();
                canvasPressedY = mouseEvent.getSceneY();
                canvasTranslateX=canvas.getTranslateX();
                canvasTranslateY=canvas.getTranslateY();
            }
        });

        canvas.setOnMouseReleased(mouseEvent->{
            if (mouseEvent.getButton().equals(MouseButton.PRIMARY)) {
                canvas.setCursor(Cursor.DEFAULT);
                canvasPressedX = mouseEvent.getSceneX();
                canvasPressedY = mouseEvent.getSceneY();
            }
        });

        canvas.setOnMouseDragged(mouseEvent->{
            if (canvas.getCursor().equals(Cursor.MOVE)) {
                double sceneX = mouseEvent.getSceneX();
                double sceneY = mouseEvent.getSceneY();
                double deltaX = sceneX - canvasPressedX;
                double deltaY = sceneY - canvasPressedY;
//                log.info("deltaX:{}", deltaX);
//                log.info("deltaY:{}", deltaY);
//                log.info("translateX:{}", canvas.getTranslateX());
//                log.info("translateY:{}", canvas.getTranslateY());
                canvas.setTranslateX(canvasTranslateX + deltaX);
                canvas.setTranslateY(canvasTranslateY + deltaY);
            }
        });

        canvas.setOnMouseClicked(mouseEvent->{
            log.info("点集坐标：（{}，{}）", mouseEvent.getX(), mouseEvent.getY());
        });
    }

    private void printBound(Bounds bounds){
        double minX = bounds.getMinX();
        double minY = bounds.getMinY();
        double width = bounds.getWidth();
        double height = bounds.getHeight();
        log.info("x:{},y:{},width:{},height:{}", minX, minY, width, height);
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
                MapBO mapBO = MapService.parseMapXml(file);
                log.info("解析完成");
                Platform.runLater(()->{
                    paint(mapBO);
                });
                return true;
            }
        };
        task.setOnSucceeded(workerStateEvent->{
            ProgressEvent progressEvent = new ProgressEvent(ProgressEvent.LOADED);
            Event.fireEvent(mainPane,progressEvent);
        });
        task.setOnFailed(workerStateEvent->{
            ProgressEvent progressEvent = new ProgressEvent(ProgressEvent.LOADED);
            Throwable exception = workerStateEvent.getSource().getException();
            log.error("解析异常",exception);
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

    private void paint(MapBO mapBO){
        GraphicsContext gc2d = canvas.getGraphicsContext2D();
        gc2d.setFill(Color.WHITE);
        gc2d.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        double width = canvas.getWidth();
        double height = canvas.getHeight();
        double mapWidth = mapBO.getMaxPosX() - mapBO.getMinPosX();
        double mapheight = mapBO.getMaxPosY() - mapBO.getMinPosY();

        double widthRatio = width / mapWidth;
        double heightRatio = height / mapheight;
        // 取比例较小的那一侧才能都满足
        double ruler = Math.min(widthRatio, heightRatio);

        List<PointBO> pointBOList = mapBO.getPointBOList();
        gc2d.setFill(Color.GREY);
        for (PointBO pointBO : pointBOList) {
            double x = pointBO.getX();
            double y = pointBO.getY();
            double xOfCanvas = convertCor(x, mapBO.getMinPosX(), ruler);
            double yOfCanvas =height- convertCor(y, mapBO.getMinPosY(), ruler);
            gc2d.fillOval(xOfCanvas - 1, yOfCanvas - 1, 2, 2);
        }

        List<LandMarkBO> landMarkBOList = mapBO.getLandMarkBOList();
        gc2d.setFill(Color.YELLOW);
        double radius = 2.5;
        double diametr = radius*2;
        for (LandMarkBO landMarkBO : landMarkBOList) {
            Double x = landMarkBO.getX();
            Double y = landMarkBO.getY();
            double xOfCanvas = convertCor(x, mapBO.getMinPosX(), ruler);
            double yOfCanvas =height- convertCor(y, mapBO.getMinPosY(), ruler);
            gc2d.fillOval(xOfCanvas - radius, yOfCanvas - radius, diametr, diametr);
        }

        List<ReflectCylinderBO> reflectCylinderBOList = mapBO.getReflectCylinderBOList();
        gc2d.setFill(Color.GREEN);
        for (ReflectCylinderBO reflectCylinderBO : reflectCylinderBOList) {
            Double x = reflectCylinderBO.getX();
            Double y = reflectCylinderBO.getY();
            double xOfCanvas = convertCor(x, mapBO.getMinPosX(), ruler);
            double yOfCanvas = height- convertCor(y, mapBO.getMinPosY(), ruler);
            gc2d.fillRect(xOfCanvas - radius, yOfCanvas - radius, diametr, diametr);
        }

        List<PathBO> pathBOList = mapBO.getPathBOList();
        Color[] colors = new Color[]{
                Color.RED, Color.ORANGE, Color.YELLOW, Color.GREEN,Color.CYAN,Color.BLUE,Color.PURPLE,Color.PINK
        };

        gc2d.setLineDashes(1);
        for(int i=0;i<pathBOList.size();i++){
            PathBO pathBO = pathBOList.get(i);
            LandMarkBO startPos = pathBO.getStartPos();
            LandMarkBO endPos = pathBO.getEndPos();
            double o1x = convertCor(startPos.getX(), mapBO.getMinPosX(), ruler);
            double o1y = height-convertCor(startPos.getY(), mapBO.getMinPosY(), ruler);

            double o2x = convertCor(endPos.getX(), mapBO.getMinPosX(), ruler);
            double o2y = height-convertCor(endPos.getY(), mapBO.getMinPosY(), ruler);

            double c1x = convertCor(pathBO.getControlX1(), mapBO.getMinPosX(), ruler);
            double c1y = height-convertCor(pathBO.getControlY1(), mapBO.getMinPosY(), ruler);
            double c2x = convertCor(pathBO.getControlX2(), mapBO.getMinPosX(), ruler);
            double c2y = height-convertCor(pathBO.getControlY2(), mapBO.getMinPosY(), ruler);
            gc2d.setStroke(colors[i%colors.length]);
            gc2d.beginPath();
            gc2d.moveTo(o1x,o1y);
            gc2d.bezierCurveTo(c1x, c1y, c2x, c2y, o2x, o2y);
            gc2d.stroke();
        }

    }

    private double convertCor(double x,double o,double ruler){
        double v = (x - o) * ruler;
        return v;
    }

}
