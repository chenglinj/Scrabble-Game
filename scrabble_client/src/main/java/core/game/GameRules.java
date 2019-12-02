package core.game;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class GameRules {
    public enum Orientation {
        HORIZONTAL,
        VERTICAL,
    }

    public static Map<Orientation, String> getValidOrientations(Board board, Point pos) {
        Map<Orientation, String> ret = new HashMap<>(2);
        Map<Orientation, String> adjMap = getAdjacentWords(board, pos);

        int horLength = adjMap.get(Orientation.HORIZONTAL).length();
        int verLength = adjMap.get(Orientation.VERTICAL).length();

        if (horLength == 1 && horLength == verLength) { // isolated letter
            ret.put(Orientation.HORIZONTAL, adjMap.get(Orientation.HORIZONTAL));
        } else {
            if (horLength > 1)
                ret.put(Orientation.HORIZONTAL, adjMap.get(Orientation.HORIZONTAL));
            if (verLength > 1)
                ret.put(Orientation.VERTICAL, adjMap.get(Orientation.VERTICAL));
        }

        return ret;
    }

    private static Map<Orientation, String> getAdjacentWords(Board board, Point pos) {
        Map<Orientation, String> ret = new HashMap<>(2);

        // horizontal direction
        int[] range = new int[2];
        String word = Character.toString(board.get(pos));

        for (int t = 0; t < range.length; t++) { // horizontal
            int dir = (int)Math.pow(-1, t);

            range[t] = pos.x + dir;
            while (range[t] < board.getNumColumns() && range[t] >= 0) {
                if (board.isEmpty(range[t], pos.y))
                    break;

                Character letter = board.get(range[t], pos.y);
                word = (dir == 1) ? word + letter.toString() : letter.toString() + word;
                range[t] += dir;
            }
        }
        ret.put(Orientation.HORIZONTAL, word);

        // vertical
        range = new int[2];
        word = Character.toString(board.get(pos));
        for (int t = 0; t < range.length; t++) { // horizontal
            int dir = (int)Math.pow(-1, t);

            range[t] = pos.y + dir;
            while (range[t] < board.getNumRows() && range[t] >= 0) {
                if (board.isEmpty(pos.x, range[t]))
                    break;

                Character letter = board.get(pos.x, range[t]);
                word = (dir == 1) ? word + letter.toString() : letter.toString() + word;
                range[t] += dir;
            }
        }
        ret.put(Orientation.VERTICAL, word);

        return ret;
    }
}
