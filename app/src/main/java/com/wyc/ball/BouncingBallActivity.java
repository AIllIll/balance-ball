package com.wyc.ball;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.Display;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;
import android.os.Vibrator;


public class BouncingBallActivity extends Activity implements SensorEventListener{
 
	// sensor-related
    private SensorManager mSensorManager;
	private Sensor mAccelerometer;
 
	// animated view
	private ShapeView mShapeView;

	// vibrator
    private Vibrator vib;
	
	// screen size
	private int mWidthScreen;
	private int mHeightScreen;

	// 图形渲染上锁
	private boolean isCanvasChanging = false;

	// motion parameters
    private final float FACTOR_FRICTION = 0f; // imaginary friction on the screen
    private final float GRAVITY = 9.8f; // acceleration of gravity
    private float mAx; // acceleration along x axis
    private float mAy; // acceleration along y axis
    private final float mDeltaT = 0.2f; // imaginary time interval between each acceleration updates
	 
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // set the screen always portrait
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        // 无topBar
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        // 无状态栏
        getWindow().setFlags(WindowManager.LayoutParams. FLAG_FULLSCREEN , WindowManager.LayoutParams. FLAG_FULLSCREEN);

        // initializing sensors
        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

         // 震动
        vib = (Vibrator) getSystemService(VIBRATOR_SERVICE);

        // obtain screen width and height
        Display display = ((WindowManager)this.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        mWidthScreen = display.getWidth();
        mHeightScreen = display.getHeight();
	 }

    @Override
    protected void onResume() {
        super.onResume();

        // initializing the view that renders the ball
        mShapeView = new ShapeView(this);
        mShapeView.setOvalCenter((int)(mWidthScreen * 0.5), (int)(mHeightScreen * 0.5));

        setContentView(mShapeView);
        // start sensor sensing
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // stop senser sensing
        // mSensorManager.unregisterListener(this);
    }

    protected void onDestory()
    {
        super.onDestroy();
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // obtain the three accelerations from sensors
        //mAx = event.values[SensorManager.DATA_X];
        //mAy = event.values[SensorManager.DATA_Y];
        //float mAz = event.values[SensorManager.DATA_Z];

        // taking into account the frictions
        // mAx = Math.signum(mAx) * Math.abs(mAx) * (1 - FACTOR_FRICTION * Math.abs(mAz) / GRAVITY);
        // mAy = Math.signum(mAy) * Math.abs(mAy) * (1 - FACTOR_FRICTION * Math.abs(mAz) / GRAVITY);



        // obtain the three accelerations from sensors
        float tmpAx = event.values[SensorManager.DATA_X];
        float tmpAy = event.values[SensorManager.DATA_Y];
        float tmpAz = event.values[SensorManager.DATA_Z];

        // taking into account the frictions
        mAx = Math.signum(tmpAx) * (Math.abs(tmpAx) - FACTOR_FRICTION * Math.abs(tmpAz));
        mAy = Math.signum(tmpAy) * (Math.abs(tmpAy) - FACTOR_FRICTION * Math.abs(tmpAz));
    }

    void vibrate() {
        vib.vibrate(500);
    }
 
    // the view that renders the ball
    private class ShapeView extends SurfaceView implements SurfaceHolder.Callback{

        private final int RADIUS = 50;
        private final float FACTOR_BOUNCEBACK = 1f;

        private int mXCenter;
        private int mYCenter;
        private int mBallColor = 0XFF000000;
        private int[] colorList = {
                0xFF78838B,
                0xFFDCF5F5,
                0xFFCDEBFF,
                0xFFFF0000,
                0xFF2A2AA5,
                0xFF87B8DE,
                0xFFA09E5F,
                0xFF00FF7F,
                0xFF7295EE,
                0xFFE0FFFF,
                0xFF621C8B,
                0xFFFF82AB,
                0xFF800000,
                0xFFCBC0FF,
                0xFF0000FF,
                0xFFCDA66C
        };
        private RectF mRectF;
        private final Paint mPaint;
        private ShapeThread mThread;

        private float mVx;
        private float mVy;

        private boolean destroyed = false;

        public ShapeView(Context context) {
            super(context);

            getHolder().addCallback(this);
            mThread = new ShapeThread(getHolder(), this);
            setFocusable(true);

            mPaint = new Paint();
            mPaint.setColor(0xFFFFFFFF);
            mPaint.setAlpha(192);
            mPaint.setStyle(Paint.Style.FILL);
            mPaint.setAntiAlias(true);

            mRectF = new RectF();
        }

        public void changeOvalColor() {
            mBallColor = colorList[(int)(1+Math.random()*(15-1+1))];
        }

        // set the position of the ball
        public boolean setOvalCenter(int x, int y) {
            mXCenter = x;
            mYCenter = y;
            return true;
        }

        // calculate and update the ball's position
        public boolean updateOvalCenter() {
            mVx -= mAx * mDeltaT;
            mVy += mAy * mDeltaT;

            mXCenter += (int)(mDeltaT * (mVx + 0.5 * mAx * mDeltaT));
            mYCenter += (int)(mDeltaT * (mVy + 0.5 * mAy * mDeltaT));

            if(mXCenter < RADIUS) {
                mXCenter = RADIUS;
                mVx = -mVx * FACTOR_BOUNCEBACK;
                changeOvalColor();
                vibrate();
            }

            if(mYCenter < RADIUS) {
                mYCenter = RADIUS;  mVy = -mVy * FACTOR_BOUNCEBACK;
                changeOvalColor();
                vibrate();
            }

            if(mXCenter > mWidthScreen - RADIUS) {
                mXCenter = mWidthScreen - RADIUS;
                mVx = -mVx * FACTOR_BOUNCEBACK;
                changeOvalColor();
                vibrate();
            }

            if(mYCenter > mHeightScreen - RADIUS) {
                mYCenter = mHeightScreen - RADIUS;
                mVy = -mVy * FACTOR_BOUNCEBACK;
                changeOvalColor();
                vibrate();
            }

            return true;
        }

        // update the canvas
        protected void onDraw(Canvas canvas) {
            if(mRectF != null && destroyed == false ) {
                mRectF.set(mXCenter - RADIUS, mYCenter - RADIUS, mXCenter + RADIUS, mYCenter + RADIUS);
                canvas.drawColor(mBallColor);
                canvas.drawOval(mRectF, mPaint);
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        }

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            mThread.setRunning(true);
            mThread.start();
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            destroyed = true;
            boolean retry = true;
            mThread.setRunning(false);
            while(retry) {
                try{
                    mThread.join();
                    retry = false;
                } catch (InterruptedException e){

                }
            }
        }

    }

    class ShapeThread extends Thread {
        private SurfaceHolder mSurfaceHolder;
        private ShapeView mShapeView;
        private boolean mRun = false;

        public ShapeThread(SurfaceHolder surfaceHolder, ShapeView shapeView) {
            mSurfaceHolder = surfaceHolder;
            mShapeView = shapeView;
        }

	    public void setRunning(boolean run) {
		 mRun = run;
	 }

	    public SurfaceHolder getSurfaceHolder() {
		 return mSurfaceHolder;
	 }

        @Override
        public void run() {
            Canvas c;
            while (mRun) {
                mShapeView.updateOvalCenter();
                c = null;
                try {
                    c = mSurfaceHolder.lockCanvas(null);
                    synchronized (mSurfaceHolder) {
                        mShapeView.onDraw(c);
                    }
                } finally {
                    if (c != null) {
                        mSurfaceHolder.unlockCanvasAndPost(c);
                    }
                }
			}
        }
    }
}