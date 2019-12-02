package client.controller;

import client.ClientMain;
import client.Connections;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.InlineCssTextArea;

import java.net.URL;
import java.util.ResourceBundle;

public class ChatBoxController implements Initializable {
    @FXML
    protected Button btnSend;
    @FXML
    protected InlineCssTextArea rtChat;
    @FXML
    protected TextArea txtInput;
    @FXML
    private VirtualizedScrollPane rtScroll;

    public void appendText(String txt, Color c) {
        Platform.runLater(() -> {
            int prevPos = rtChat.getLength();
            rtChat.appendText(txt);

            // TODO: Thanks to https://stackoverflow.com/a/3607942 for the hint
            String hex =  String.format("#%02x%02x%02x",
                    (int)(c.getRed() * 255),
                    (int)(c.getGreen() * 255),
                    (int)(c.getBlue() * 255));

            rtChat.setStyle(prevPos, rtChat.getLength(),
                    String.format("-fx-fill: %s;", hex));
        });
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // pressing ENTER key sends the chat msg, SHIFT+ENTER creates a new line
        txtInput.addEventHandler(KeyEvent.KEY_PRESSED, new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent event) {
                if (event.getCode() == KeyCode.ENTER) {
                    if (event.isShiftDown())
                        txtInput.appendText("\n");
                    else {
                        event.consume(); // prevents new line after pressing ENTER
                        btnSend.fireEvent(new ActionEvent());
                    }
                }
            }
        });

        btnSend.setOnAction(e -> {
            if (!btnSend.disabledProperty().get()) {
                Connections.getListener().sendChatMessage(txtInput.getText());
                txtInput.setText("");
            }
        });

        // scroll to the bottom end of chatbox, if new text arrives
        rtChat.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                rtScroll.estimatedScrollYProperty().setValue(Double.MAX_VALUE);
            }
        });

        txtInput.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                btnSend.setDisable(txtInput.textProperty()
                        .get()
                        .replace("\n", "")
                        .isEmpty());
            }
        });

        btnSend.setDisable(true);
    }
}
