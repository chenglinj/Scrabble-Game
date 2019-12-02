package client.util;

import javafx.scene.control.Dialog;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.net.URL;

public class StageUtils {
    // annoyingly JavaFX doesn't provide a method to center a window on another window
    // TODO: Thanks to https://stackoverflow.com/a/13702636 for the code
    public static void centreStage(Stage parent, Stage child) {
        child.setX(parent.getX() + parent.getWidth() / 2 - child.getWidth() / 2);
        child.setY(parent.getY() + parent.getHeight() / 2 - child.getHeight() / 2);
    }

    public static void dialogCenter(Stage parent, Dialog dialog) {
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(parent);
    }
}
