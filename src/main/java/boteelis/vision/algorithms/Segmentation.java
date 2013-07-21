package boteelis.vision.algorithms;

import javax.imageio.ImageIO;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Efficient image manipulation example.
 *
 * @author Tommi S.E. Laukkanen
 */
public class Segmentation {
    public static void main(final String[] args) throws IOException {

        final String inputImageFile = args[0];
        final String outputImageFile = args[1];

        BufferedImage inputImage = ImageIO.read(new FileInputStream(inputImageFile));

        int type = inputImage.getType();
        if(type!=BufferedImage.TYPE_INT_ARGB) {
            BufferedImage tempImage = new BufferedImage(inputImage.getWidth(),inputImage.getHeight(),BufferedImage.TYPE_INT_ARGB);
            Graphics g = tempImage.createGraphics();
            g.drawImage(inputImage,0,0,null);
            g.dispose();
            inputImage = tempImage;
        }

        int width = inputImage.getWidth();
        int height = inputImage.getHeight();

        BufferedImage outputImage = new BufferedImage(inputImage.getWidth(),inputImage.getHeight(),BufferedImage.TYPE_INT_ARGB);

        long startTimeMillis = System.currentTimeMillis();

        int[] inputColors = ((DataBufferInt) inputImage.getRaster().getDataBuffer()).getData();
        int[] outputColors = ((DataBufferInt) outputImage.getRaster().getDataBuffer()).getData();

        int tolerance = 50;

        for (int x = 0; x < width; x++) {
            float segmentRed = 0;
            float segmentGreen = 0;
            float segmentBlue = 0;
            int segmentCount = 0;

            for (int y = 0; y < height; y++) {
                int i = x + y * width;
                float red = (inputColors[i] >> 16) & 0xff;
                float green = (inputColors[i] >> 8) & 0xff;
                float blue = (inputColors[i] >> 0) & 0xff;

                float delta = Math.abs(red - segmentRed) + Math.abs(green - segmentGreen) + Math.abs(blue - segmentBlue);
                if (delta > tolerance || y == height - 1) {
                    int color = (int) 255;
                    color = (color << 8) + (int) segmentRed;
                    color = (color << 8) + (int) segmentGreen;
                    color = (color << 8) + (int) segmentBlue;
                    for (int dy = -segmentCount; dy < (y == height - 1 ? 1 : 0); dy++) {
                        int j = i + dy * width;
                        outputColors[j] = color;
                        int k = j + 1;

                        if (k < width * height) {
                            float red2 = (inputColors[k] >> 16) & 0xff;
                            float green2 = (inputColors[k] >> 8) & 0xff;
                            float blue2 = (inputColors[k] >> 0) & 0xff;

                            float delta2 = Math.abs(red2 - segmentRed) + Math.abs(green2 - segmentGreen) + Math.abs(blue2 - segmentBlue);
                            if (delta2 <= tolerance) {
                                inputColors[k] = color;
                            }
                        }
                    }
                    segmentRed = red;
                    segmentGreen = green;
                    segmentBlue = blue;
                    segmentCount = 1;
                } else {
                    segmentRed = (segmentRed * segmentCount + red) / (segmentCount + 1);
                    segmentGreen = (segmentGreen * segmentCount + green) / (segmentCount + 1);
                    segmentBlue = (segmentBlue * segmentCount + blue) / (segmentCount + 1);
                    segmentCount ++;
                }
            }
        }

        /*for (int y = 0; y < height; y++) {
            float segmentRed = 0;
            float segmentGreen = 0;
            float segmentBlue = 0;
            int segmentCount = 0;

            for (int x = 0; x < width; x++) {
                int i = x + y * width;
                float red = (inputColors[i] >> 16) & 0xff;
                float green = (inputColors[i] >> 8) & 0xff;
                float blue = (inputColors[i] >> 0) & 0xff;

                float delta = Math.abs(red - segmentRed) + Math.abs(green - segmentGreen) + Math.abs(blue - segmentBlue);
                if (delta > tolerance || x == width - 1) {
                    int color = (int) 255;
                    color = (color << 8) + (int) segmentRed;
                    color = (color << 8) + (int) segmentGreen;
                    color = (color << 8) + (int) segmentBlue;
                    for (int dx = -segmentCount; dx < (x == width - 1 ? 1 : 0); dx++) {
                        outputColors[i + dx] = color;
                    }
                    segmentRed = red;
                    segmentGreen = green;
                    segmentBlue = blue;
                    segmentCount = 1;
                } else {
                    segmentRed = (segmentRed * segmentCount + red) / (segmentCount + 1);
                    segmentGreen = (segmentGreen * segmentCount + green) / (segmentCount + 1);
                    segmentBlue = (segmentBlue * segmentCount + blue) / (segmentCount + 1);
                    segmentCount ++;
                }
            }
        }*/

        System.out.println("Manipulation took: " + (System.currentTimeMillis() -  startTimeMillis) + "ms.");

        ImageIO.write(outputImage, "png" ,new FileOutputStream(outputImageFile, false));
    }
}
