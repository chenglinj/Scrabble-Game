package core.game;

import sun.reflect.annotation.ExceptionProxy;

import java.awt.*;
import java.util.Observable;

public class Board extends Observable {
    private transient char[][] config;

    private final int rows;
    private final int cols;

    public Board(int rows, int cols) {
        config = new char[rows][cols];
        this.rows = rows;
        this.cols = cols;
    }

    public char get(int r, int c) {
        return config[r][c];
    }

    public char get(Point p) {
        return config[p.x][p.y];
    }

    public void set(int r, int c, char letter) {
        if (config[r][c] != letter) {
            config[r][c] = letter;
            this.setChanged();
            this.notifyObservers(new Point(r, c));
        }
    }

    public void set(Point p, char letter) {
        set(p.x, p.y, letter);
    }

    public int getNumRows() {
        return rows;
    }

    public int getNumColumns() {
        return cols;
    }

    public boolean isEmpty(int r, int c) {
        return config[r][c] == 0;
    }

    public boolean isEmpty(Point p) { return isEmpty(p.x, p.y); }

    public void empty(Point p) {
        set(p, (char)0);
    }
}
