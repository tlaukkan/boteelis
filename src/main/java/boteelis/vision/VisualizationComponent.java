package boteelis.vision;

import boteelis.vision.model.StereoFrame;
import boteelis.vision.model.VisionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
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
    private VisionContext context;

    private boolean exited = false;

    private ExecutorService executor = Executors.newFixedThreadPool(1);

    private Future mainFuture;

    public VisualizationComponent(VisionContext context) {
        this.context = context;
        this.panel = new JPanel();
        this.panel.setSize(4 * context.width, context.height);
    }

    public JPanel getPanel() {
        return panel;
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
                    final BufferedImage leftImage = convertRawRgbToImage(stereoFrame.width, stereoFrame.height, stereoFrame.leftRawRgb);
                    final BufferedImage rightImage = convertRawRgbToImage(stereoFrame.width, stereoFrame.height, stereoFrame.rightRawRgb);
                    final BufferedImage regionImage = convertRawRgbToImage(stereoFrame.width, stereoFrame.height, stereoFrame.regionsRawRgb);
                    final BufferedImage correlationImage = convertRawRgbToImage(stereoFrame.width, stereoFrame.height, stereoFrame.correlationsRawRgb);

                    SwingUtilities.invokeAndWait(new Runnable() {
                        @Override
                        public void run() {
                            panel.getGraphics().drawImage(leftImage, 0, 0, null);
                            panel.getGraphics().drawImage(rightImage, context.width, 0, null);
                            panel.getGraphics().drawImage(regionImage, 2 * context.width, 0, null);
                            panel.getGraphics().drawImage(correlationImage, 3 * context.width, 0, null);
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
