package boteelis.vision.algorithms;

import com.github.sarxos.webcam.Webcam;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.io.File;

/**
 * Created with IntelliJ IDEA.
 * User: tlaukkan
 * Date: 19.7.2013
 * Time: 22:38
 * To change this template use File | Settings | File Templates.
 */
public class StereoCapture {

    public static void main(String[] args) throws Exception {
        int width = 640;
        int height = 480;
        JFrame frame = new JFrame();
        frame.setSize(2 * width, height);
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

        frame.setVisible(true);

        Webcam webcam1 = Webcam.getWebcams().get(0);
        Webcam webcam2 = Webcam.getWebcams().get(1);
        webcam1.setViewSize(new Dimension(width, height));
        webcam1.open();
        webcam2.setViewSize(new Dimension(width, height));
        webcam2.open();

        for (int i = 0; i < 10; i++) {
            long startTimeMillis = System.currentTimeMillis();
            frame.getContentPane().getGraphics().drawImage(webcam1.getImage(), 0, 0, null);
            frame.getContentPane().getGraphics().drawImage(webcam2.getImage(), width, 0, null);
            System.out.println((System.currentTimeMillis() - startTimeMillis) + " ms.");
        }

        webcam1.close();
        webcam2.close();
    }
}
