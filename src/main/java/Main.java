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
		NetworkTable.setIPAddress("169.254.153.78");
		NetworkTable.initialize();
		NetworkTable rootTable = NetworkTable.getTable("Root");

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
		List<MatOfPoint> contours = new Vector<MatOfPoint>();
		Mat hierarchy = new Mat();

		// Infinitely process image
		long referenceTime = System.currentTimeMillis();
		double totalTimeSeconds = 0.0;
		int frames = 0;
		
		//default HSV values
		int hueLow = 59;
		int hueHigh = 60;
		int saturationLow = 0;
		int saturationHigh = 255;
		int valueLow = 30;
		int valueHigh = 255;
		
		NetworkTable hsvTable = NetworkTable.getTable("HSV");
		
		hsvTable.putNumber("hueLow", hueLow);
		hsvTable.putNumber("hueHigh", hueHigh);
		hsvTable.putNumber("saturationLow", saturationLow);
		hsvTable.putNumber("saturationHigh", saturationHigh);
		hsvTable.putNumber("valueLow", valueLow);
		hsvTable.putNumber("valueHigh", valueHigh);		
		
		while (true) {
			// Grab a frame. If it has a frame time of 0, there was an error.
			// Just skip and continue
			long frameTime = imageSink.grabFrame(inputImage);
			if (frameTime == 0) {
				System.out.println("Error grabbing frame");
				continue;
			}
			
			//inputImage = Imgcodecs.imread("/home/pi/vision1675-2017/imgs/target.jpg");
			
			hueLow = (int) hsvTable.getNumber("hueLow", hueLow);
			hueHigh = (int) hsvTable.getNumber("hueHigh", hueHigh);
			saturationLow = (int) hsvTable.getNumber("saturationLow", saturationLow);
			saturationHigh = (int) hsvTable.getNumber("saturationHigh", saturationHigh);
			valueLow = (int) hsvTable.getNumber("valueLow", valueLow);
			valueHigh = (int) hsvTable.getNumber("valueHigh", valueHigh);	
			
			// HSV Threshold values		 (H, S, V)
			Scalar hsvLowerb = new Scalar(hueLow, saturationLow, valueLow);
			Scalar hsvUpperb = new Scalar(hueHigh, saturationHigh, valueHigh);
			
			// OpenCV Processing operations
			Imgproc.cvtColor(inputImage, hsv, Imgproc.COLOR_BGR2HSV);			
			Core.inRange(hsv, hsvLowerb, hsvUpperb, thresh);			
//			Imgproc.findContours(thresh, contours, hierarchy , Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_NONE);
//			^^^This was causing an issue
			
//			for(int contourIdx = 0; contourIdx < contours.size(); contourIdx++){
//				Imgproc.drawContours(contourImg, contours, contourIdx, new Scalar(255, 0 ,0));
//				Imgcodecs.imwrite("/home/pi/vision1675-2017/imgs/contour"+contourIdx+".jpg", contourImg);
//			}

			// Stream the processed image
			imageSource.putFrame(thresh);

			//Imgcodecs.imwrite("/home/pi/vision1675-2017/imgs/input.jpg", inputImage);
//			Imgcodecs.imwrite("/home/pi/vision1675-2017/imgs/output.jpg", thresh);

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