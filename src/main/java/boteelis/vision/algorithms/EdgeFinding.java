package boteelis.vision.algorithms;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Image edge finding example.
 *
 * @author Tommi S.E. Laukkanen
 */
public class EdgeFinding {


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
        int[] tempColors = new int [width*height];
        int[] temp2Colors = new int [width*height];
        int[] outputColors = ((DataBufferInt) outputImage.getRaster().getDataBuffer()).getData();

        smooth(width, height, inputColors, tempColors);
        smooth(width, height, tempColors, temp2Colors);
        gradient(width, height, temp2Colors, outputColors);

        System.out.println("Manipulation took: " + (System.currentTimeMillis() -  startTimeMillis) + "ms.");

        ImageIO.write(outputImage, "png" ,new FileOutputStream(outputImageFile, false));
    }

    private static void gradient(int width, int height, int[] inputColors, int[] outputColors) {
        for (int x = 0; x < width; x++) {

            for (int y = 0; y < height; y++) {

                final ColorSum colorSum = new ColorSum();

                /*gradientSample(width, height, inputColors, x, y, x + 1, y, colorSum, 1f);
                gradientSample(width, height, inputColors, x, y, x, y + 1, colorSum, 1f);
                gradientSample(width, height, inputColors, x, y, x + 1, y + 1, colorSum, 1f);
                gradientSample(width, height, inputColors, x, y, x - 1, y + 1, colorSum, 1f);
                gradientSample(width, height, inputColors, x, y, x + 1, y - 1, colorSum, 1f);*/

                gradientSample(width, height, inputColors, x, y, x - 1, y, colorSum, 1f);
                gradientSample(width, height, inputColors, x, y, x, y - 1, colorSum, 1f);

                gradientSample(width, height, inputColors, x, y, x - 1, y - 1, colorSum, 1f);

                /*gradientSample(width, height, inputColors, x, y, x - 2, y, colorSum, 1f);
                gradientSample(width, height, inputColors, x, y, x, y - 2, colorSum, 1f);

                gradientSample(width, height, inputColors, x, y, x - 2, y - 1, colorSum, 1f);
                gradientSample(width, height, inputColors, x, y, x - 1, y - 2, colorSum, 1f);


                gradientSample(width, height, inputColors, x, y, x - 3, y, colorSum, 1f);
                gradientSample(width, height, inputColors, x, y, x, y - 3, colorSum, 1f);*/


                colorSum.sumRed /= colorSum.count;
                colorSum.sumGreen /= colorSum.count;
                colorSum.sumBlue /= colorSum.count;

                float sum = colorSum.sumRed + colorSum.sumGreen + colorSum.sumBlue;

                sum = sum * 2;

                if (sum < 0) {
                    sum = 0;
                }
                if (sum > 255) {
                    sum = 255;
                }

                int outputColor = (int) 255;
                outputColor = (outputColor << 8) + (int) (255 - sum);
                outputColor = (outputColor << 8) + (int) (255 - sum);
                outputColor = (outputColor << 8) + (int) (255 - sum);
                outputColors[x + y * width] = outputColor;
            }
        }
    }

    private static void limit(ColorSum colorSum) {
        if (colorSum.sumRed < 0) {
            colorSum.sumRed = 0;
        }
        if (colorSum.sumRed > 255) {
            colorSum.sumRed = 255;
        }
        if (colorSum.sumGreen < 0) {
            colorSum.sumGreen = 0;
        }
        if (colorSum.sumGreen > 255) {
            colorSum.sumGreen = 255;
        }
        if (colorSum.sumBlue < 0) {
            colorSum.sumBlue = 0;
        }
        if (colorSum.sumBlue > 255) {
            colorSum.sumBlue = 255;
        }
    }

    private static void smooth(int width, int height, int[] inputColors, int[] outputColors) {
        for (int x = 0; x < width; x++) {

            for (int y = 0; y < height; y++) {

                final ColorSum colorSum = new ColorSum();
                sumSample(width, height, inputColors, x, y, colorSum);
                sumSample(width, height, inputColors, x + 1, y, colorSum);
                sumSample(width, height, inputColors, x - 1, y, colorSum);
                sumSample(width, height, inputColors, x, y + 1, colorSum);
                sumSample(width, height, inputColors, x, y - 1, colorSum);

                sumSample(width, height, inputColors, x + 1, y + 1, colorSum);
                sumSample(width, height, inputColors, x - 1, y + 1, colorSum);
                sumSample(width, height, inputColors, x + 1, y - 1, colorSum);
                sumSample(width, height, inputColors, x - 1, y - 1, colorSum);

                sumSample(width, height, inputColors, x + 2, y, colorSum);
                sumSample(width, height, inputColors, x - 2, y, colorSum);
                sumSample(width, height, inputColors, x, y + 2, colorSum);
                sumSample(width, height, inputColors, x, y - 2, colorSum);

                int outputColor = (int) 255;
                outputColor = (outputColor << 8) + (int) (colorSum.sumRed / colorSum.count);
                outputColor = (outputColor << 8) + (int) (colorSum.sumGreen / colorSum.count);
                outputColor = (outputColor << 8) + (int) (colorSum.sumBlue / colorSum.count);
                outputColors[x + y * width] = outputColor;
            }
        }
    }

    private static void gradientSample(int width, int height, int[] inputColors, int x0, int y0, int x, int y, ColorSum colorSum, float weight) {
        int i0 = x0 + y0 * width;
        int i = x + y * width;
        if (i >= 0 && i < width * height) {
            int inputColor0 = inputColors[i0];
            float red0 = (inputColor0 >> 16) & 0xff;
            float green0 = (inputColor0 >> 8) & 0xff;
            float blue0 = (inputColor0 >> 0) & 0xff;

            int inputColor = inputColors[i];
            float red = (inputColor >> 16) & 0xff;
            float green = (inputColor >> 8) & 0xff;
            float blue = (inputColor >> 0) & 0xff;

            colorSum.sumRed += Math.abs(red - red0) * weight;
            colorSum.sumGreen += Math.abs(green - green0) * weight;
            colorSum.sumBlue += Math.abs(blue - blue0) * weight;
            colorSum.count++;
        }
    }

    private static void sumSample(int width, int height, int[] inputColors, int x, int y, ColorSum colorSum) {
        int i = x + y * width;
        if (i >= 0 && i < width * height) {
            int inputColor = inputColors[i];
            float red = (inputColor >> 16) & 0xff;
            float green = (inputColor >> 8) & 0xff;
            float blue = (inputColor >> 0) & 0xff;

            colorSum.sumRed += red;
            colorSum.sumGreen += green;
            colorSum.sumBlue += blue;
            colorSum.count++;
        }
    }

    private static class ColorSum {
        float sumRed = 0;
        float sumGreen = 0;
        float sumBlue = 0;
        int count = 0;
    }
}
