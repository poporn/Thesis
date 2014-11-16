package hgcore.core;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

import javax.swing.JLabel;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.highgui.VideoCapture;

public class HG_Core extends Thread{
	private static final long serialVersionUID = 1L;
	
	private JLabel scene = new JLabel("Hello world!");
	
	BufferedImage image; 
	

	public HG_Core()	{
		super("Core");
		
		
	}//construct
	
	
	int count = 1;
	//main must be replaced with a run function after it becomes a Thread
	public void run()	{
		Mat webcam_image = new Mat();
		VideoCapture capture = new VideoCapture(0);
		
		while(true)	{
			System.out.println("Count " + " " + count++);
			capture.read(webcam_image);
			System.out.println("Frame Obtained");
			System.out.println("Captured Frame Width " + 
		    webcam_image.width() + " Height " + webcam_image.height());
			Core.flip(webcam_image, webcam_image, 1); // flip image
			image = matToBufferedImage(webcam_image);
		}//while
		
	}//main
	
	public BufferedImage getImage()	{
		return image;
	}//getImage()
	
	public boolean isfetching()	{
		return true;
	}
	
	public static BufferedImage matToBufferedImage(Mat m) { 
		int type = BufferedImage.TYPE_BYTE_GRAY;
        if ( m.channels() > 1 ) {
            type = BufferedImage.TYPE_3BYTE_BGR;
        }
        int bufferSize = m.channels()*m.cols()*m.rows();
        byte [] b = new byte[bufferSize];
        m.get(0,0,b); // get all the pixels
        BufferedImage image = new BufferedImage(m.cols(),m.rows(), type);
        final byte[] targetPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        System.arraycopy(b, 0, targetPixels, 0, b.length);  
        return image;

	}//metToBufferedImage
}//class