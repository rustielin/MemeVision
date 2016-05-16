package com.floatlearning.android_opencv_template;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.features2d.FeatureDetector;
import org.opencv.features2d.Features2d;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;


// NOTE: Need OpenCV Manager, to prevent app size from being huge
public class MainActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2 {
    private static final String TAG = "MainActivity";

    private CameraBridgeViewBase mOpenCVCameraView;

    private CascadeClassifier mCascadeClassifier;
    private Mat grayscaleImage;
    private int absoluteFaceSize;

    private ImageView imageView;

    // private FeatureDetector detector = FeatureDetector.create(FeatureDetector.SIFT);

    private BaseLoaderCallback   mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV load success");

                    // fixes aspect ratio issue

                    //Toast.makeText(this, "INITIALIZED CV", Toast.LENGTH_LONG).show();
                    //mOpenCVCameraView.setMaxFrameSize(850, 480); // increases frame rate. change this
                   // fps meter bound to main_activity.xml
                    initializeOpenCVDependencies();
                    //mOpenCVCameraView.enableView();

                    // initializeOpenCVDependencies();
                } break;
                default: {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        //mOpenCVCameraView = new JavaCameraView(this, -1);
        mOpenCVCameraView = (JavaCameraView) findViewById(R.id.MainActivityCameraView); // bind
        mOpenCVCameraView.setVisibility(SurfaceView.VISIBLE);
        //setContentView(mOpenCVCameraView);
        mOpenCVCameraView.setCvCameraViewListener(this);

        //imageView = (ImageView) this.findViewById(R.id.imageView);
    }

    private void initializeOpenCVDependencies() {


        try {
            // Copy the resource into a temp file so OpenCV can load it
            InputStream is = getResources().openRawResource(R.raw.lbpcascade_frontalface);
            File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
            File mCascadeFile = new File(cascadeDir, "lbpcascade_frontalface.xml");
            FileOutputStream os = new FileOutputStream(mCascadeFile);


            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            is.close();
            os.close();


            // Load the cascade classifier
            mCascadeClassifier = new CascadeClassifier(mCascadeFile.getAbsolutePath());
        } catch (Exception e) {
            Log.e("OpenCVActivity", "Error loading cascade", e);
        }


        // And we are ready to go
        mOpenCVCameraView.setMaxFrameSize(850, 480);
        mOpenCVCameraView.enableView();
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mOpenCVCameraView != null) {
            mOpenCVCameraView.disableView();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_9, this, mLoaderCallback); // link to cv manager and make cv work YAAASSSSSSSSSSSSSS
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mOpenCVCameraView != null) {
            mOpenCVCameraView.disableView();
        }
    }

    public void onCameraViewStarted(int width, int height) {
        grayscaleImage = new Mat(height, width, CvType.CV_8UC4);
        absoluteFaceSize = (int) (height * 0.2);
    }

    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat matRgba = inputFrame.rgba();
        // just draw a text string to test modifying onCameraFrame real time by listener
        Core.putText(matRgba, "~20% screen size; frontal face straight", new Point(300,300), 3, 1, new Scalar (255, 0, 0, 255), 2);
        onCameraFrame(matRgba);
        return matRgba;
        //return inputFrame.rgba();
    }



    public Mat onCameraFrame(Mat aInputFrame) {
        // Create a grayscale image
        Imgproc.cvtColor(aInputFrame, grayscaleImage, Imgproc.COLOR_RGBA2RGB);


        MatOfRect faces = new MatOfRect();


        // Use the classifier to detect faces
        if (mCascadeClassifier != null) {
            mCascadeClassifier.detectMultiScale(grayscaleImage, faces, 1.1, 2, 2,
                    new Size(absoluteFaceSize, absoluteFaceSize), new Size());
        }


        // If there are any faces found, draw a rectangle around it
        Rect[] facesArray = faces.toArray();
        for (int i = 0; i <facesArray.length; i++)
            Core.rectangle(aInputFrame, facesArray[i].tl(), facesArray[i].br(), new Scalar(0, 255, 0, 255), 3);


        return aInputFrame;
    }


    public void onCameraViewStopped() { }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        View decorView = getWindow().getDecorView();
        if(hasFocus) {
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    }

    /* public void sift(Mat rgba) {
        Bitmap bitmap = Bitmap.createBitmap(rgba.width(), rgba.height(), Bitmap.Config.ARGB_8888); // create a bitmap file with same dimensions as matrix

        MatOfKeyPoint keyPoints = new MatOfKeyPoint();
        Imgproc.cvtColor(rgba, rgba, Imgproc.COLOR_RGBA2GRAY); // image detection best done with grayscale
        detector.detect(rgba, keyPoints); // detect key points and store

        Features2d.drawKeypoints(rgba, keyPoints, rgba); // draw key points to original matrix
        Utils.matToBitmap(rgba, bitmap); // convert matrix to bitmap to inflate and display later

        imageView.setImageBitmap(bitmap); // change imageView and set display

    } */
}
