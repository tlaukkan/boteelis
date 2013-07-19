package boteelis.vision;

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
        BufferedImage inputImage = ImageStorage.readImage("src/test/resources/vision/lion_right.png");

        int width = inputImage.getWidth();
        int height = inputImage.getHeight();

        BufferedImage outputImage = new BufferedImage(inputImage.getWidth(),inputImage.getHeight(),BufferedImage.TYPE_INT_ARGB);

        long startTimeMillis = System.currentTimeMillis();

        int[] inputColors = ((DataBufferInt) inputImage.getRaster().getDataBuffer()).getData();
        int[] smoothedColors = new int[width*height];
        int[] regionColors = new int[width*height];
        int[] outputColors = ((DataBufferInt) outputImage.getRaster().getDataBuffer()).getData();
        float[] gradient = new float[width*height];

        //Watersheding.smooth(width, height, inputColors, smoothedColors);
        Watersheding.gradient(width, height, inputColors, gradient);
        Watersheding.watershed(width, height, inputColors, gradient, regionColors);

        final LinkedList<Region> regions = new LinkedList<Region>();
        final Map<Integer, Region> indexRegionMap = new HashMap<Integer, Region>();

        analyzeRegions(width, height, inputColors, regionColors, regions, indexRegionMap, 0.01f, 0.01f);
        //horizontalGradient(width, height, regions, gradient);
        //renderGradient(width, height, gradient, outputColors);
        //fillRegions(width, height, regions, outputColors, 0.005f, 0.1f);
        System.out.println("Manipulation took: " + (System.currentTimeMillis() -  startTimeMillis) + "ms.");
        System.out.println("Regions: " + regions.size());

        /*List<Region> removedRegions = new ArrayList<Region>();
        // Optimizing regions
        for (Region region : regions) {

            if (region.indexes.size() < 3) {

                Region closestNeighour = null;
                float closestColorDistance = Float.MAX_VALUE;
                for (Region neighbour : region.neighbours) {
                    neighbour.neighbours.remove(region);
                    float colorDistance = colorDistance(region.red, region.green, region.blue, neighbour.red, neighbour.blue, neighbour.green, 0.01f, 0.3f);
                    if (closestNeighour == null || colorDistance < closestColorDistance) {
                        closestColorDistance = colorDistance;
                        closestNeighour = neighbour;
                    }
                }

                if (closestNeighour != null) {
                    removedRegions.add(region);
                    for (int index : region.indexes) {
                            indexRegionMap.put(index, closestNeighour);
                            closestNeighour.indexes.add(index);
                    }
                    for (Region neighbour : region.neighbours) {
                        if (closestNeighour != neighbour) {
                            neighbour.neighbours.add(closestNeighour);
                            closestNeighour.neighbours.add(neighbour);
                        }
                    }
                }

            }
        }

        for (final Region removedRegion : removedRegions) {
            regions.remove(removedRegion);
        }

        System.out.println("Optimized regions: " + regions.size());*/

        renderRegions(width, height,indexRegionMap, outputColors);

        ImageStorage.writeImage("target/lion.png", outputImage);
    }

    public static float colorDistance(float red0, float green0, float blue0, float red, float green, float blue, float hueTolerance, float brightnessTolerance) {
        float brightness0 = Math.max(Math.max(red0, green0), blue0);
        float redHue0 = red0 / brightness0;
        float greenHue0 = green0 / brightness0;
        float blueHue0 = blue0 / brightness0;

        float brightness = Math.max(Math.max(red, green), blue);
        float redHue = red / brightness;
        float greenHue = green / brightness;
        float blueHue = blue / brightness;

        float hueDelta = (Math.abs(redHue - redHue0) + Math.abs(greenHue - greenHue0) + Math.abs(blueHue - blueHue0)) / 3;
        float brightnessDelta = Math.abs(brightness - brightness0) / 255f;

        return hueDelta / hueTolerance + brightnessDelta / brightnessTolerance;
    }


    private static void renderRegions(int width, int height, Map<Integer, Region> indexRegionMap, int[] outputColors) {
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int i = x + y * width;
                Region region = indexRegionMap.get(i);
                if (region != null) {
                    int color = (int) 255;
                    color = (color << 8) + (int) region.red;
                    color = (color << 8) + (int) region.green;
                    color = (color << 8) + (int) region.blue;
                    outputColors[i] = color;
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

        while (indexes.size() > 0) {
            IndexPair indexPair = indexes.pop();
            int lastIndex = indexPair.source;
            int currentIndex = indexPair.target;
            int x = currentIndex % width;
            int y = (currentIndex -  x) / width;
            if (!indexRegionMap.containsKey(currentIndex) && x > -1 && x < width && y > -1 && y < height) {
                //if (regionColor == regionColors[currentIndex]) {
                if (Watersheding.isSameRegion(regionColor, regionColors[currentIndex], hueTolerance, brightnessTolerance)) {

                    int size = region.indexes.size();
                    region.x = (region.x * size + x) / (size + 1);
                    region.y = (region.y * size + y) / (size + 1);
                    region.red = (region.red * size + ((regionColors[currentIndex] >> 16) & 0xff)) / (size + 1);
                    region.green = (region.green * size + ((regionColors[currentIndex] >> 8) & 0xff)) / (size + 1);
                    region.blue = (region.blue * size + ((regionColors[currentIndex] >> 0) & 0xff)) / (size + 1);

                    region.indexes.add(currentIndex);
                    indexRegionMap.put(currentIndex, region);

                    if (x > 0 && x < width - 1 && y > 0 && y < height - 1) {
                        pushIndex(width, height, currentIndex, x + 1, y, region, indexRegionMap, indexes, analyzed);
                        pushIndex(width, height, currentIndex, x - 1, y, region, indexRegionMap, indexes, analyzed);
                        pushIndex(width, height, currentIndex, x, y + 1, region, indexRegionMap, indexes, analyzed);
                        pushIndex(width, height, currentIndex, x, y - 1, region, indexRegionMap, indexes, analyzed);
                    }
                } else {
                    analyzed[currentIndex] = false;
                }
            }
        }

        /*if (region.indexes.size() <= 1) {
            regions.remove(region);
            for (int index : region.indexes) {
                indexRegionMap.remove(index);
            }
        }*/
    }

    private static void pushIndex(int width, int height, int currentIndex, int x, int y, Region region, Map<Integer, Region> indexRegionMap,  LinkedList<IndexPair> indexes, boolean[] analyzed) {
            int nextIndex = x + y * width;
            if (analyzed[nextIndex]) {
                return;
            }
            //if (!indexRegionMap.containsKey(nextIndex)) {
                analyzed[nextIndex] = true;
                indexes.push(new IndexPair(currentIndex, nextIndex));
            /*} else {
                Region peerRegion = indexRegionMap.get(nextIndex);
                if (region != peerRegion) {
                    region.neighbours.add(peerRegion);
                    peerRegion.neighbours.add(region);
                }
            }*/
    }

    private static class Region {
        public float x;
        public float y;

        public float red;
        public float green;
        public float blue;

        public float stereoDeltaX;
        public float stereoDeltaY;
        public float stereoCorrelation;

        Set<Integer> indexes = new HashSet<Integer>();
        Set<Region> neighbours = new HashSet<Region>();
    }

}
