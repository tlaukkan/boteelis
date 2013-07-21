package boteelis.vision.model;

import java.util.LinkedList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.SynchronousQueue;

/**
 * Created with IntelliJ IDEA.
 * User: tlaukkan
 * Date: 20.7.2013
 * Time: 16:46
 * To change this template use File | Settings | File Templates.
 */
public class VisionContext {
    public VisionContext(int width, int height) {
        this.width = width;
        this.height = height;
    }
    public int width;
    public int height;
    public SynchronousQueue<StereoFrame> capturedFrames = new SynchronousQueue<StereoFrame>();
    public SynchronousQueue<StereoFrame> analyzedFrames = new SynchronousQueue<StereoFrame>();
}
