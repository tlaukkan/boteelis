package boteelis.vision;

import java.awt.*;

import com.sun.j3d.utils.universe.*;

import javax.media.j3d.*;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.vecmath.Color3f;
import javax.vecmath.Point3f;

final class Panel3d extends JPanel {

    private TransformGroup transformGroup;
    private BranchGroup lineGroup;
    private SimpleUniverse universe;

    public Panel3d() {
        setLayout(new BorderLayout());
        GraphicsConfiguration gc=SimpleUniverse.getPreferredConfiguration();
        Canvas3D canvas3D = new Canvas3D(gc);
        add(BorderLayout.CENTER, canvas3D);

        universe = new SimpleUniverse(canvas3D);
        universe.getViewingPlatform().setNominalViewingTransform();

        transformGroup = new TransformGroup();
    }

    public void addPoint(Point3f point3f, Color3f colorf) {
        Point3f[] plaPts = new Point3f[] {point3f};
        PointArray pla = new PointArray(1, GeometryArray.COORDINATES);
        pla.setCoordinates(0, plaPts);
        Appearance app = new Appearance();
        ColoringAttributes ca = new ColoringAttributes(colorf, ColoringAttributes.SHADE_FLAT);
        app.setColoringAttributes(ca);
        Shape3D plShape = new Shape3D(pla, app);
        transformGroup.addChild(plShape);
    }

    public void drawPoints() {
        if (lineGroup != null) {
            lineGroup.detach();
        }
        lineGroup = new BranchGroup();
        lineGroup.setCapability(BranchGroup.ALLOW_DETACH) ;
        transformGroup.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        lineGroup.addChild(transformGroup);
        universe.addBranchGraph(lineGroup);
        transformGroup = new TransformGroup();
        repaint();
    }

    public static void main(String[] args) {
        Panel3d example3D = new Panel3d();

        JFrame frame = new JFrame();
        frame.add(example3D);
        frame.setSize(600, 300);
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        for (int i = 0; i < 360; i++) {
            example3D.addPoint(new Point3f((float) (Math.cos(i / 180f * Math.PI) * 0.5f),
                    (float) (Math.sin(i / 180f * Math.PI) * 0.5f),0.0f), new Color3f(255f, 0f, 0f));
            example3D.addPoint(new Point3f(-0.1f,-0.1f,0.0f), new Color3f(0, 0f, 255f));
            example3D.drawPoints();

            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
            }
        }
    }
}