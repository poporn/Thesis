package hgcore.core;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JLabel;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Scalar;
import org.opencv.highgui.VideoCapture;
import org.opencv.imgproc.Imgproc;

public class HG_Core extends Thread{
	private static final long serialVersionUID = 1L;
	
	private JLabel scene = new JLabel("Hello world!");
	
	BufferedImage image; 
	

	public HG_Core()	{
		super("Core");
		
		
	}//construct
	
	
	double ro, go, bo;
	double[] rgbo;
	
	double[] srcPx;
	double[] mskPx;
	
	//temp tresholding
	private double tresh = 50;
	
	//main must be replaced with a run function after it becomes a Thread
	public void run()	{
		Mat webcam_image = new Mat();
		VideoCapture capture = new VideoCapture(0);
		Mat ground = new Mat();
		
		
		Mat model = new Mat();
		
		boolean sing = true;
		while(true)	{
			capture.read(webcam_image);
			System.out.println("Frame Captured: Width " + 
		    webcam_image.width() + " Height " + webcam_image.height());
			Core.flip(webcam_image, webcam_image, 1); // flip image
			
			//rgb to grayscale
			Mat nMz = webcam_image.clone();
		    Imgproc.cvtColor(webcam_image, nMz, Imgproc.COLOR_BGR2GRAY);
		     //rgb to grayscale
			//image = matToBufferedImage(nMz); // grayScale value
			
		    
		    Mat normal = webcam_image.clone();
		    
		    ground = webcam_image.clone();
		    //capturing a model image (one time only)
		    if(sing)	{
		    	capture.read(model);
		    	Core.flip(model, model, 1); // flip image
		    	sing = false;
		    }
		    
		    
		    /****************************************************************************************************
		    *                NON-ADAPTIVE BACKGROUND SUBTRACITON
		    *                                 START
		    ****************************************************************************************************/
		    
		    for (int i = 0; i < webcam_image.rows(); i++)
				for (int j = 0; j < webcam_image.cols(); j++)	{
					srcPx = ground.get(i,j);
					mskPx = model.get(i,j);
					double bp = mskPx[0];
					double gp = mskPx[1];
					double rp = mskPx[2];
					double bc = srcPx[0];
					double gc = srcPx[1];
					double rc = srcPx[2];
					
					double b = Math.abs(bp - bc);
					double g = Math.abs(gp - gc);
					double r = Math.abs(rp - rc);
					
					// masking
					if(b < tresh && g < tresh && r < tresh)
						ground.put(i, j, new double[]{ 0, 0, 0 });
					else //if(b > tresh && g > tresh && r > tresh)
						ground.put(i, j, new double[]{ 255, 255, 255 });
					//masking
					
				}//FOR
		    
		    /****************************************************************************************************
			    *                NON-ADAPTIVE BACKGROUND SUBTRACITON
			    *                                 START
			****************************************************************************************************/
		    
		    //Contour Definition
		    //defineContour(webcam_image, webcam_image);
		    //Imgproc.cvtColor(webcam_image, webcam_image, Imgproc.COLOR_BGR2GRAY);
		    
//		    Mat orig = webcam_image.clone();
//		    Imgproc.Canny(webcam_image, webcam_image, 1, 100);
//		    Imgproc.cvtColor(webcam_image, webcam_image, Imgproc.COLOR_GRAY2BGR);
//		    double[] hld;
//		    double[] hld2;
//		    for (int i = 0; i < webcam_image.rows(); i++)
//				for (int j = 0; j < webcam_image.cols(); j++)	{
//					hld = webcam_image.get(i, j);
//					hld2 = orig.get(i, j);
//					if(hld[0] == 255)
//						webcam_image.put(i, j, new double[]{0,0,255});
//					if(hld[0] == 0)
//						webcam_image.put(i, j, normal.get(i,j));
//				}//for j
		    
		    Imgproc.cvtColor(ground, ground, Imgproc.COLOR_BGR2GRAY);
		    Imgproc.Canny(ground, ground, 1, 100);
		    ArrayList<MatOfPoint> contours = new ArrayList<MatOfPoint>();
		    List<MatOfInt> hull = new ArrayList<MatOfInt>(contours.size());
		    Mat hierarchy = new Mat();
		    Imgproc.findContours(ground, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
		    
		    for(int i = 0; i < contours.size(); i++)
		    {
		    	Scalar color = new Scalar(255);
		    	//drawContours( webcam_image, contours, i, color, 2, 8, hierarchy, 0, Point() );
		    	//Imgproc.drawContours(webcam_image, contours, i, color);
		    	Imgproc.drawContours(ground, contours, i, color, 2);
		    }//for
		    
		    Imgproc.cvtColor(ground, ground, Imgproc.COLOR_GRAY2BGR);
		    double[] hldd;
		    for (int i = 0; i < webcam_image.rows(); i++)
				for (int j = 0; j < webcam_image.cols(); j++)	{
					hldd = ground.get(i,j);
//					if(hldd[0] == 0)
//						ground.put(i, j, normal.get(i,j));
				}
		    
		    
		    
			image = matToBufferedImage(ground); // normal BGR Output
			
		}//while
	}//main
	
	
	public void setThresh(double value)	{
		tresh = value;
	}//setThresh
	
	public double getThresh()	{
		return tresh;
	}//get thresh
	
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