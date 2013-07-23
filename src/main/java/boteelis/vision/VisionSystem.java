package boteelis.vision;

import boteelis.vision.model.VisionContext;
import org.apache.log4j.xml.DOMConfigurator;

import javax.swing.*;
import java.awt.*;

/**
 * Created with IntelliJ IDEA.
 * User: tlaukkan
 * Date: 20.7.2013
 * Time: 17:34
 * To change this template use File | Settings | File Templates.
 */
public class VisionSystem {

    private VisionContext visionContext;

    private CaptureComponent captureComponent;
    private AnalysisComponent analysisComponent;
    private VisualizationComponent visualizationComponent;

    private VisionSystem(int width, int height) {
        visionContext = new VisionContext(width, height, true);
        captureComponent = new CaptureComponent(visionContext);
        analysisComponent = new AnalysisComponent(visionContext);
        visualizationComponent = new VisualizationComponent(visionContext);
    }

    public void startup() {
        try {
            captureComponent.startup();
            analysisComponent.startup();
            visualizationComponent.startup();
        } catch (final Exception e) {
            throw new RuntimeException("Failed to startup vision system", e);
        }
    }

    public void shutdown() {
        try {
            captureComponent.shutdown();
            analysisComponent.shutdown();
            visualizationComponent.shutdown();
        } catch (final Exception e) {
            throw new RuntimeException("Error in vision system shutdown.", e);
        }
    }

    public VisionContext getVisionContext() {
        return visionContext;
    }

    public VisualizationComponent getVisualizationComponent() {
        return visualizationComponent;
    }

    public static VisionSystem getVisionSystem() {
        return new VisionSystem(320, 240);
    }

    public static void main(String[] args) throws Exception {

        DOMConfigurator.configure("log4j.xml");

        final VisionSystem visionSystem = VisionSystem.getVisionSystem();
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                visionSystem.shutdown();
            }
        });

        int width = visionSystem.getVisionContext().width;
        int height = visionSystem.getVisionContext().height;
        JFrame frame = new JFrame();
        frame.setSize(4 * width, 2 * height);
        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        frame.setLocation(dim.width / 2 - frame.getSize().width / 2, dim.height / 2 - frame.getSize().height / 2);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setTitle("Stereo Capture");

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }

        SwingUtilities.updateComponentTreeUI(frame);

        final GridLayout layout = new GridLayout(2, 1);
        frame.getContentPane().setLayout(layout);
        frame.getContentPane().add(visionSystem.getVisualizationComponent().getPanel());
        frame.getContentPane().add(visionSystem.getVisualizationComponent().getPanel3d());
        frame.setVisible(true);

        visionSystem.startup();

    }

}
