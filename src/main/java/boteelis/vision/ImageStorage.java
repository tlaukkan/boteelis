package boteelis.vision;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * User: tlaukkan
 * Date: 19.7.2013
 * Time: 12:52
 * To change this template use File | Settings | File Templates.
 */
public class ImageStorage {
    public static void writeImage(String outputImageFile, BufferedImage outputImage) throws IOException {
        ImageIO.write(outputImage, "png", new FileOutputStream(outputImageFile, false));
    }

    public static BufferedImage readImage(String inputImageFile) throws IOException {
        BufferedImage inputImage = ImageIO.read(new FileInputStream(inputImageFile));
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
