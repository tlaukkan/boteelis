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

    public long captureTimeMillis;
    public int width;
    public int height;
    public int[] leftRawRgb;
    public int[] rightRawRgb;
    public int[] regionsRawRgb;
    public int[] correlationsRawRgb;

    public StereoFrame(long captureTimeMillis, int width, int height, int[] leftRawRgb, int[] rightRawRgb, boolean turn) {
        this.captureTimeMillis = captureTimeMillis;

        if (turn) {
            this.leftRawRgb = new int[width*height];
            this.rightRawRgb = new int[width*height];
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    this.leftRawRgb[height  - 1 - y + x * height] = leftRawRgb[x + y * width];
                    this.rightRawRgb[height - 1 - y + x * height] = rightRawRgb[x + y * width];
                }
            }
            this.height = width;
            this.width = height;
        } else {
            this.width = width;
            this.height = height;
            this.leftRawRgb = leftRawRgb;
            this.rightRawRgb = rightRawRgb;
        }
    }

    public float[] gradient;

    public final LinkedList<Region> regions = new LinkedList<Region>();

}
