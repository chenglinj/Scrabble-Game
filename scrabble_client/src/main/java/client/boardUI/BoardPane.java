package client.boardUI;


import core.game.Board;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.event.EventHandler;
import javafx.geometry.Dimension2D;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.text.Font;

import java.awt.*;
import java.util.Observable;
import java.util.Observer;

// TODO: Credit to: https://stackoverflow.com/a/31761362 for the code
public class BoardPane extends AnchorPane {
    private final ScrabbleCanvas canvas;
    private final ScrabbleInput letterType;

    private Board board;

    public BoardPane() {
        this(new Board(1, 1));
    }

    public void setBoard(Board board) {
        this.board = board;

        board.addObserver(new Observer() {
            @Override
            public void update(Observable o, Object arg) {
                if (board.isEmpty((Point)arg)) {
                    canvas.chosenCellProperty.set(null);
                    canvas.enabledProperty.set(true);
                }

                System.out.println("LETTER:\t" + board.get((Point)arg));
            }
        });

        canvas.setBoard(board);
        System.out.println("Board set: " + board.countObservers());
    }

    public BooleanProperty enabledProperty() {
        return canvas.enabledProperty;
    }

    public ObjectProperty<Point> chosenCellProperty() { return canvas.chosenCellProperty; }

    public Point getSelectedCell() { return canvas.getSelectedCell(); }

    public BoardPane(Board board) {
        super();

        this.board = board;

        canvas = new ScrabbleCanvas(board);
        letterType = new ScrabbleInput();
        letterType.setVisible(false);

        prepareTextField();
        prepareCanvas();

        // ensure pane uses up all available space in parent container
        AnchorPane.setBottomAnchor(canvas, 0d);
        AnchorPane.setTopAnchor(canvas, 0d);
        AnchorPane.setLeftAnchor(canvas, 0d);
        AnchorPane.setRightAnchor(canvas, 0d);

        getChildren().add(canvas);
        getChildren().add(letterType);
    }

    private void prepareCanvas() {
        canvas.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2)
                openTextField();
        });

        canvas.addEventHandler(MouseEvent.MOUSE_PRESSED, new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                if (letterType.isVisible()) {
                    board.set(canvas.getSelectedCell(), letterType.getLetter());
                    canvas.requestFocus();
                }

                letterType.setVisible(false);
            }
        });

        canvas.addEventHandler(KeyEvent.KEY_PRESSED, new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent event) {
                switch (event.getCode()) {
                    case LEFT:
                    case RIGHT:
                    case UP:
                    case DOWN:
                        break;
                    default:
                        if (event.getText().length() == 1 &&
                                Character.isLetter(event.getText().charAt(0)) ||
                                event.getCode() == KeyCode.ENTER)
                            openTextField();
                        break;
                }

            }
        });

        canvas.widthProperty().addListener(o -> {
            canvas.repaint();
            positionTextField();
            letterType.autosize();
        });
        canvas.heightProperty().addListener(o -> {
            canvas.repaint();
            positionTextField();
            letterType.autosize();
        } );

        canvas.enabledProperty.addListener(o -> {
            if (!canvas.enabledProperty.get())
                letterType.setVisible(false);
        });
    }

    private void prepareTextField() {
        letterType.addEventHandler(KeyEvent.KEY_RELEASED, new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent event) {
                if (letterType.getLetter() != 0) {
                    board.set(canvas.getSelectedCell(), letterType.getLetter());
                    canvas.chosenCellProperty.set(canvas.getSelectedCell());
                    canvas.enabledProperty.set(!canvas.enabledProperty.get());

                    letterType.setVisible(false);
                    canvas.fireEvent(event);
                }


                /**
                switch (event.getCode()) {
                    /**
                    case ESCAPE:
                        canvas.requestFocus();
                        letterType.setVisible(false);
                        break;
                    case ENTER:
                        board.set(canvas.getSelectedCell(), letterType.getLetter());
                        canvas.chosenCellProperty.set(canvas.getSelectedCell());
                        letterType.setVisible(false);

                        canvas.requestFocus();
                        canvas.enabledProperty.set(!canvas.enabledProperty.get());
                    case UP:
                    case DOWN:
                    case LEFT:
                    case RIGHT:
                        canvas.fireEvent(event);
                        break;

                    default:
                }
                 **/
            }
        });
    }

    private void positionTextField() {
        if (canvas.getSelectedCell() == null)
            return;

        //System.out.println("NEW SIZE: " + getWidth() + " " + getHeight());
        Point2D selected_xy = canvas.toCell(canvas.getSelectedCell());
        Dimension2D cell_size = canvas.getCellSize();
        // move textbox to cell
        letterType.setLayoutX(selected_xy.getX());
        letterType.setLayoutY(selected_xy.getY());

        letterType.setPrefSize(cell_size.getWidth(), cell_size.getHeight());

        letterType.setPadding(new Insets(-0.05 * cell_size.getHeight(), 0,0, 0));

        Font f = canvas.getLetterFont();
        letterType.setFont(new Font(f.getName(), f.getSize()/1.2));
    }

    private void openTextField() {
        if (canvas.getSelectedCell() == null || !canvas.enabledProperty.get() ||
            !board.isEmpty(canvas.getSelectedCell()))
            return;

        positionTextField();

        letterType.setText(Character.toString(
                canvas.getBoard().get(
                        canvas.getSelectedCell())));
        letterType.setVisible(true);
        letterType.requestFocus();
    }
}