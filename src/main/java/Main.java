import java.util.List;
import java.util.Vector;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import edu.wpi.cscore.CvSink;
import edu.wpi.cscore.CvSource;
import edu.wpi.cscore.MjpegServer;
import edu.wpi.cscore.UsbCamera;
import edu.wpi.cscore.VideoMode;
import edu.wpi.first.wpilibj.networktables.NetworkTable;

public class Main {
	public static void main(String[] args) {
		// Loads our OpenCV library. This MUST be included
		System.loadLibrary("opencv_java310");

		// Connect NetworkTables, and get access to the publishing table
		NetworkTable.setClientMode();
		//NetworkTable.setTeam(1675);
		NetworkTable.setIPAddress("169.254.120.85");
		NetworkTable.initialize();

		// This gets the image from a USB camera.
		// Usually this will be on device 0
		UsbCamera camera = new UsbCamera("CoprocessorCamera", 0);
		camera.setResolution(320, 240);

		// This grabs images from our camera for use in opencv
		CvSink imageSink = new CvSink("CV Image Grabber");
		imageSink.setSource(camera);

		// This will take in a Mat image that has had OpenCV operations
		// operations
		CvSource imageSource = new CvSource("CV Image Source", VideoMode.PixelFormat.kMJPEG, 640, 480, 30);

		// Configure input and output streams
		// By rules, ports must be between 1180 and 1190
		MjpegServer inputStream = new MjpegServer("Input Stream", 1185);
		MjpegServer outputStream = new MjpegServer("Output Stream", 1186);
		// Link stream objects to their sources
		inputStream.setSource(camera);
		outputStream.setSource(imageSource);

		// All Mats and Lists should be stored outside the loop to avoid
		// allocations as they are expensive to create
		Mat inputImage = new Mat();
		Mat hsv = new Mat();		
		Mat thresh = new Mat();
		Mat contourImg = new Mat();
		// HSV Threshold values		 (H, S, V)
		Scalar hsvLowerb = new Scalar(55, 	0, 10);
		Scalar hsvUpperb = new Scalar(65, 255, 255);
		List<MatOfPoint> contours = new Vector<MatOfPoint>();
		Mat hierarchy = new Mat();

		// Infinitely process image
		long referenceTime = System.currentTimeMillis();
		double totalTimeSeconds = 0.0;
		int frames = 0;
		while (true) {
			// Grab a frame. If it has a frame time of 0, there was an error.
			// Just skip and continue
			long frameTime = imageSink.grabFrame(inputImage);
			if (frameTime == 0) {
				System.out.println("Error grabbing frame");
				continue;
			}
			
			//inputImage = Imgcodecs.imread("/home/pi/vision1675-2017/imgs/target.jpg");

			// OpenCV Processing operations
			Imgproc.cvtColor(inputImage, hsv, Imgproc.COLOR_BGR2HSV);			
			Core.inRange(hsv, hsvLowerb, hsvUpperb, thresh);			
			Imgproc.findContours(thresh, contours, hierarchy , Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_NONE);
			
//			for(int contourIdx = 0; contourIdx < contours.size(); contourIdx++){
//				Imgproc.drawContours(contourImg, contours, contourIdx, new Scalar(255, 0 ,0));
//				Imgcodecs.imwrite("/home/pi/vision1675-2017/imgs/contour"+contourIdx+".jpg", contourImg);
//			}

			// Stream the processed image
			imageSource.putFrame(thresh);

			//Imgcodecs.imwrite("/home/pi/vision1675-2017/imgs/input.jpg", inputImage);
			//Imgcodecs.imwrite("/home/pi/vision1675-2017/imgs/output.jpg", thresh);

			frames++;
			double elapsedTimeMilliSeconds = (System.currentTimeMillis() - referenceTime);
			referenceTime = System.currentTimeMillis();
			totalTimeSeconds += elapsedTimeMilliSeconds / 1000;
			double fps = frames / totalTimeSeconds;
			// System.out.println("Current FPS: "+fps);
			System.out.println("Elapsed Time: " + elapsedTimeMilliSeconds);
		}
	}
}