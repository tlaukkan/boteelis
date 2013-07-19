package boteelis.vision;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Random;
import java.util.Set;

/**
 * Image region growing example.
 *
 * @author Tommi S.E. Laukkanen
 */
public class Watersheding {

    public static void main(final String[] args) throws IOException {

        final String inputImageFile = args[0];
        final String outputImageFile = args[1];

        BufferedImage inputImage = ImageIO.read(new FileInputStream(inputImageFile));

        int type = inputImage.getType();
        //if(type!=BufferedImage.TYPE_INT_ARGB) {
            BufferedImage tempImage = new BufferedImage(640,480,BufferedImage.TYPE_INT_ARGB);
            Graphics g = tempImage.createGraphics();
            g.drawImage(inputImage,0,0,640,480,null);
            g.dispose();
            inputImage = tempImage;
        //}

        int width = inputImage.getWidth();
        int height = inputImage.getHeight();

        BufferedImage outputImage = new BufferedImage(inputImage.getWidth(),inputImage.getHeight(),BufferedImage.TYPE_INT_ARGB);

        long startTimeMillis = System.currentTimeMillis();

        int[] inputColors = ((DataBufferInt) inputImage.getRaster().getDataBuffer()).getData();
        int[] smoothed = new int[width*height];
        float[] gradient = new float[width*height];
        int[] seedRegions = new int[width*height];
        //int[] filledRegions = new int[width*height];
        int[] outputColors = ((DataBufferInt) outputImage.getRaster().getDataBuffer()).getData();

        //smooth(width, height, inputColors, smoothed);

        gradient(width, height, inputColors, gradient);

        watershed(width, height, inputColors, gradient, outputColors);

        //fillRegions(width, height, seedRegions, outputColors, 0.005f, 0.1f);

        System.out.println("Manipulation took: " + (System.currentTimeMillis() -  startTimeMillis) + "ms.");

        ImageIO.write(outputImage, "png" ,new FileOutputStream(outputImageFile, false));
    }

    private static void watershed(int width, int height, int[] inputColors, float[] gradient, int[] outputColors) {
        final LinkedList<Integer> indexes = new LinkedList<Integer>();
        final Set<Integer> indexes2 = new HashSet<Integer>();
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int i = x + y * width;
                if (outputColors[i] == 0) {
                    watershedPixel(width, height, inputColors, gradient, outputColors, x, y, indexes, indexes2);
                }
            }
        }
    }

    private static void watershedPixel(int width, int height, int[] inputColors, float[] gradient, int[] outputColors, int x0, int y0, LinkedList<Integer> indexes,
        final Set<Integer> indexes2) {
        int i0 = x0 + y0 * width;

        indexes.push(i0);
        indexes2.add(i0);
        int regionColor = 0;
        while (indexes.size() > 0) {
            int i = indexes.pop();
            int x = i % width;
            int y = (i -  x) / width;

            int minGradientIndex = getMinGradientIndex(width, height, gradient, x, y);
            if (outputColors[i] != 0) {
                regionColor = outputColors[i];
                break;
            }
            if (i == minGradientIndex || indexes2.contains(minGradientIndex)) {
                regionColor = inputColors[i];
                break; // Returning back without finding region.
            }

            indexes.push(minGradientIndex);
            indexes2.add(minGradientIndex);
        }
        for (int i : indexes2) {
            outputColors[i] = regionColor;
        }
        indexes2.clear();
    }

    private static int getMinGradientIndex(int width, int height, float[] gradient, int x, int y) {
        float currentGradient = getGradient(width, height, gradient, x, y);
        float topGradient = getGradient(width, height, gradient, x, y + 1);
        float bottomGradient = getGradient(width, height, gradient, x, y - 1);
        float leftGradient = getGradient(width, height, gradient, x - 1, y);
        float rightGradient = getGradient(width, height, gradient, x + 1, y);

        if (currentGradient <= topGradient && currentGradient <= leftGradient && currentGradient <= bottomGradient
                && currentGradient <= rightGradient) {
            return x + y * width;
        }
        if (topGradient < bottomGradient) {
            if (leftGradient < rightGradient) {
                if (topGradient < leftGradient) {
                    return x + (y + 1) * width;
                } else {
                    return x - 1 + (y) * width;
                }
            } else {
                if (topGradient < rightGradient) {
                    return x + (y + 1) * width;
                } else {
                    return x + 1 + (y) * width;
                }
            }
        } else {
            if (leftGradient < rightGradient) {
                if (bottomGradient < leftGradient) {
                    return x + (y - 1) * width;
                } else {
                    return x - 1 + (y) * width;
                }
            } else {
                if (bottomGradient < rightGradient) {
                    return x + (y - 1) * width;
                } else {
                    return x + 1 + (y) * width;
                }
            }
        }
    }

    private static float getGradient(int width, int height, float[] gradient, int x, int y) {
        if (x > -1 && x < width && y > -1 && y < height) {
            int i = x + y * width;
            return gradient[i];
        } else {
            return 256;
        }
    }

    private static void fillRegions(int width, int height, int[] inputColors, int[] outputColors, float hueTolerance, float brightnessTolerance) {
        //int colorIndex = 0;
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int i = x + y * width;
                if (outputColors[i] == 0) {
                    fillRegion(width, height, inputColors, outputColors, x, y, hueTolerance, brightnessTolerance);
                    /*colorIndex++;
                    if (colorIndex == colors.length) {
                        colorIndex = 0;
                    }*/
                }
            }
        }
    }

    private static void fillRegion(int width, int height, int[] inputColors, int[] outputColors, int x0, int y0, float hueTolerance, float brightnessTolerance) {
        int i0 = x0 + y0 * width;
        int fillColor = inputColors[i0];

        final LinkedList<IndexPair> indexes = new LinkedList<IndexPair>();
        indexes.push(new IndexPair(i0,i0));
        while (indexes.size() > 0) {
            IndexPair indexPair = indexes.pop();
            int lastIndex = indexPair.source;
            int currentIndex = indexPair.target;
            int x = currentIndex % width;
            int y = (currentIndex -  x) / width;
            if (x > 0 && x < width - 1 && y > 0 && y < height - 1) {
                if (isSameRegion(inputColors[lastIndex], inputColors[currentIndex], hueTolerance, brightnessTolerance)) {
                    outputColors[currentIndex] = fillColor;
                    if (outputColors[(x + 1) + (y) * width] == 0) {
                        indexes.push(new IndexPair(currentIndex, (x + 1) + (y) * width));
                    }
                    if (outputColors[(x - 1) + (y) * width] == 0) {
                        indexes.push(new IndexPair(currentIndex,(x - 1) + (y) * width));
                    }
                    if (outputColors[(x) + (y + 1) * width] == 0) {
                        indexes.push(new IndexPair(currentIndex,(x) + (y + 1) * width));
                    }
                    if (outputColors[(x) + (y - 1) * width] == 0) {
                        indexes.push(new IndexPair(currentIndex,(x) + (y - 1) * width));
                    }
                }
            }
        }
    }

    private static boolean isSameRegion(int color0, int color1, float hueTolerance, float brightnessTolerance) {
        if (color0 == color1) {
            return true;
        }
        float red0 = (color0 >> 16) & 0xff;;
        float green0 = (color0 >> 8) & 0xff;;
        float blue0 = (color0 >> 0) & 0xff;;
        float brightness0 = Math.max(Math.max(red0, green0), blue0);
        float redHue0 = red0 / brightness0;
        float greenHue0 = green0 / brightness0;
        float blueHue0 = blue0 / brightness0;

        float red = (color1 >> 16) & 0xff;
        float green = (color1 >> 8) & 0xff;
        float blue = (color1 >> 0) & 0xff;
        float brightness = Math.max(Math.max(red, green), blue);
        float redHue = red / brightness;
        float greenHue = green / brightness;
        float blueHue = blue / brightness;

        float hueDelta = (Math.abs(redHue - redHue0) + Math.abs(greenHue - greenHue0) + Math.abs(blueHue - blueHue0)) / 3;
        float brightnessDelta = Math.abs(brightness - brightness0) / 255f;

        /*if (brightness0 > 10 && brightness > 10) {*/
            if (hueDelta < hueTolerance && brightnessDelta < brightnessTolerance) {
                return true;
            } else {
                return false;
            }
        /*} else if (brightness0 <= 10 && brightness <= 10) {
            return true;
        } else {
            return false;
        }*/
    }

    private static void gradient(int width, int height, int[] inputColors, float[] gradient) {
        for (int x = 0; x < width; x++) {

            for (int y = 0; y < height; y++) {

                final ColorSum colorSum = new ColorSum();

                gradientSample(width, height, inputColors, x, y, x + 1, y, colorSum, 1f);
                gradientSample(width, height, inputColors, x, y, x, y + 1, colorSum, 1f);
                gradientSample(width, height, inputColors, x, y, x - 1, y, colorSum, 1f);
                gradientSample(width, height, inputColors, x, y, x, y - 1, colorSum, 1f);

                gradientSample(width, height, inputColors, x, y, x + 1, y + 1, colorSum, 1f);
                gradientSample(width, height, inputColors, x, y, x - 1, y + 1, colorSum, 1f);
                gradientSample(width, height, inputColors, x, y, x + 1, y - 1, colorSum, 1f);
                gradientSample(width, height, inputColors, x, y, x - 1, y - 1, colorSum, 1f);

                colorSum.sumRed /= colorSum.count;
                colorSum.sumGreen /= colorSum.count;
                colorSum.sumBlue /= colorSum.count;

                float sum = colorSum.sumRed + colorSum.sumGreen + colorSum.sumBlue;

                gradient[x + y * width] = sum;
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

    private static class IndexPair {
        public IndexPair(int source, int target) {
            this.source = source;
            this.target = target;
        }
        public int source;
        public int target;
    }

    private static class ColorSum {
        float sumRed = 0;
        float sumGreen = 0;
        float sumBlue = 0;
        int count = 0;
    }

    /*static int[] colors;
    static Random random = new Random(0);

    static {
        int n = 2048;
        colors = new int[n];
        for(int i = 0; i < n; i++)
        {
            if (i == 0) {
                colors[i] = generateRandomColor(null).getRGB();
            } else {
                colors[i] = generateRandomColor(new Color(colors[i - 1])).getRGB();
            }
        }
    }

    public static Color generateRandomColor(Color mix) {
        int red = random.nextInt(256);
        int green = random.nextInt(256);
        int blue = random.nextInt(256);

        // mix the color
        if (mix != null) {
            red = (red + mix.getRed()) / 2;
            green = (green + mix.getGreen()) / 2;
            blue = (blue + mix.getBlue()) / 2;
        }

        Color color = new Color(red, green, blue);
        return color;
    }*/

}
