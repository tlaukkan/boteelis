package boteelis.vision;

import com.github.sarxos.webcam.Webcam;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.File;

/**
 * Created with IntelliJ IDEA.
 * User: tlaukkan
 * Date: 19.7.2013
 * Time: 22:38
 * To change this template use File | Settings | File Templates.
 */
public class StereoCapture {

    public static void main(String[] args) throws Exception {

        Webcam webcam1 = Webcam.getWebcams().get(0);
        Webcam webcam2 = Webcam.getWebcams().get(1);
        webcam1.setViewSize(new Dimension(640, 480));
        webcam1.open();
        webcam2.setViewSize(new Dimension(640, 480));
        webcam2.open();

        for (int i = 0; i < 100; i++) {
            long startTimeMillis = System.currentTimeMillis();
            webcam1.getImage();
            webcam2.getImage();
            System.out.println(i + ") " + (System.currentTimeMillis() - startTimeMillis) + " ms.");
        }

        webcam1.close();
        webcam2.close();
    }
}
