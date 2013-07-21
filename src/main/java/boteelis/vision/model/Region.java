package boteelis.vision.model;

import java.util.HashSet;
import java.util.Set;

/**
* Created with IntelliJ IDEA.
* User: tlaukkan
* Date: 20.7.2013
* Time: 10:54
* To change this template use File | Settings | File Templates.
*/
public class Region {
    public float x;
    public float y;

    public float red;
    public float green;
    public float blue;

    public int stereoCorrelationDeltaX;
    public float stereoCorrelation;

    public Set<Integer> indexes = new HashSet<Integer>();
    public Set<Integer> boundaryIndexes = new HashSet<Integer>();
    public Set<Region> neighbours = new HashSet<Region>();
}
