package client.boardUI;

import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.input.KeyEvent;

/***
 * A JavaFX component which only allows one letter text input.
 */
public class ScrabbleInput extends TextField {
    public ScrabbleInput() {
        super();

        setAlignment(Pos.CENTER);

        // only allow one letter
        this.setTextFormatter(
                new TextFormatter<String>((TextFormatter.Change change) -> {
                    String newText = change.getControlNewText();

                    if (newText.length() > 1)
                        return null;
                    else if (change.getControlText().length() == 0 &&
                            change.isAdded() && Character.isLetter(newText.charAt(0))) {
                        change.setText(newText.toUpperCase());
                    }
                    else
                        change.setText("");

                    return change;
                })
        );
    }

    public char getLetter() {
        return getText().isEmpty() ? (char)0 : getText().charAt(0);
    }
}
