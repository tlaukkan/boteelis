package boteelis.vision;

import boteelis.vision.algorithms.StereoCorrelation;
import boteelis.vision.algorithms.Watersheding;
import boteelis.vision.model.Region;
import boteelis.vision.model.StereoFrame;
import boteelis.vision.model.VisionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Created with IntelliJ IDEA.
 * User: tlaukkan
 * Date: 20.7.2013
 * Time: 17:35
 * To change this template use File | Settings | File Templates.
 */
public class AnalysisComponent {
    final Logger logger = LoggerFactory.getLogger(AnalysisComponent.class);

    private VisionContext context;

    private boolean exited = false;

    private ExecutorService executor = Executors.newFixedThreadPool(1);

    private Future mainFuture;

    public AnalysisComponent(VisionContext context) {
        this.context = context;
    }

    public void startup() throws InterruptedException, ExecutionException, InvocationTargetException {
        mainFuture = executor.submit(new Runnable() {
            @Override
            public void run() {
                process();
            }
        });
    }

    public void shutdown() {
        exited = true;
        mainFuture.cancel(true);
        executor.shutdown();
    }

    public void process() {
        while (!exited) {
            try {
                if (context.capturedFrames.size() > 0) {
                    final StereoFrame stereoFrame = context.capturedFrames.poll();
                    synchronized (context.capturedFrames) {
                        context.capturedFrames.notifyAll();
                    }

                    int width = stereoFrame.width;
                    int height = stereoFrame.height;

                    long startTimeMillis = System.currentTimeMillis();

                    int[] leftColors = stereoFrame.rightRawRgb;
                    int[] rightColors = stereoFrame.leftRawRgb;
                    int[] regionColors = new int[width*height];
                    int[] outputColors = new int[width*height];
                    float[] gradient = new float[width*height];

                    Watersheding.gradient(width, height, rightColors, gradient);
                    Watersheding.watershed(width, height, rightColors, gradient, regionColors);

                    final Map<Integer, Region> indexRegionMap = new HashMap<Integer, Region>();

                    StereoCorrelation.analyzeRegions(width, height, rightColors, regionColors, stereoFrame.regions, indexRegionMap, 0.015f, 0.015f);
                    StereoCorrelation.correlateRegions(width, height, leftColors, rightColors, stereoFrame.regions);

                    StereoCorrelation.renderRegions(width, height,indexRegionMap, leftColors, outputColors);

                    stereoFrame.regionsRawRgb = outputColors;

                    logger.info("Analysis took: " + (System.currentTimeMillis() -  startTimeMillis) + "ms.");
                    logger.info("Found regions: " + stereoFrame.regions.size());

                    context.analyzedFrames.put(stereoFrame);
                    synchronized (context.analyzedFrames) {
                        context.analyzedFrames.notifyAll();
                    }

                } else {
                    synchronized (context.capturedFrames) {
                        context.capturedFrames.wait();
                    }
                }
            } catch (InterruptedException e) {
                logger.debug("Interrupted.");
            } catch (Exception e) {
                logger.error("Error analysing frame.", e);
            }
        }
    }

    private BufferedImage convertRawRgbToImage(int width, int height,int[] inputColors) {
        BufferedImage leftImage = new BufferedImage(width, height,
                BufferedImage.TYPE_INT_ARGB);
        int[] leftColors = ((DataBufferInt) leftImage.getRaster().getDataBuffer()).getData();
        for (int i = 0; i < width * height; i++) {
            leftColors[i] = inputColors[i];
        }
        return leftImage;
    }

    public BufferedImage convertImage(BufferedImage inputImage) {
        int type = inputImage.getType();
        if(type!=BufferedImage.TYPE_INT_ARGB) {
            BufferedImage tempImage = new BufferedImage(inputImage.getWidth(),inputImage.getHeight(),BufferedImage.TYPE_INT_ARGB);
            Graphics g = tempImage.createGraphics();
            g.drawImage(inputImage,0,0,null);
            g.dispose();
            inputImage = tempImage;
        }
        return inputImage;
    }
}
