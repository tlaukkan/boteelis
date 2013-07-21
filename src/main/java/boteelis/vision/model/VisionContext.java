package boteelis.vision.model;

import java.util.concurrent.*;

/**
 * Created with IntelliJ IDEA.
 * User: tlaukkan
 * Date: 20.7.2013
 * Time: 16:46
 * To change this template use File | Settings | File Templates.
 */
public class VisionContext {
    public VisionContext(int captureWidth, int captureHeight, boolean turn) {
        this.turn = turn;
        if (turn) {
            this.width = captureHeight;
            this.height = captureWidth;
        } else {
            this.width = captureWidth;
            this.height = captureHeight;
        }
        this.captureWidth = captureWidth;
        this.captureHeight = captureHeight;
    }
    public boolean turn;
    public int captureWidth;
    public int captureHeight;
    public int width;
    public int height;

    public BlockingQueue <StereoFrame> capturedFrames = new LinkedBlockingQueue<StereoFrame>();
    public BlockingQueue <StereoFrame> analyzedFrames = new LinkedBlockingQueue <StereoFrame>();
}
