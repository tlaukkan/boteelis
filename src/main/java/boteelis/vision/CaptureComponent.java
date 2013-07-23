package boteelis.vision;

import boteelis.vision.algorithms.ImageConvert;
import boteelis.vision.model.StereoFrame;
import boteelis.vision.model.VisionContext;
import com.github.sarxos.webcam.Webcam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.concurrent.*;

/**
 * Created with IntelliJ IDEA.
 * User: tlaukkan
 * Date: 20.7.2013
 * Time: 17:35
 * To change this template use File | Settings | File Templates.
 */
public class CaptureComponent {
    final Logger logger = LoggerFactory.getLogger(CaptureComponent.class);

    private VisionContext context;

    private boolean exited = false;

    private ExecutorService executor = Executors.newFixedThreadPool(3);

    private Webcam leftCam;
    private Webcam rightCam;
    private Future mainFuture;

    public CaptureComponent(VisionContext context) {
        this.context = context;
    }

    public void startup() throws InterruptedException, ExecutionException {
        Future<Webcam> leftCamFuture = executor.submit(new Callable<Webcam>() {
            @Override
            public Webcam call() throws Exception {
                logger.info("Preparing left camera...");
                final Webcam webcam = Webcam.getWebcams().get(0);
                webcam.setViewSize(new Dimension(context.captureWidth, context.captureHeight));
                webcam.open();
                logger.info("Prepared left camera...");
                return webcam;
            }
        });
        Future<Webcam> rightCamFuture = executor.submit(new Callable<Webcam>() {
            @Override
            public Webcam call() throws Exception {
                logger.info("Preparing right camera...");
                final Webcam webcam = Webcam.getWebcams().get(1);
                webcam.setViewSize(new Dimension(context.captureWidth, context.captureHeight));
                webcam.open();
                logger.info("Prepared right camera...");
                return webcam;
            }
        });

        leftCam = leftCamFuture.get();
        rightCam = rightCamFuture.get();

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

                final long captureBeginMillis = System.currentTimeMillis();

                Future<BufferedImage> leftCamFuture = executor.submit(new Callable<BufferedImage>() {
                    @Override
                    public BufferedImage call() throws Exception {
                        return leftCam.getImage();
                    }
                });
                Future<BufferedImage> rightCamFuture = executor.submit(new Callable<BufferedImage>() {
                    @Override
                    public BufferedImage call() throws Exception {
                        return rightCam.getImage();
                    }
                });

                final BufferedImage leftImage = ImageConvert.convertImage(leftCamFuture.get());
                final BufferedImage rightImage = ImageConvert.convertImage(rightCamFuture.get());

                final long captureEndMillis = System.currentTimeMillis();
                final long captureTimeMillis = (captureBeginMillis + captureEndMillis) / 2;

                if (context.currentCaptureFrame == null) {
                    context.currentCaptureFrame = new StereoFrame(captureTimeMillis,
                            context.captureWidth, context.captureHeight, context.turn);
                }

                context.currentCaptureFrame.addCapture(((DataBufferInt) leftImage.getRaster().getDataBuffer()).getData(),
                        ((DataBufferInt) rightImage.getRaster().getDataBuffer()).getData());

                if (context.capturedFrames.size() == 0 && context.currentCaptureFrame.captureCount > 5) {
                    context.capturedFrames.put(context.currentCaptureFrame);
                    context.currentCaptureFrame = null;
                    synchronized (context.capturedFrames) {
                        context.capturedFrames.notifyAll();
                    }
                }

            } catch (InterruptedException e) {
                logger.debug("Interrupted.");
            } catch (Exception e) {
                logger.error("Error capturing frame.", e);
            }
        }
    }

}
