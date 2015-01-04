package hgcore.core;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.imageio.ImageIO;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfInt4;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.highgui.VideoCapture;
import org.opencv.imgproc.Imgproc;

public class HG_Core extends Thread{
	private static final long serialVersionUID = 1L;
	
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
	private boolean flip = true;
	
	
	//main must be replaced with a run function after it becomes a Thread
	public void run()	{
		Mat webcam_image = new Mat();
		VideoCapture capture = new VideoCapture(0);
		Mat ground = new Mat();
		
		//temporary holder for cast objects
		BufferedImage img = null;
		try {
		    img = ImageIO.read(new File(getClass().getResource("/_0cast.png").getPath()));
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		byte[] data = ((DataBufferByte) img.getRaster().getDataBuffer()).getData();
		Mat obCast = new Mat(img.getHeight(), img.getWidth(), CvType.CV_8UC3);
		obCast.put(0, 0, data);
//		Mat obCast = new Mat();
		//end
		// problem with casting we get 0 size of image
		
		
		Mat model = new Mat();
		
		int castX = 0;
		int castY = 0;
		
		boolean sing = true;
		while(true)	{
			capture.read(webcam_image);
			System.out.println("Frame Captured: Width " + 
		    webcam_image.width() + " Height " + webcam_image.height());
			Core.flip(webcam_image, webcam_image, 1); // flip image
			
			//rgb to grayscale
			Mat nMz = webcam_image.clone();
		    Imgproc.cvtColor(webcam_image, nMz, Imgproc.COLOR_BGR2GRAY);
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
			    *                                 END
			****************************************************************************************************/
		    
		    
		    ArrayList<MatOfPoint> contours = new ArrayList<MatOfPoint>();
		    ArrayList<MatOfPoint> convexHullMatOfPointArrayList = new ArrayList<MatOfPoint>();
		    
		    //Hand gesture recognition
		    ground = CVHandRec(ground, contours, convexHullMatOfPointArrayList);
		    
	    
		    //grab obCast
		    Imgproc.cvtColor(ground, ground, Imgproc.COLOR_GRAY2BGR);
		    double[] obcc;
		    double[] gtc = new double[]{1,2,3};
		    
		    for(int i = 2; i < obCast.rows(); i++)	
		    	for(int j = 0; j < obCast.cols(); j++)	{
		    		obcc = obCast.get(i,j);
		    		try	{
		    		gtc = ground.get(i + castX,j + castY);
		    		}catch (Exception e){e.printStackTrace();}
		    		
		    		ground.put(i + castX, j + castY, obcc);
		    		
		    		try	{
			    		if(gtc[0] == 255) {
			    			castX++;
			    		}//if
		    		}catch (Exception e){System.out.println("limit reached!");}
		    	}//for
		    
		    //this makes the whole program delayed
		    //reveal true image
//		    Imgproc.cvtColor(ground, ground, Imgproc.COLOR_GRAY2BGR);
		    double[] hldd;
		    for (int i = 0; i < webcam_image.rows(); i++)
				for (int j = 0; j < webcam_image.cols(); j++)	{
					hldd = ground.get(i,j);
					if(hldd[0] == 0)
						ground.put(i, j, webcam_image.get(i,j));
				}
		    
		    /************************************************************
		     * *********************** DRAWING **************************
		     * *********************************************************/
		    //contour
		    ground = drawCG(ground, contours, new Scalar(0,0,255), 1);
		    
		    //convex hull
		    ground = drawCG(ground, convexHullMatOfPointArrayList, new Scalar(0,255,255), 1);
		    
		    //bounding box
		    ground = boundBox(convexHullMatOfPointArrayList, ground);
		    

		    
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
	}//isFetching
	
	public Mat CVHandRec(Mat src, ArrayList<MatOfPoint> contours, ArrayList<MatOfPoint> convexHullMatOfPointArrayList)	{
		//pre-processing img_op for cntr_def
	    Imgproc.cvtColor(src, src, Imgproc.COLOR_BGR2HSV);
	    Mat dest = new Mat();
	    Core.inRange(src, new Scalar(58,125,0), new Scalar(256,256,256), dest);
	    Mat erode = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3,3));
        Mat dilate = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(5,5));

        Imgproc.dilate(src, src, dilate);
        Imgproc.erode(src, src, erode);
        
	    //contour definition
	    Imgproc.cvtColor(src, src, Imgproc.COLOR_BGR2GRAY);
	    Imgproc.Canny(src, src, 1, 100);
	    
	    Mat hierarchy = new Mat();
	    Imgproc.findContours(src, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
	    
	    //convex hull
	    MatOfInt convexHullMatOfInt = new MatOfInt();
	    ArrayList<Point> convexHullPointArrayList = new ArrayList<Point>();
	    MatOfPoint convexHullMatOfPoint = new MatOfPoint();
	    ArrayList<MatOfInt> convexHullMatOfIntArrayList = new ArrayList();
	    
	    try {
	        //Calculate convex hulls
	        if(contours.size() > 0)	{
	            Imgproc.convexHull( contours.get(0), convexHullMatOfInt, false);

	            for(int j=0; j < convexHullMatOfInt.toList().size(); j++)
	                convexHullPointArrayList.add(contours.get(0).toList().get(convexHullMatOfInt.toList().get(j)));
	            convexHullMatOfPoint.fromList(convexHullPointArrayList);
	            convexHullMatOfPointArrayList.add(convexHullMatOfPoint);    
	            
	            MatOfInt4 cd_moi = new MatOfInt4();
			    Imgproc.convexityDefects(contours.get(0), convexHullMatOfInt, cd_moi);
				
				int start;
				int end;
				int depth_point;
				int farthest;
				
				java.util.List<Integer> list_DefectsList = cd_moi.toList();
				
				for(int i = 0; i < list_DefectsList.size(); i += 4)
				{
					start = list_DefectsList.get(i);
					end = list_DefectsList.get(i+1);
					depth_point = list_DefectsList.get(i+2);
					farthest = list_DefectsList.get(i+3);
					
					Core.circle(src, new Point(contours.get(0).get(start, 0)[0],contours.get(0).get(start, 0)[1]), 3, new Scalar(255,0,255), -1);
//					Core.circle(ground, new Point(contours.get(0).get(end, 0)[0],contours.get(0).get(end, 0)[1]), 3, new Scalar(123,0,255), -1);
					
					//System.out.println(depth_point);
					//System.out.println(farthest);
					//System.out.println(list_DefectsList.size());
					
				}//for
	        }//if
	    } catch (Exception e) {
	        // TODO Auto-generated catch block
	        System.out.println("Calculate convex hulls failed. Details below");
	        e.printStackTrace();
	    }
		
		return src;
	}//CVHandRed
	
	public Mat boundBox(ArrayList<MatOfPoint> srcMopList, Mat srcImg)	{
		MatOfPoint2f approxCurve = new MatOfPoint2f();
	    
	    for (int i=0; i<srcMopList.size(); i++)
	    {
	        MatOfPoint2f contour2f = new MatOfPoint2f( srcMopList.get(i).toArray() );
	        double approxDistance = Imgproc.arcLength(contour2f, true)*0.02;
	        Imgproc.approxPolyDP(contour2f, approxCurve, approxDistance, true);
	        
	        MatOfPoint points = new MatOfPoint( approxCurve.toArray() );

	        Rect rect = Imgproc.boundingRect(points);

	        Core.rectangle(srcImg, new Point(rect.x,rect.y), new Point(rect.x+rect.width,rect.y+rect.height), new Scalar(0,255,0), 1);
	        Core.circle(srcImg, new Point(rect.x + +rect.width /2 , rect.y+ rect.height /2), 10, new Scalar(255,0,0), 1);
	    }
	    return srcImg;
	}//boundBox
	
	public Mat drawCG(Mat srcImg, ArrayList<MatOfPoint> srcMopList, Scalar color, int thickness)	{
		try	{
			for(int i = 0; i < srcMopList.size(); i++)
				Imgproc.drawContours(srcImg, srcMopList, i, color, thickness);
		}catch(Exception e)	{ System.out.println("Unable to draw");}
		return srcImg;
	}//drawCG
	
	
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