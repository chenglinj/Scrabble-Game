package client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import client.controller.LoginFormController;

public class ClientMain extends Application {
    private static boolean appEnded = false;

    public static void main(String[] args) {
        launch(args);
    }

    public static void endApp(String msg) {
        boolean prevValue = appEnded;
        appEnded = true;

        if (!prevValue) {
            new Alert(Alert.AlertType.WARNING, msg).showAndWait();
            System.exit(0);
        }
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(this.getClass().getResource("/LoginForm.fxml"));
        loader.setController(new LoginFormController(primaryStage));

        Scene scene = new Scene(loader.load());
        primaryStage.setScene(scene);
        primaryStage.setTitle("Scrabble Login");
        primaryStage.setResizable(false);

        primaryStage.show();
    }
}
