package at.tuwien.panels;

import lombok.Setter;

public abstract class AbstractPanel {
    @Setter
    protected static String dataEndpoint;
    public static final String DATASRC_UID = "infinityDataSrc";

    protected static int x;
    protected static int y;
    private static int prevHeight = -1;

    public static void resetCoordinates() {
        x = 0;
        y = 0;
    }

    public static void addRowPlaceHolder() {
        y += 1;
    }

    public static void markNewRow() {
        x = 0;
    }


    public static void handleOverflow(int height, int width) {
        if ( (x + width) > 24) {
            x = 0;
            y += prevHeight == -1 ? height : prevHeight;
            prevHeight = height;
        }
    }

    public static void updateCoords(int height, int width) {
        x += width;

        if (x > 24) {
            x = 0;
            y += prevHeight == -1 ? height : prevHeight;
        }
        prevHeight = height;
    }

    public abstract String getConstructedPanel();
}
