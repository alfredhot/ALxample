/***
  Copyright (c) 2008-2012 CommonsWare, LLC
  Licensed under the Apache License, Version 2.0 (the "License"); you may not
  use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0. Unless required
  by applicable law or agreed to in writing, software distributed under the
  License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
  OF ANY KIND, either express or implied. See the License for the specific
  language governing permissions and limitations under the License.
  
  From _The Busy Coder's Guide to Advanced Android Development_
    http://commonsware.com/AdvAndroid
 */

package com.commonsware.android.picture;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

public class PictureDemo extends Activity {
	private SurfaceView preview = null;
	private SurfaceHolder previewHolder = null;
	private Camera camera = null;
	private boolean inPreview = false;
	private boolean cameraConfigured = false;
	
	public static final int _HANDLER_SHOW_PROCESS = 1001;
	public static final int _HANDLER_HIDE_PROCESS = 1002;

	/**
	 * views
	 */
	private ImageView img_main_frame;
	private ImageButton imb_main_shutter;
	private RelativeLayout rl_main_process_layer;
	private Button btn_show,btn_hide;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// Remove title bar
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.main);
		initView();

		preview = (SurfaceView) findViewById(R.id.preview);
		previewHolder = preview.getHolder();
		previewHolder.addCallback(surfaceCallback);
		// previewHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		imb_main_shutter.setOnClickListener(viewOnClickListener);
	}

	@Override
	public void onResume() {
		super.onResume();
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
			Camera.CameraInfo info = new Camera.CameraInfo();

			for (int i = 0; i < Camera.getNumberOfCameras(); i++) {
				Camera.getCameraInfo(i, info);

				if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
					camera = Camera.open(i);
				}
			}
		}
		if (camera == null) {
			camera = Camera.open();
		}
		startPreview();
	}

	@Override
	public void onPause() {
		if (inPreview) {
			camera.stopPreview();
		}
		camera.release();
		camera = null;
		inPreview = false;

		super.onPause();
	}

	@SuppressLint("NewApi")
	public void initView() {
		img_main_frame = (ImageView) findViewById(R.id.img_main_frame);
		imb_main_shutter = (ImageButton) findViewById(R.id.imb_main_shuter);
		img_main_frame.setScaleX(0.8f);
		img_main_frame.setScaleY(0.8f);
		
		rl_main_process_layer = (RelativeLayout) findViewById(R.id.rl_main_process_layer);
//		showProcessBar(false);
		
		btn_show = (Button) findViewById(R.id.btn_show);
		btn_show.setOnClickListener(viewOnClickListener);
		btn_hide = (Button) findViewById(R.id.btn_hide);
		btn_hide.setOnClickListener(viewOnClickListener);
	}

	OnClickListener viewOnClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			if (v.equals(imb_main_shutter)) {
				if (inPreview) {
					camera.takePicture(null, null, photoCallback);
					inPreview = false;
				}
			} else if(v.equals(btn_show)){
				showProcessBar(true);
			} else if(v.equals(btn_hide)){
				showProcessBar(false);
			}else {
			}
		}
	};

	
	@SuppressLint("HandlerLeak")
	private Handler handler = new Handler(){

		@Override
		public void handleMessage(Message msg) {
			switch(msg.what){
			case _HANDLER_SHOW_PROCESS:
				rl_main_process_layer.setVisibility(View.VISIBLE);
				break;
			case _HANDLER_HIDE_PROCESS:
				rl_main_process_layer.setVisibility(View.GONE);
				break;
			default:
				break;
			}
		}
		
	};
	
	private Camera.Size getBestPreviewSize(int width, int height, Camera.Parameters parameters) {
		Camera.Size result = null;

		for (Camera.Size size : parameters.getSupportedPreviewSizes()) {
			if (size.width <= width && size.height <= height) {
				if (result == null) {
					result = size;
				} else {
					int resultArea = result.width * result.height;
					int newArea = size.width * size.height;

					if (newArea > resultArea) {
						result = size;
					}
				}
			}
		}
		return (result);
	}

	private Camera.Size getSmallestPictureSize(Camera.Parameters parameters) {
		Camera.Size result = null;

		for (Camera.Size size : parameters.getSupportedPictureSizes()) {
			if (result == null) {
				result = size;
			} else {
				int resultArea = result.width * result.height;
				int newArea = size.width * size.height;

				if (newArea < resultArea) {
					result = size;
				}
			}
		}

		return (result);
	}

	@SuppressLint("InlinedApi")
	private void initPreview(int width, int height) {

		if (camera != null && previewHolder.getSurface() != null) {
			try {
				camera.setPreviewDisplay(previewHolder);
			} catch (Throwable t) {
				Log.e("PreviewDemo-surfaceCallback", "Exception in setPreviewDisplay()", t);
				Toast.makeText(PictureDemo.this, t.getMessage(), Toast.LENGTH_LONG).show();
			}

			camera.setDisplayOrientation(90);
			if (!cameraConfigured) {
				Camera.Parameters parameters = camera.getParameters();
				Camera.Size size = getBestPreviewSize(width, height, parameters);
				Camera.Size pictureSize = getSmallestPictureSize(parameters);

				List<String> focusModes = parameters.getSupportedFocusModes();
				if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE))
				{
					parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
				}
				
				if (size != null && pictureSize != null) 
				{
					parameters.setPictureFormat(ImageFormat.JPEG);
					camera.setParameters(parameters);
					cameraConfigured = true;
				}
			}
		}
	}

	private void startPreview() {
		if (cameraConfigured && camera != null) {
			camera.startPreview();
			inPreview = true;
		}
	}

	SurfaceHolder.Callback surfaceCallback = new SurfaceHolder.Callback() {
		public void surfaceCreated(SurfaceHolder holder) {
			// no-op -- wait until surfaceChanged()
		}

		public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
			initPreview(width, height);
			startPreview();
		}

		public void surfaceDestroyed(SurfaceHolder holder) {
			// no-op
		}
	};

	Camera.PictureCallback photoCallback = new Camera.PictureCallback() {
		public void onPictureTaken(byte[] data, Camera camera) {
//			 new SavePhotoTask().execute(data);
			generateImage(data);
		}
	};

	class SavePhotoTask extends AsyncTask<byte[], String, String> {

		@Override
		protected String doInBackground(byte[]... jpeg) {
			generateImage(jpeg[0]);
			return null;
		}
	}
	
	/**
	 * 이미지 생성하기
	 * @param date
	 * @return
	 */
	private boolean generateImage(byte[] date){
		long basetime = Calendar.getInstance().getTimeInMillis();
		Log.d("Alfred", "start___!!! time:0 ms");
		camera.stopPreview();
		Log.d("Alfred", "stopPrev!!! time:"+(Calendar.getInstance().getTimeInMillis()-basetime)+" ms");
		showProcessBar(true);
		RelativeLayout frm = (RelativeLayout) findViewById(R.id.rl_main_frame);
		frm.setDrawingCacheEnabled(true);
		frm.buildDrawingCache();
		Log.d("Alfred", "buildDrw!!! time:"+(Calendar.getInstance().getTimeInMillis()-basetime)+" ms");
		
		//bitmap list to be recycled
		List<Bitmap> bmList = new ArrayList<Bitmap>();
		
		//ROTATE THE CAMERA BITMAP
		Matrix matrix_rotate = new Matrix();
		matrix_rotate.postRotate(90f);
		Bitmap bm_camera_org = BitmapFactory.decodeByteArray(date, 0, date.length);
		Bitmap bm_camera_rotate = 
				Bitmap.createBitmap(bm_camera_org, 0, 0
								, bm_camera_org.getWidth()
								, bm_camera_org.getHeight()
								, matrix_rotate , false);
		Log.d("Alfred", "rotateCm!!! time:"+(Calendar.getInstance().getTimeInMillis()-basetime)+" ms");
		clearBitmap(bm_camera_org);
		Bitmap bm_frame_org = frm.getDrawingCache();
		Log.d("Alfred", "drawFrme!!! time:"+(Calendar.getInstance().getTimeInMillis()-basetime)+" ms");
		
		bmList.add(bm_camera_rotate);
		bmList.add(bm_frame_org);
		
		int wid_camera = bm_camera_rotate.getWidth();
		int hgt_camera = bm_camera_rotate.getHeight();
		int wid_frame = bm_frame_org.getWidth();
		int hgt_frame = bm_frame_org.getHeight();
		float scaleWidth = ((float) wid_camera) / wid_frame;
		float scaleHeight = ((float) hgt_camera) /hgt_frame;
		
		Bitmap bm_final = Bitmap.createBitmap(wid_camera, hgt_camera, Bitmap.Config.ARGB_8888);
		bmList.add(bm_final);
		Canvas canvas = new Canvas(bm_final);
		Log.d("Alfred", "createCv!!! time:"+(Calendar.getInstance().getTimeInMillis()-basetime)+" ms");
		canvas.drawBitmap(bm_camera_rotate, 0f, 0f, null);
		Log.d("Alfred", "draw1Lyr!!! time:"+(Calendar.getInstance().getTimeInMillis()-basetime)+" ms");
		clearBitmap(bm_camera_rotate);
		// RESIZE THE BIT MAP
		Matrix matrix = new Matrix();
		matrix.postScale(scaleWidth, scaleHeight);
		Bitmap bm_frame_scale = Bitmap.createBitmap(bm_frame_org, 0, 0, wid_frame, hgt_frame, matrix, false);
		bmList.add(bm_frame_scale);
		clearBitmap(bm_frame_org);
		Log.d("Alfred", "scaleFrm!!! time:"+(Calendar.getInstance().getTimeInMillis()-basetime)+" ms");
		
		canvas.drawBitmap(bm_frame_scale, 0f, 0f, null);
		clearBitmap(bm_frame_scale);
		Log.d("Alfred", "draw2Lyr!!! time:"+(Calendar.getInstance().getTimeInMillis()-basetime)+" ms");
		

		try {
			Log.d("Alfred", "tryStart!!! time:"+(Calendar.getInstance().getTimeInMillis()-basetime)+" ms");
			File rootFile = new File(Environment.getExternalStorageDirectory().toString() + "/MYCAMERAOVERLAY");
			rootFile.mkdirs();
			Random generator = new Random();
			int n = 10000;
			n = generator.nextInt(n);
			String fname = "Image-" + n + ".jpg";

			Log.d("Alfred", "newFileS!!! time:"+(Calendar.getInstance().getTimeInMillis()-basetime)+" ms");
			File resultingfile = new File(rootFile, fname);
			Log.d("Alfred", "newFileF!!! time:"+(Calendar.getInstance().getTimeInMillis()-basetime)+" ms");

			if (resultingfile.exists())
				resultingfile.delete();
			try {
				Log.d("Alfred", "newFout_!!! time:"+(Calendar.getInstance().getTimeInMillis()-basetime)+" ms");
				FileOutputStream Fout = new FileOutputStream(resultingfile);
				// frameBitmap.compress(CompressFormat.PNG, 100, Fout);
				Log.d("Alfred", "compress!!! time:"+(Calendar.getInstance().getTimeInMillis()-basetime)+" ms");
				bm_final.compress(CompressFormat.JPEG, 50, Fout);
				Log.d("Alfred", "flushStart! time:"+(Calendar.getInstance().getTimeInMillis()-basetime)+" ms");
				Fout.flush();
				Log.d("Alfred", "flushFinish time:"+(Calendar.getInstance().getTimeInMillis()-basetime)+" ms");
				Fout.close();
				Log.d("Alfred", "close___!!! time:"+(Calendar.getInstance().getTimeInMillis()-basetime)+" ms");

			} catch (FileNotFoundException e) {
				Log.d("In Saving File", e + "");
			}
		} catch (IOException e) {
			Log.d("In Saving File", e + "");
		}


		//recycle
		Log.d("Alfred", "recycleS!!! time:"+(Calendar.getInstance().getTimeInMillis()-basetime)+" ms");
		for(Bitmap bm: bmList)
		{
			clearBitmap(bm);
		}
		Log.d("Alfred", "recycleF!!! time:"+(Calendar.getInstance().getTimeInMillis()-basetime)+" ms");
		
		inPreview = true;
		
		showProcessBar(false);
		Log.d("Alfred", "previewS!!! time:"+(Calendar.getInstance().getTimeInMillis()-basetime)+" ms");
		camera.startPreview();
		return true;
	}
	
	/**
	 * recycle resource
	 * @param bm the bitmap need to be recycled
	 * @return
	 */
	public boolean clearBitmap(Bitmap bm){
		if(bm!=null){
			bm.recycle();
			bm = null;
		}
		return true;
	}
	
	/**
	 * 로딩바 커버 제어
	 * @param trueForYes
	 * @return
	 */
	public boolean showProcessBar(final boolean trueForYes){
		if(trueForYes){
			handler.sendEmptyMessage(_HANDLER_SHOW_PROCESS);
		}else{
			handler.sendEmptyMessage(_HANDLER_HIDE_PROCESS);
		}
		return true;
	}
}