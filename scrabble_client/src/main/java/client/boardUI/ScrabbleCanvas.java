package client.boardUI;

import core.game.Board;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Dimension2D;
import javafx.geometry.Point2D;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import java.awt.*;

public class ScrabbleCanvas extends Pane {
    private final Color SELECTED_COLOR  = Color.LIGHTBLUE;
    private final Color NORMAL_COLOR = Color.WHITE;
    private final Color DISABLED_COLOR = Color.LIGHTGREY;
    private final Color PARTCHOSEN_COLOR = Color.YELLOW;
    private final Color CHOSEN_COLOR = Color.GREENYELLOW;

    private Board board;

    private Dimension2D cell_size;
    private Color current_color = NORMAL_COLOR;
    private Point selected_cell;
    private final double line_width = 0.6;

    private Canvas layer1_Lines;
    private Canvas layer2_Colors;
    private Canvas layer3_Letters;
    private Canvas[] canvases;

    // TODO: The cell that was typed in.
    public ObjectProperty<Point> chosenCellProperty;
    public BooleanProperty enabledProperty;

    public ScrabbleCanvas(Board b) {
        super();
        this.board = b;

        layer1_Lines = new Canvas();
        layer2_Colors = new Canvas();
        layer3_Letters = new Canvas();

        canvases = new Canvas[] {layer1_Lines, layer2_Colors, layer3_Letters};

        // prepare canvas (each canvas should fill entire pane)
        for (int i = 0; i < canvases.length; i++) {
            canvases[i].widthProperty().bind(this.widthProperty());
            canvases[i].heightProperty().bind(this.heightProperty());

            canvases[i].setMouseTransparent(true);

            getChildren().add(canvases[i]);
        }

        // BOOLEAN PROPERTIES

        enabledProperty = new SimpleBooleanProperty(true);
        enabledProperty.addListener(o -> {
            if (enabledProperty.get() == false)
                selected_cell = null;

            current_color = enabledProperty.get() ? NORMAL_COLOR : DISABLED_COLOR;
            deepPaint();
        });

        chosenCellProperty = new SimpleObjectProperty<>(null);
        chosenCellProperty.addListener(o -> deepPaint());

        // END BOOLEAN PROPERTIES

        deepPaint();

        this.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> {
            if (enabledProperty.get()) {
                shallowPaint(getCellHovering(e));
                requestFocus();
            }
        });

        this.addEventHandler(KeyEvent.KEY_PRESSED, e -> {
            Point new_selected_cell = null;

            switch (e.getCode()) {
                case LEFT:
                    if (selected_cell.getX() > 0)
                        new_selected_cell = new Point(selected_cell.x - 1, selected_cell.y);
                    break;
                case RIGHT:
                    if (selected_cell.getX() < board.getNumRows() - 1)
                        new_selected_cell = new Point(selected_cell.x + 1, selected_cell.y);
                    break;
                case DOWN:
                    if (selected_cell.getY() < board.getNumRows() - 1)
                        new_selected_cell = new Point(selected_cell.x, selected_cell.y + 1);
                    break;
                case UP:
                    if (selected_cell.getY() > 0)
                        new_selected_cell = new Point(selected_cell.x, selected_cell.y - 1);
                    break;
                default:
                    return;
            }

            if (new_selected_cell != null) {
                shallowPaint(new_selected_cell);
            }

            this.requestFocus();
        });
    }

    public Board getBoard() {
        return board;
    }

    public void setBoard(Board board) {
        this.board = board;
        deepPaint();
    }

    public Dimension2D getCellSize() {
        return cell_size;
    }

    private Point getCellHovering(MouseEvent e) {
        int row =(int)(e.getX()/cell_size.getWidth());
        int col = (int)(e.getY()/cell_size.getHeight());

        return new Point(row, col);
    }

    public Point2D toCell(Point coordinate) {
        double x = coordinate.getX() * cell_size.getWidth();
        double y = coordinate.getY() * cell_size.getHeight();

        return new Point2D(x, y);
    }

    public Point getSelectedCell() {
        return selected_cell;
    }

    public Font getLetterFont() {
        double size = Math.min(cell_size.getHeight(), cell_size.getWidth());
        return new javafx.scene.text.Font("Arial", (int)(size/1.1));
    }

    private Dimension2D measureText(Font f, String text) {
        javafx.scene.text.Text theText = new javafx.scene.text.Text(text);
        theText.setFont(f);

        return new Dimension2D(theText.getBoundsInLocal().getWidth(),
                theText.getBoundsInLocal().getHeight());
    }

    private double snap(double val) {
        // TODO: Refer to https://stackoverflow.com/a/27847190 .
        // This allows sharper lines by drawing it in 'middle of pixel'
        return (int)val + 0.5;
    }

    private void drawCell(Point cell, Color color) {
        if (cell == null)
            return;

        Point2D cell_xy = toCell(cell);

        // fill it clear first
        GraphicsContext c = layer2_Colors.getGraphicsContext2D();

        c.setFill(color);
        c.fillRect(snap(cell_xy.getX()) + line_width * 1.6,
                snap(cell_xy.getY()) + line_width * 1.6,
                snap(cell_size.getWidth()) - line_width * 3.2,
                snap(cell_size.getHeight()) - line_width * 3.2);


        drawLetter(cell);
    }

    private void shallowPaint(Point new_selected_cell) {
        drawCell(selected_cell, NORMAL_COLOR);
        drawCell(new_selected_cell, SELECTED_COLOR);
        highlightChosen();

        selected_cell = new_selected_cell;
    }

    public void repaint() {
        deepPaint();
    }

    private void deepPaint() {
        // adjust cell sizes due to change in window size
        cell_size = new Dimension2D(getWidth() / board.getNumRows(), getHeight() / board.getNumColumns());

        for (Canvas layer : canvases) {
            GraphicsContext c = layer.getGraphicsContext2D();
            c.clearRect(0, 0, getWidth(), getHeight());
        }

        // set bottom most layer to be white
        layer1_Lines.getGraphicsContext2D().setFill(current_color);
        layer1_Lines.getGraphicsContext2D().fillRect(0, 0, getWidth(), getHeight());

        // for the words horizontally/vertically part of the 'chosen cell'
        highlightChosen();

        // for the selected one
        drawCell(selected_cell, SELECTED_COLOR);

        // draw letters
        // TODO: Better implementation
        for (int x = 0; x < board.getNumRows(); x++) {
            for (int y = 0; y < board.getNumColumns(); y++) {
                drawLetter(new Point(x, y));
            }
        }

        drawGrid();
    }

    private void highlightChosen() {
        if (chosenCellProperty.get() == null)
            return;

        drawCell(chosenCellProperty.get(), CHOSEN_COLOR);

        // TODO: Make this code better (prototyped for now)
        for (int dir : new int[] {1, -1}) {
            // horizontal
            for (int x = chosenCellProperty.get().x + dir; x < board.getNumRows() && x >= 0 ; x += dir) {
                Point p = new Point(x, chosenCellProperty.get().y);

                if (board.isEmpty(p))
                    break;

                drawCell(p, PARTCHOSEN_COLOR);
            }

            // vertical
            for (int y = chosenCellProperty.get().y + dir; y < board.getNumColumns() && y >= 0 ; y += dir) {
                Point p = new Point(chosenCellProperty.get().x, y);

                if (board.isEmpty(p))
                    break;

                drawCell(p, PARTCHOSEN_COLOR);
            }
        }
    }

    private void drawLetter(Point cell) {
        if (board.isEmpty(cell.x, cell.y))
            return;

        GraphicsContext c = layer3_Letters.getGraphicsContext2D();
        Point2D cell_xy = toCell(cell);

        // clear letter
        c.clearRect(snap(cell_xy.getX()) + line_width * 1.6,
                snap(cell_xy.getY()) + line_width * 1.6,
                snap(cell_size.getWidth()) - line_width * 3.2,
                snap(cell_size.getHeight()) - line_width * 3.2);

        c.setFill(Color.BLACK);
        c.setFont(getLetterFont());

        Dimension2D char_size = measureText(c.getFont(),
                Character.toString(board.get(cell)));

        double x = cell_xy.getX() + (cell_size.getWidth() - char_size.getWidth())/2.0;
        double y = cell_xy.getY() + cell_size.getHeight()/2.0 + char_size.getHeight()/3;

        c.fillText(Character.toString(board.get(cell)), x, y);
    }

    private void drawGrid() {
        GraphicsContext c = layer1_Lines.getGraphicsContext2D();

        for (int x = 0; x < board.getNumRows(); x++) {
            c.strokeLine(snap(x * cell_size.getWidth()), 0,
                    snap(x * cell_size.getWidth()), getHeight());
        }

        for (int y = 0; y < board.getNumColumns(); y++) {
            c.strokeLine(0, snap(y * cell_size.getHeight()),
                    getWidth(), snap(y * cell_size.getHeight()));
        }

        // faces of the table need bolder lines
        c.setLineWidth(line_width * 2);
        c.strokeLine(0, 0,0, getHeight());
        c.strokeLine(getWidth(), 0, getWidth(), getHeight());
        c.strokeLine(0, 0, getWidth(), 0);
        c.strokeLine(0, getHeight(), getWidth(), getHeight());
    }
}
