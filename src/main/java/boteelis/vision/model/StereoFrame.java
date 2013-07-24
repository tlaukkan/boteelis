package boteelis.vision.model;

import java.util.LinkedList;

/**
 * Created with IntelliJ IDEA.
 * User: tlaukkan
 * Date: 20.7.2013
 * Time: 10:52
 * To change this template use File | Settings | File Templates.
 */
public class StereoFrame {
    public boolean turn;
    public long captureTimeMillis;
    public int captureWidth;
    public int captureHeight;
    public int width;
    public int height;
    public int[] leftRawRgb;
    public int[] rightRawRgb;
    public int[] regionsRawRgb;
    public int[] correlationsRawRgb;
    public int captureCount;

    public StereoFrame(long captureTimeMillis, int captureWidth, int captureHeight, boolean turn) {
        this.captureTimeMillis = captureTimeMillis;
        this.turn = turn;
        this.captureWidth = captureWidth;
        this.captureHeight = captureHeight;
        if (turn) {
            this.leftRawRgb = new int[captureWidth*captureHeight];
            this.rightRawRgb = new int[captureWidth*captureHeight];
            this.height = captureWidth;
            this.width = captureHeight;
        } else {
            this.width = captureWidth;
            this.height = captureHeight;
        }
        this.leftRawRgb = new int[this.width*this.height];
        this.rightRawRgb = new int[this.width*this.height];
    }

    public void addCapture(int[] leftRawRgb, int[] rightRawRgb) {
        int[] newLeftRawRgb;
        int[] newRightRawRgb;
        if (turn) {
            newLeftRawRgb = new int[width*height];
            newRightRawRgb = new int[width*height];
            for (int x = 0; x < captureWidth; x++) {
                for (int y = 0; y < captureHeight; y++) {
                    newLeftRawRgb[captureHeight  - 1 - y + x * captureHeight] = leftRawRgb[x + y * captureWidth];
                    newRightRawRgb[captureHeight - 1 - y + x * captureHeight] = rightRawRgb[x + y * captureWidth];
                }
            }
        } else {
            newLeftRawRgb = leftRawRgb;
            newRightRawRgb = rightRawRgb;
        }

        for (int i = 0; i < width * height; i++) {
                this.leftRawRgb[i] = average(this.leftRawRgb[i], newLeftRawRgb[i]);
                this.rightRawRgb[i] = average(this.rightRawRgb[i], newRightRawRgb[i]);
        }

        captureCount++;
    }

    private int average(int oldAverageColor, int newColor) {
        if (oldAverageColor == newColor) {
            return oldAverageColor;
        }
        float red0 = (oldAverageColor >> 16) & 0xff;;
        float green0 = (oldAverageColor >> 8) & 0xff;;
        float blue0 = (oldAverageColor >> 0) & 0xff;;

        float red1 = (newColor >> 16) & 0xff;
        float green1 = (newColor >> 8) & 0xff;
        float blue1 = (newColor >> 0) & 0xff;

        int color = (int) 255;
        color = (color << 8) + (int) ((red0*captureCount + red1) / (captureCount + 1));
        color = (color << 8) + (int) ((green0*captureCount + green1) / (captureCount + 1));
        color = (color << 8) + (int) ((blue0*captureCount + blue1) / (captureCount + 1));

        return color;
    }

    public final LinkedList<Region> regions = new LinkedList<Region>();

}
