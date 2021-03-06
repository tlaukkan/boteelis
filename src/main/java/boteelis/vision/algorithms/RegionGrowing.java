package boteelis.vision.algorithms;

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
public class RegionGrowing {

    static int[] colors;
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
    }

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
        int[] smoothed = new int[width*height];
        float[] gradient = new float[width*height];
        int[] seedRegions = new int[width*height];
        int[] filledRegions = new int[width*height];
        int[] outputColors = ((DataBufferInt) outputImage.getRaster().getDataBuffer()).getData();

        //smooth(width, height, inputColors, smoothed);

        gradient(width, height, inputColors, gradient);

        seedRegions(width, height, inputColors, seedRegions, 0.018f, 0.3f);
        fillRegions(width, height, seedRegions, filledRegions); // Can this be done in previous step?
        growRegions(width, height, filledRegions, gradient, outputColors); // Can this be done in previous step?

        System.out.println("Manipulation took: " + (System.currentTimeMillis() -  startTimeMillis) + "ms.");

        ImageIO.write(outputImage, "png" ,new FileOutputStream(outputImageFile, false));
    }

    private static void growRegions(int width, int height, int[] inputColors, float[] gradient, int[] outputColors) {
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int i = x + y * width;
                if (outputColors[i] == 0) {
                    if (inputColors[i] == 0) {
                        //outputColors[i] = gradient[i];
                        if (outputColors[i] == 0) {
                            growRegion(width, height, inputColors, gradient, outputColors, x, y);
                        }
                        /*
                        int minGradientIndex = getMinGradientIndex(width, height, gradient, x, y);
                        if (inputColors[minGradientIndex] != 0) {
                            outputColors[i] = inputColors[minGradientIndex];
                        }
                        */
                    } else {
                        outputColors[i] = inputColors[i];
                    }
                }
            }
        }
    }

    private static void growRegion(int width, int height, int[] inputColors, float[] gradient, int[] outputColors, int x0, int y0) {
        int i0 = x0 + y0 * width;
        final LinkedList<Integer> indexes = new LinkedList<Integer>();
        final Set<Integer> indexes2 = new HashSet<Integer>();
        indexes.push(i0);
        int regionColor = 0;
        while (indexes.size() > 0) {
            int i = indexes.pop();
            int x = i % width;
            int y = (i -  x) / width;

            float g = gradient[i];
            if (g < 0) {
                g = 0;
            }
            if (g > 255) {
                g = 255;
            }
            int outputColor = (int) 255;
            outputColor = (outputColor << 8) + (int) (g);
            outputColor = (outputColor << 8) + (int) (0);
            outputColor = (outputColor << 8) + (int) (0);
            //outputColors[i] = (int) outputColor;

            int minGradientIndex = getMinGradientIndex(width, height, gradient, x, y);
            if (indexes2.contains(minGradientIndex)) {
                return; // Returning back without finding region.
            }
            if (inputColors[minGradientIndex] != 0) {
                regionColor = inputColors[minGradientIndex];
                break;
            }
            indexes.push(minGradientIndex);
            indexes2.add(minGradientIndex);
        }
        for (int i : indexes2) {
            /*float g = gradient[i];
            if (g < 0) {
                g = 0;
            }
            if (g > 255) {
                g = 255;
            }
            int outputColor = (int) 255;
            outputColor = (outputColor << 8) + (int) (g);
            outputColor = (outputColor << 8) + (int) (0);
            outputColor = (outputColor << 8) + (int) (g);
            outputColors[i] = (int) outputColor;*/


            outputColors[i] = regionColor;
        }
    }

    private static int getMinGradientIndex(int width, int height, float[] gradient, int x, int y) {
        float topGradient = getGradient(width, height, gradient, x, y + 1);
        float bottomGradient = getGradient(width, height, gradient, x, y - 1);
        float leftGradient = getGradient(width, height, gradient, x - 1, y);
        float rightGradient = getGradient(width, height, gradient, x + 1, y);
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

    private static void fillRegions(int width, int height, int[] inputColors, int[] outputColors) {
        int colorIndex = 0;
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int i = x + y * width;
                if (inputColors[i] != 0 && outputColors[i] == 0) {
                    fillRegion(width, height, inputColors, outputColors, x, y, colors[colorIndex]);
                    colorIndex++;
                    if (colorIndex == colors.length) {
                        colorIndex = 0;
                    }
                }
            }
        }
    }

    private static void fillRegion(int width, int height, int[] inputColors, int[] outputColors, int x0, int y0, int fillColor) {
        int i0 = x0 + y0 * width;
        final LinkedList<Integer> indexes = new LinkedList<Integer>();
        indexes.push(i0);
        while (indexes.size() > 0) {
            int i = indexes.pop();
            int x = i % width;
            int y = (i -  x) / width;
            if (x > 0 && x < width - 1 && y > 0 && y < height - 1) {
                if (inputColors[i] != 0 && outputColors[i] == 0) {
                    outputColors[i] = fillColor;
                    if (outputColors[(x + 1) + (y) * width] == 0) {
                        indexes.push((x + 1) + (y) * width);
                    }
                    if (outputColors[(x - 1) + (y) * width] == 0) {
                        indexes.push((x - 1) + (y) * width);
                    }
                    if (outputColors[(x) + (y + 1) * width] == 0) {
                        indexes.push((x) + (y + 1) * width);
                    }
                    if (outputColors[(x) + (y - 1) * width] == 0) {
                        indexes.push((x) + (y - 1) * width);
                    }
                }
            }
        }
    }

    private static void seedRegions(int width, int height, int[] inputColors, int[] outputColors, float hueTolerance, float brightnessTolerance) {
        for (int x = 0; x < width; x++) {

            for (int y = 0; y < height; y++) {
                boolean seedRegionPixel = true;
                seedRegionPixel &= isSameRegion(width, height, inputColors, x, y, x - 1, y, hueTolerance, brightnessTolerance);
                seedRegionPixel &= isSameRegion(width, height, inputColors, x, y, x, y - 1, hueTolerance, brightnessTolerance);
                seedRegionPixel &= isSameRegion(width, height, inputColors, x, y, x + 1, y, hueTolerance, brightnessTolerance);
                seedRegionPixel &= isSameRegion(width, height, inputColors, x, y, x, y + 1, hueTolerance, brightnessTolerance);

                if (seedRegionPixel) {
                    if (outputColors[x + y * width] == 0) {
                        outputColors[x + y * width] = inputColors[x + y * width];
                    }
                } else {
                    outputColors[x + y * width] = 0;
                    /*setColor(width, height, outputColors, x - 1, y, 0, 0, 0, 0);
                    setColor(width, height, outputColors, x, y - 1, 0, 0, 0, 0);
                    setColor(width, height, outputColors, x + 1, y, 0, 0, 0, 0);
                    setColor(width, height, outputColors, x, y + 1, 0, 0, 0, 0);*/

                    /*setColor(width, height, outputColors, x - 1, y - 1, 0, 0, 0);
                    setColor(width, height, outputColors, x + 1, y - 1, 0, 0, 0);
                    setColor(width, height, outputColors, x - 1, y + 1, 0, 0, 0);
                    setColor(width, height, outputColors, x + 1, y + 1, 0, 0, 0);*/
                }
            }
        }
    }

    private static void setColor(int width, int height, int[] outputColors, int x, int y, float red, float green, float blue, float alpha) {
        int i = x + y * width;
        if (i >= 0 && i < width * height) {
            int outputColor = (int) alpha;
            outputColor = (outputColor << 8) + (int) (red);
            outputColor = (outputColor << 8) + (int) (green);
            outputColor = (outputColor << 8) + (int) (blue);
            outputColors[i] = outputColor;
        }
    }

    private static boolean isSameRegion(int width, int height, int[] inputColors, int x0, int y0, int x, int y, float hueTolerance, float brightnessTolerance) {
        int i0 = x0 + y0 * width;
        int i = x + y * width;
        if (i >= 0 && i < width * height) {
            int inputColor0 = inputColors[i0];
            float red0 = (inputColor0 >> 16) & 0xff;
            float green0 = (inputColor0 >> 8) & 0xff;
            float blue0 = (inputColor0 >> 0) & 0xff;
            float average0 = (red0 + green0 + blue0) / 3;
            float brightness0 = Math.max(Math.max(red0, green0), blue0);
            float redHue0 = red0 / brightness0;
            float greenHue0 = green0 / brightness0;
            float blueHue0 = blue0 / brightness0;

            int inputColor = inputColors[i];
            float red = (inputColor >> 16) & 0xff;
            float green = (inputColor >> 8) & 0xff;
            float blue = (inputColor >> 0) & 0xff;
            float average = (red + green + blue) / 3;
            float brightness = Math.max(Math.max(red, green), blue);
            float redHue = red / brightness;
            float greenHue = green / brightness;
            float blueHue = blue / brightness;

            float hueDelta = (Math.abs(redHue - redHue0) + Math.abs(greenHue - greenHue0) + Math.abs(blueHue - blueHue0)) / 3;
            float brightnessDelta = Math.abs(brightness - brightness0) / 255f;

            if (brightness0 > 10 && brightness > 10) {
                if (hueDelta < hueTolerance && brightnessDelta < brightnessTolerance) {
                    return true;
                } else {
                    return false;
                }
            } else if (brightness0 <= 10 && brightness <= 10) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    private static void gradient(int width, int height, int[] inputColors, float[] gradient) {
        for (int x = 0; x < width; x++) {

            for (int y = 0; y < height; y++) {

                final ColorSum colorSum = new ColorSum();

                gradientSample(width, height, inputColors, x, y, x + 1, y, colorSum, 1f);
                gradientSample(width, height, inputColors, x, y, x, y + 1, colorSum, 1f);
                gradientSample(width, height, inputColors, x, y, x + 1, y + 1, colorSum, 1f);
                gradientSample(width, height, inputColors, x, y, x - 1, y + 1, colorSum, 1f);
                gradientSample(width, height, inputColors, x, y, x + 1, y - 1, colorSum, 1f);
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

                gradient[x + y * width] = sum;
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
