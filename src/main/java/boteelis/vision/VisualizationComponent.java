package boteelis.vision;

import boteelis.vision.algorithms.ImageConvert;
import boteelis.vision.model.Region;
import boteelis.vision.model.StereoFrame;
import boteelis.vision.model.VisionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.vecmath.Color3f;
import javax.vecmath.Point3f;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.*;

/**
 * Created with IntelliJ IDEA.
 * User: tlaukkan
 * Date: 20.7.2013
 * Time: 17:35
 * To change this template use File | Settings | File Templates.
 */
public class VisualizationComponent {
    final Logger logger = LoggerFactory.getLogger(VisualizationComponent.class);

    private JPanel panel;
    private Panel3d panel3d;
    private VisionContext context;

    private boolean exited = false;

    private ExecutorService executor = Executors.newFixedThreadPool(1);

    private Future mainFuture;

    public VisualizationComponent(VisionContext context) {
        this.context = context;
        this.panel = new JPanel();
        this.panel3d = new Panel3d();
    }

    public JPanel getPanel() {
        return panel;
    }

    public Panel3d getPanel3d() {
        return panel3d;
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
                if (context.analyzedFrames.size() > 0) {
                    final StereoFrame stereoFrame = context.analyzedFrames.poll();
                    final BufferedImage leftImage = ImageConvert.convertRawRgbToImage(stereoFrame.width, stereoFrame.height, stereoFrame.leftRawRgb);
                    final BufferedImage rightImage = ImageConvert.convertRawRgbToImage(stereoFrame.width, stereoFrame.height, stereoFrame.rightRawRgb);
                    final BufferedImage regionImage = ImageConvert.convertRawRgbToImage(stereoFrame.width, stereoFrame.height, stereoFrame.regionsRawRgb);
                    final BufferedImage correlationImage = ImageConvert.convertRawRgbToImage(stereoFrame.width, stereoFrame.height, stereoFrame.correlationsRawRgb);

                    SwingUtilities.invokeAndWait(new Runnable() {
                        @Override
                        public void run() {
                            panel.getGraphics().drawImage(leftImage, 0, 0, null);
                            panel.getGraphics().drawImage(rightImage, context.width, 0, null);
                            panel.getGraphics().drawImage(regionImage, 2 * context.width, 0, null);
                            panel.getGraphics().drawImage(correlationImage, 3 * context.width, 0, null);

                            for (final Region region : stereoFrame.regions) {
                                if (region.stereoCorrelation > 0.95) {
                                    panel3d.addPoint(new Point3f(region.rx, region.ry, region.rz),
                                            new Color3f(region.red / 255.f, region.green / 255.f, region.blue / 255.f));
                                }
                                //System.out.println(new Point3f(region.rx, region.ry, region.rz));
                            }
                            panel3d.drawPoints();
                        }
                    });

                    synchronized (context.analyzedFrames) {
                        context.analyzedFrames.notifyAll();
                    }
                } else {
                    synchronized (context.analyzedFrames) {
                        context.analyzedFrames.wait();
                    }
                }
            } catch (InterruptedException e) {
                logger.debug("Interrupted.");
            } catch (Exception e) {
                logger.error("Error visualizing frame.", e);
            }
        }
    }

}
