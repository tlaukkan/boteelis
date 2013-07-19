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
        int[] regionColors = new int[width*height];
        int[] outputColors = ((DataBufferInt) outputImage.getRaster().getDataBuffer()).getData();
        float[] gradient = new float[width*height];

        //smooth(width, height, inputColors, smoothed);
        Watersheding.gradient(width, height, inputColors, gradient);
        Watersheding.watershed(width, height, inputColors, gradient, regionColors);

        final List<Region> regions = new ArrayList<Region>();
        final Map<Integer, Region> indexRegionMap = new HashMap<Integer, Region>();

        analyzeRegions(width, height, inputColors, regionColors, regions, indexRegionMap);
        //horizontalGradient(width, height, regions, gradient);
        //renderGradient(width, height, gradient, outputColors);
        //fillRegions(width, height, regions, outputColors, 0.005f, 0.1f);

        System.out.println("Manipulation took: " + (System.currentTimeMillis() -  startTimeMillis) + "ms.");
        System.out.println("Regions: " + regions.size());

        //ImageStorage.writeImage("target/lion.png", outputImage);
    }

    public static void analyzeRegions(int width, int height, int[] inputColors, int[] regionColors, List<Region> regions, Map<Integer, Region> indexRegionMap) {

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int i = x + y * width;
                if (!indexRegionMap.containsKey(i)) {
                    analyzeRegion(width, height, inputColors, regionColors, x, y, regions, indexRegionMap);
                }
            }
        }
    }

    private static void analyzeRegion(int width, int height, int[] inputColors, int[] regionColors, int x0, int y0, List<Region> regions, Map<Integer, Region> indexRegionMap) {
        int i0 = x0 + y0 * width;
        int regionColor = regionColors[i0];

        Region region = new Region();
        regions.add(region);

        final LinkedList<Integer> indexes = new LinkedList<Integer>();
        indexes.push(i0);
        while (indexes.size() > 0) {
            int i = indexes.pop();
            int x = i % width;
            int y = (i -  x) / width;
            if (x > -1 && x < width && y > -1 && y < height) {
                if (regionColor == regionColors[i]) {
                    region.x += x;
                    region.y += y;

                    region.red += (inputColors[i] >> 16) & 0xff;
                    region.green += (inputColors[i] >> 8) & 0xff;
                    region.blue += (inputColors[i] >> 0) & 0xff;

                    region.indexes.add(i);

                    indexRegionMap.put(i, region);

                    pushIndex(width, height, x + 1, y, region, indexRegionMap, indexes);
                    pushIndex(width, height, x - 1, y, region, indexRegionMap, indexes);
                    pushIndex(width, height, y + 1, y, region, indexRegionMap, indexes);
                    pushIndex(width, height, y - 1, y, region, indexRegionMap, indexes);
                }
            }
        }

        int size = region.indexes.size();
        region.x /= size;
        region.y /= size;
        region.red /= size;
        region.green /= size;
        region.blue /= size;
    }

    private static void pushIndex(int width, int height, int x, int y, Region region, Map<Integer, Region> indexRegionMap,  LinkedList<Integer> indexes) {
        if (x > -1 && x < width && y > -1 && y < height) {
            int i = x + y * width;
            if (!indexRegionMap.containsKey(i)) {
                    indexes.push(i);
            } else {
                Region peerRegion = indexRegionMap.get(i);
                region.neighbours.add(peerRegion);
                peerRegion.neighbours.add(region);
            }
        }
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
