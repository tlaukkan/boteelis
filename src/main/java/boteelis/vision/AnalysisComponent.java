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
                    int[] smoothedRightColors = new int[width*height];
                    int[] smoothedLeftColors = new int[width*height];
                    float[] gradient = new float[width*height];

                    Watersheding.smooth(width, height, rightColors, smoothedRightColors);
                    Watersheding.smooth(width, height, leftColors, smoothedLeftColors);
                    Watersheding.gradient(width, height, smoothedRightColors, gradient);
                    Watersheding.watershed(width, height, smoothedRightColors, gradient, regionColors);

                    final Map<Integer, Region> indexRegionMap = new HashMap<Integer, Region>();

                    StereoCorrelation.analyzeRegions(width, height, smoothedRightColors, regionColors,
                            stereoFrame.regions, indexRegionMap, 0.015f, 0.015f);
                    float correlation = StereoCorrelation.correlateRegions(width, height, smoothedLeftColors,
                            smoothedRightColors, stereoFrame.regions,
                            context.leftRedShift, context.leftGreenShift, context.leftBlueShift, context.leftYShift);

                    //float correlatedBrightnessDifference =  StereoCorrelation.compareCorrelatedRegionsBrightness(width, height, indexRegionMap, leftColors, rightColors);

                    StereoCorrelation.renderRegions(width, height, indexRegionMap, leftColors, outputColors);

                    stereoFrame.leftRawRgb = smoothedLeftColors;
                    stereoFrame.rightRawRgb = smoothedRightColors;
                    stereoFrame.regionsRawRgb = regionColors;
                    stereoFrame.correlationsRawRgb = outputColors;

                    logger.info("Analysis took: " + (System.currentTimeMillis() -  startTimeMillis) + "ms.");
                    logger.info("Found regions: " + stereoFrame.regions.size() + " Capture averaged over: " + stereoFrame.captureCount);
                    logger.info("Correlation: " + (((int) (correlation * 10000)) / 100f) + "%");

                    if (correlation < 0.90 ||(correlation < 0.98 && System.currentTimeMillis() - context.lastCalibrationMillis > 60000)) {
                        logger.info("Calibrating brightness due to low correction...");
                        context.lastCalibrationMillis = System.currentTimeMillis();

                        float lastCorrelation = correlation;
                        int i = 0;
                        while (true) {
                            float maxCorrelation = - Float.MAX_VALUE;
                            float bestRedShift = context.leftRedShift;
                            float bestGreenShift = context.leftGreenShift;
                            float bestBlueShift = context.leftBlueShift;
                            for (int r = -1; r <= 1; r+=1) {
                                for (int g = -1; g <= 1; g+=1) {
                                    for (int b = -1; b <= 1; b+=1) {
                                        if (r == 0 && g == 0 && b == 0)  {
                                            continue;
                                        }
                                        float redShiftCandidate = context.leftRedShift + r;
                                        float greenShiftCandidate = context.leftGreenShift + g;
                                        float blueShiftCandidate = context.leftBlueShift + b;
                                        float newCorrelation = StereoCorrelation.correlateRegions(width, height,
                                                smoothedLeftColors, smoothedRightColors, stereoFrame.regions,
                                                redShiftCandidate, greenShiftCandidate, blueShiftCandidate, context.leftYShift);
                                        if (newCorrelation > maxCorrelation) {
                                            maxCorrelation = newCorrelation;
                                            bestRedShift = redShiftCandidate;
                                            bestGreenShift = greenShiftCandidate;
                                            bestBlueShift = blueShiftCandidate;
                                        }
                                    }
                                }
                            }
                            context.leftRedShift = bestRedShift;
                            context.leftGreenShift = bestGreenShift;
                            context.leftBlueShift = bestBlueShift;

                            if (lastCorrelation >= maxCorrelation) {
                                break;
                            }
                            lastCorrelation = maxCorrelation;
                            i++;
                            logger.info("Calibration iteration " + i + " candidate correlation " + maxCorrelation
                                    + " with left camera color shift:"
                                    + " r=" + context.leftRedShift
                                    + " g=" + context.leftGreenShift
                                    + " b=" + context.leftBlueShift
                                    + " y=" + context.leftYShift);
                        }

                        float maxCorrelation = - Float.MAX_VALUE;
                        int bestYShift = context.leftYShift;
                        for (int y = -5; y <= 5; y+=1) {
                            int yShiftCandidate = context.leftYShift + y;
                            float newCorrelation = StereoCorrelation.correlateRegions(width, height,
                                    smoothedLeftColors, smoothedRightColors, stereoFrame.regions,
                                    context.leftRedShift, context.leftGreenShift, context.leftBlueShift,
                                    yShiftCandidate);
                            if (newCorrelation > maxCorrelation) {
                                maxCorrelation = newCorrelation;
                                bestYShift = yShiftCandidate;
                            }
                            i++;
                            logger.info("Calibration iteration " + i + " candidate correlation " + newCorrelation
                                    + " with left camera y shift:"
                                    + " r=" + context.leftRedShift
                                    + " g=" + context.leftGreenShift
                                    + " b=" + context.leftBlueShift
                                    + " y=" + yShiftCandidate);
                        }
                        context.leftYShift = bestYShift;
                        lastCorrelation = maxCorrelation;

                        logger.info("Calibration iteration " + i + " found best correlation " + lastCorrelation
                                + " with shifts:"
                                + " r=" + context.leftRedShift
                                + " g=" + context.leftGreenShift
                                + " b=" + context.leftBlueShift
                                + " y=" + context.leftYShift);

                    }


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

}
