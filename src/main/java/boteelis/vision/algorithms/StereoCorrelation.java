package boteelis.vision.algorithms;

import boteelis.vision.model.IndexPair;
import boteelis.vision.model.Region;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.IOException;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: tlaukkan
 * Date: 19.7.2013
 * Time: 12:57
 * To change this template use File | Settings | File Templates.
 */
public class StereoCorrelation {

    public static void main(final String[] args) throws IOException {
        BufferedImage leftImage = ImageStorage.readImage("src/test/resources/vision/lion_left.png");
        BufferedImage rightImage = ImageStorage.readImage("src/test/resources/vision/lion_right.png");

        BufferedImage outputImage = new BufferedImage(rightImage.getWidth(),rightImage.getHeight(),BufferedImage.TYPE_INT_ARGB);

        int width = rightImage.getWidth();
        int height = rightImage.getHeight();


        long startTimeMillis = System.currentTimeMillis();

        int[] leftColors = ((DataBufferInt) leftImage.getRaster().getDataBuffer()).getData();
        int[] rightColors = ((DataBufferInt) rightImage.getRaster().getDataBuffer()).getData();
        int[] regionColors = new int[width*height];
        int[] outputColors = ((DataBufferInt) outputImage.getRaster().getDataBuffer()).getData();
        float[] gradient = new float[width*height];

        Watersheding.gradient(width, height, rightColors, gradient);
        Watersheding.watershed(width, height, rightColors, gradient, regionColors);

        final LinkedList<Region> regions = new LinkedList<Region>();
        final Map<Integer, Region> indexRegionMap = new HashMap<Integer, Region>();

        analyzeRegions(width, height, rightColors, regionColors, regions, indexRegionMap, 0.015f, 0.015f);
        correlateRegions(width, height, leftColors, rightColors, regions);

        System.out.println("Manipulation took: " + (System.currentTimeMillis() -  startTimeMillis) + "ms.");
        System.out.println("Regions: " + regions.size());

        renderRegions(width, height,indexRegionMap, leftColors, outputColors);

        ImageStorage.writeImage("target/lion.png", outputImage);
    }

    public static void correlateRegions(int width, int height, int[] leftColors, int[] rightColors, LinkedList<Region> regions) {
        int maxDx = width / 2;
        for (Region region : regions) {
            float minCorrelation = Float.MAX_VALUE;
            float maxCorrelation = - Float.MAX_VALUE;
            int maxCorrelationDx = -1;

            for (int dx = 0; dx < maxDx; dx++) {
                float correlation = 0;
                for (int i : region.indexes) {
                    int rx = i % width;
                    int lx = rx - dx;
                    if (lx >= 0) {
                        correlation += colorCorrelation(leftColors[i - dx], rightColors[i]);
                    }
                }
                if (correlation > maxCorrelation) {
                    maxCorrelation = correlation;
                    maxCorrelationDx = dx;
                }
                if (correlation < minCorrelation) {
                    minCorrelation = correlation;
                }
            }

            region.stereoCorrelationDeltaX = maxCorrelationDx;
            region.stereoCorrelation = maxCorrelation / region.indexes.size();

            // base distance between cams ~ 3.75 cm
            float distanceBetweenCamsInMeters = 0.0375f;
            // focal length of cams ~ 4mm
            float focalLengthInMeters = 0.004f;
            // field of view in radians
            float fov = (float) (2 * Math.PI * 60f / 360f);
            float sensorWidthInPixels = Math.max(width, height);
            // meters per pixel
            float pixelWidthInMeters = (float) (2* (Math.sin(fov / 2) * focalLengthInMeters / Math.cos(fov / 2)) / sensorWidthInPixels);
            if (maxCorrelationDx == 0) {
                region.rx = 0;
                region.ry = 0;
                region.rz = Float.MAX_VALUE;
            } else {
                region.rz = distanceBetweenCamsInMeters * focalLengthInMeters / (pixelWidthInMeters * Math.abs(maxCorrelationDx));
                region.rx = (pixelWidthInMeters * (region.x - width / 2)) * region.rz / focalLengthInMeters;
                region.ry = (pixelWidthInMeters * (- (region.y - height / 2))) * region.rz / focalLengthInMeters;
            }
        }
    }

    public static float colorCorrelation(int color0, int color1) {
        if (color0 == color1) {
            return 1;
        }
        float red0 = (color0 >> 16) & 0xff;;
        float green0 = (color0 >> 8) & 0xff;;
        float blue0 = (color0 >> 0) & 0xff;;

        float red1 = (color1 >> 16) & 0xff;
        float green1 = (color1 >> 8) & 0xff;
        float blue1 = (color1 >> 0) & 0xff;

        return 1 - (Math.abs(red0 - red1) + Math.abs(green0 - green1) + Math.abs(blue0 - blue1)) / (255f * 3f);
    }

    public static void renderRegions(int width, int height, Map<Integer, Region> indexRegionMap, int[] leftColors, int[] outputColors) {
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int i = x + y * width;
                Region region = indexRegionMap.get(i);
                if (region != null) {
                    /*int color = (int) 255;
                    if (region.boundaryIndexes.contains(i)) {
                        color = (color << 8) + (int) (2 * region.stereoCorrelationDeltaX);
                        color = (color << 8) + (int) (0);
                        color = (color << 8) + (int) (0);
                    } else {
                        color = (color << 8) + (int) region.red;
                        color = (color << 8) + (int) region.green;
                        color = (color << 8) + (int) region.blue;
                    }
                    outputColors[i] = color;
                    */
                    int rx = (i - region.stereoCorrelationDeltaX) % width;
                    if (rx >= 0 && region.stereoCorrelation > 0.95f) {
                        outputColors[i] = leftColors[i - region.stereoCorrelationDeltaX];
                    } else {
                        int color = (int) 255;
                        color = (color << 8) + (int) (0);
                        color = (color << 8) + (int) (0);
                        color = (color << 8) + (int) (255);
                        outputColors[i] = color;
                    }
                }
            }
        }
    }

    public static void analyzeRegions(int width, int height, int[] inputColors, int[] regionColors, List<Region> regions, Map<Integer, Region> indexRegionMap, float hueTolerance, float brightnessTolerance) {

        final boolean[] analyzed = new boolean[width * height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int i = x + y * width;
                if (!analyzed[i]) {
                    analyzeRegion(width, height, inputColors, regionColors, x, y, regions, indexRegionMap, analyzed, hueTolerance, brightnessTolerance);
                }
            }
        }
    }

    private static void analyzeRegion(int width, int height, int[] inputColors, int[] regionColors, int x0, int y0, List<Region> regions, Map<Integer, Region> indexRegionMap, boolean[] analyzed, float hueTolerance, float brightnessTolerance) {
        int i0 = x0 + y0 * width;

        int regionColor = regionColors[i0];

        Region region = new Region();
        regions.add(region);

        final LinkedList<IndexPair> indexes = new LinkedList<IndexPair>();
        indexes.push(new IndexPair(i0,i0));
        analyzed[i0] = true;

        int n = 1;
        while (indexes.size() > 0) {
            IndexPair indexPair = indexes.pop();
            int lastIndex = indexPair.source;
            int currentIndex = indexPair.target;
            int x = currentIndex % width;
            int y = (currentIndex -  x) / width;
            if (!indexRegionMap.containsKey(currentIndex) && x > -1 && x < width && y > -1 && y < height) {
                if (regionColor == regionColors[currentIndex]) {
                //if (Watersheding.isSameRegion(regionColor, regionColors[currentIndex], hueTolerance, brightnessTolerance)) {

                    int size = region.indexes.size();
                    region.x = (region.x * size + x) / (size + 1);
                    region.y = (region.y * size + y) / (size + 1);
                    region.red = (region.red * size + ((regionColors[currentIndex] >> 16) & 0xff)) / (size + 1);
                    region.green = (region.green * size + ((regionColors[currentIndex] >> 8) & 0xff)) / (size + 1);
                    region.blue = (region.blue * size + ((regionColors[currentIndex] >> 0) & 0xff)) / (size + 1);

                    region.indexes.add(currentIndex);
                    indexRegionMap.put(currentIndex, region);

                    if (n < 100) { // limit region size to ~30 core pixels + region pixels
                        n++;
                        if (x > 0 && x < width - 1 && y > 0 && y < height - 1) {
                            pushIndex(width, height, currentIndex, x + 1, y, region, indexRegionMap, indexes, analyzed);
                            pushIndex(width, height, currentIndex, x - 1, y, region, indexRegionMap, indexes, analyzed);
                            pushIndex(width, height, currentIndex, x, y + 1, region, indexRegionMap, indexes, analyzed);
                            pushIndex(width, height, currentIndex, x, y - 1, region, indexRegionMap, indexes, analyzed);
                        }
                    }
                } else {
                    analyzed[currentIndex] = false;
                }
            }
        }
    }

    private static void pushIndex(int width, int height, int currentIndex, int x, int y, Region region, Map<Integer, Region> indexRegionMap,  LinkedList<IndexPair> indexes, boolean[] analyzed) {
            int nextIndex = x + y * width;
            if (analyzed[nextIndex]) {
                Region peerRegion = indexRegionMap.get(nextIndex);
                if (peerRegion != null && region != peerRegion) {
                    region.boundaryIndexes.add(currentIndex);
                    region.neighbours.add(peerRegion);
                    peerRegion.neighbours.add(region);
                    peerRegion.boundaryIndexes.add(nextIndex);
                }
                return;
            }

            analyzed[nextIndex] = true;
            indexes.push(new IndexPair(currentIndex, nextIndex));
    }

}
