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

    public StereoFrame(long captureTimeMillis, int width, int height, int[] leftRawRgb, int[] rightRawRgb) {
        this.captureTimeMillis = captureTimeMillis;
        this.width = width;
        this.height = height;
        this.leftRawRgb = leftRawRgb;
        this.rightRawRgb = rightRawRgb;
    }

    public float[] gradient;

    public int[] regionRgb;
    final LinkedList<Region> regions = new LinkedList<Region>();

}
