package boteelis.vision;

import com.github.sarxos.webcam.Webcam;

import javax.imageio.ImageIO;
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
        Webcam webcam = Webcam.getDefault();
        webcam.open();
        ImageIO.write(webcam.getImage(), "PNG", new File("target/capture.png"));
    }
}
