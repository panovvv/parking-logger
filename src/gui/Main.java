package gui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.opencv.core.Core;

import java.awt.*;

public class Main extends Application {

    private Rectangle getScreenSize() {
        return GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("main_window.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root, getScreenSize().width, getScreenSize().height);
        primaryStage.setTitle("Parking logger");
        primaryStage.setScene(scene);
        Controller controller = loader.getController();
        primaryStage.setOnCloseRequest((we -> controller.close()));
        primaryStage.show();
    }


    public static void main(String[] args) {
        System.setProperty("java.util.logging.SimpleFormatter.format",
                "%1$tT %5$s%6$s%n");
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        launch(args);
    }
}
