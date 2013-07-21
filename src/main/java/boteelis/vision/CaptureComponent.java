package boteelis.vision;

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

    private SynchronousQueue<Runnable> tasks = new SynchronousQueue<Runnable>();

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
                final Webcam webcam = Webcam.getWebcams().get(0);
                webcam.setViewSize(new Dimension(context.width, context.height));
                webcam.open();
                return webcam;
            }
        });
        Future<Webcam> rightCamFuture = executor.submit(new Callable<Webcam>() {
            @Override
            public Webcam call() throws Exception {
                final Webcam webcam = Webcam.getWebcams().get(1);
                webcam.setViewSize(new Dimension(context.width, context.height));
                webcam.open();
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
            synchronized (context.capturedFrames) {
                try {
                    context.wait();

                    if (context.capturedFrames.size() < 3) {
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

                        final BufferedImage leftImage = leftCamFuture.get();
                        final BufferedImage rightImage = rightCamFuture.get();

                        final long captureEndMillis = System.currentTimeMillis();
                        final long captureTimeMillis = (captureBeginMillis + captureEndMillis) / 2;

                        final StereoFrame stereoFrame = new StereoFrame(captureTimeMillis,
                                context.width, context.height,
                                ((DataBufferInt) leftImage.getRaster().getDataBuffer()).getData(),
                                ((DataBufferInt) rightImage.getRaster().getDataBuffer()).getData());

                        context.capturedFrames.put(stereoFrame);
                        context.capturedFrames.notifyAll();
                    }

                } catch (InterruptedException e) {
                    logger.debug("Interrupted.");
                } catch (ExecutionException e) {
                    logger.error("Error capturing image.", e);
                }
            }
        }
    }

}
