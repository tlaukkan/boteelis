package boteelis.vision.algorithms;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

/**
 * Created with IntelliJ IDEA.
 * User: tlaukkan
 * Date: 23.7.2013
 * Time: 14:27
 * To change this template use File | Settings | File Templates.
 */
public class ImageConvert {
    public static BufferedImage convertRawRgbToImage(int width, int height,int[] inputColors) {
        BufferedImage leftImage = new BufferedImage(width, height,
                BufferedImage.TYPE_INT_ARGB);
        int[] leftColors = ((DataBufferInt) leftImage.getRaster().getDataBuffer()).getData();
        for (int i = 0; i < width * height; i++) {
            leftColors[i] = inputColors[i];
        }
        return leftImage;
    }

    public static BufferedImage convertImage(BufferedImage inputImage) {
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
