package boteelis.vision;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.IOException;
import java.util.*;

/**
 * Image region growing example.
 *
 * @author Tommi S.E. Laukkanen
 */
public class Watersheding {

    public static void main(final String[] args) throws IOException {
        BufferedImage inputImage = ImageStorage.readImage(args[0]);

        int width = inputImage.getWidth();
        int height = inputImage.getHeight();

        BufferedImage outputImage = new BufferedImage(inputImage.getWidth(),inputImage.getHeight(),BufferedImage.TYPE_INT_ARGB);

        long startTimeMillis = System.currentTimeMillis();

        int[] inputColors = ((DataBufferInt) inputImage.getRaster().getDataBuffer()).getData();
        int[] outputColors = ((DataBufferInt) outputImage.getRaster().getDataBuffer()).getData();
        float[] gradient = new float[width*height];

        //smooth(width, height, inputColors, smoothed);
        gradient(width, height, inputColors, gradient);
        watershed(width, height, inputColors, gradient, outputColors);
        //horizontalGradient(width, height, regions, gradient);
        //renderGradient(width, height, gradient, outputColors);
        //fillRegions(width, height, regions, outputColors, 0.005f, 0.1f);

        System.out.println("Manipulation took: " + (System.currentTimeMillis() -  startTimeMillis) + "ms.");

        ImageStorage.writeImage(args[1], outputImage);
    }

    private static void renderGradient(int width, int height, float[] gradient, int[] outputColors) {
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int i = x + y * width;
                float g = gradient[i];
                if (g < 0) {
                    g = 0;
                }
                if (g > 255) {
                    g = 255;
                }
                int color = (int) 255;
                color = (color << 8) + (int) g;
                color = (color << 8) + (int) g;
                color = (color << 8) + (int) g;
                outputColors[i] = color;
            }
        }
    }

    public static void watershed(int width, int height, int[] inputColors, float[] gradient, int[] outputColors) {
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
                //reverseWatershedPixel(width, height, inputColors, gradient, outputColors, x, y, indexes, indexes2);
                break;
            }
            if (i == minGradientIndex || indexes2.contains(minGradientIndex)) {
                regionColor = inputColors[i];
                //reverseWatershedPixel(width, height, inputColors, gradient, outputColors, x, y, indexes, indexes2);
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

    private static void reverseWatershedPixel(int width, int height, int[] inputColors, float[] gradient, int[] outputColors, int x0, int y0, LinkedList<Integer> indexes,
                                       final Set<Integer> indexes2) {
        int i0 = x0 + y0 * width;

        indexes.push(i0);
        //indexes2.add(i0);
        while (indexes.size() > 0) {
            int i = indexes.pop();
            int x = i % width;
            int y = (i -  x) / width;
            pushIndexIfGreaterGradient(width, height, gradient[i], x - 1, y, gradient, indexes, indexes2);
            pushIndexIfGreaterGradient(width, height, gradient[i], x + 1, y, gradient, indexes, indexes2);
            pushIndexIfGreaterGradient(width, height, gradient[i], x, y - 1, gradient, indexes, indexes2);
            pushIndexIfGreaterGradient(width, height, gradient[i], x, y + 1, gradient, indexes, indexes2);
        }
    }

    private static void pushIndexIfGreaterGradient(int width, int height, float currentGradient, int x, int y, float[] gradient, LinkedList<Integer> indexes,
                                                   final Set<Integer> indexes2) {
        if (x > -1 && x < width && y > -1 && y < height) {
            int nextIndex = x + y * width;
            if (gradient[nextIndex] < currentGradient) {
                return;
            }
            if (indexes2.contains(nextIndex) || indexes.contains(nextIndex)) {
                return;
            }
            indexes.push(nextIndex);
            indexes2.add(nextIndex);
        }
    }

    private static int getMinGradientIndex(int width, int height, float[] gradient, int x, int y) {
        float currentGradient = getGradient(width, height, gradient, x, y);
        float topGradient = getGradient(width, height, gradient, x, y + 1);
        float bottomGradient = getGradient(width, height, gradient, x, y - 1);
        float leftGradient = getGradient(width, height, gradient, x - 1, y);
        float rightGradient = getGradient(width, height, gradient, x + 1, y);

        if (currentGradient < topGradient && currentGradient < leftGradient && currentGradient < bottomGradient
                && currentGradient < rightGradient) {
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
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int i = x + y * width;
                if (outputColors[i] == 0) {
                    fillRegion(width, height, inputColors, outputColors, x, y, hueTolerance, brightnessTolerance);
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

    public static boolean isSameRegion(int color0, int color1, float hueTolerance, float brightnessTolerance) {
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

        if (hueDelta < hueTolerance && brightnessDelta < brightnessTolerance) {
            return true;
        } else {
            return false;
        }
    }

    public static void gradient(int width, int height, int[] inputColors, float[] gradient) {
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

    private static void horizontalGradient(int width, int height, int[] inputColors, float[] gradient) {
        for (int x = 0; x < width; x++) {

            for (int y = 0; y < height; y++) {

                final ColorSum colorSum = new ColorSum();

                gradientSample(width, height, inputColors, x, y, x - 1, y, colorSum, 1f);

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

    public static void smooth(int width, int height, int[] inputColors, int[] outputColors) {
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

}
