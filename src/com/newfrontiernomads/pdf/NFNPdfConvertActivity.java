package com.newfrontiernomads.pdf;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Bitmap.Config;
import android.graphics.PointF;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;

import com.artifex.mupdf.MuPDFCore;
import com.newfrontiernomads.pdf.Intents.Convert;

public class NFNPdfConvertActivity extends Activity {
	private final String TAG = "NFN";
	private MuPDFCore mPdfCore;
	private Bitmap mBitmap;
	private final static float SCALE_MAX = 5;
	private final static float SCALE_MIN = 0.1f;
	private File defaultOutputDir;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		String appName = getString(getApplicationInfo().labelRes);
		defaultOutputDir = new File(Environment.getExternalStorageDirectory(),
				Environment.DIRECTORY_PICTURES);
		defaultOutputDir = new File(defaultOutputDir, appName);
		defaultOutputDir = new File(defaultOutputDir, Long.toString(System
				.currentTimeMillis()));

		super.onCreate(savedInstanceState);
		setContentView(R.layout.scr_convert);
		// setVisible(false);

		Intent intent = getIntent();
		Log.d(TAG, "Intent action: " + intent.getAction());
		Log.d(TAG, "Intent data: " + intent.getData() );
		// if (!Convert.ACTION.equals(intent.getAction())) {
		// setResult(RESULT_CANCELED);
		// finish();
		// return;
		// }

		Uri uri = intent.getData();
		if(uri == null){
			setResult(RESULT_CANCELED);
			finish();
			return;
		}
		if (uri.toString().startsWith("content://media/external/file")) {
			Cursor cursor = getContentResolver().query(uri,
					new String[] { "_data" }, null, null, null);
			if (cursor.moveToFirst()) {
				uri = Uri.parse(cursor.getString(0));
			}
		}

		String pathToFile = Uri.decode(uri.getEncodedPath());
		mPdfCore = openFile(pathToFile);

		Log.d(TAG, "Handling convert action: " + intent.getAction());

		AsyncTask<Intent, Void, Void> convertTask = new AsyncTask<Intent, Void, Void>() {
			@Override
			protected Void doInBackground(Intent... params) {
				Intent convertIntent = params[0];

				Log.d(TAG, "calling handleConvertAction");
				handleConvertAction(convertIntent);
				return null;
			}

			@Override
			protected void onPostExecute(Void result) {
				finish();
			}

		};

		convertTask.execute(intent);

	}

	private MuPDFCore openFile(String path) {

		if (BuildConfig.DEBUG) {
			Log.d(TAG, "Trying to open " + path);
		}
		try {
			mPdfCore = new MuPDFCore(path);
		} catch (Exception e) {
			Log.e(TAG, "", e);
			return null;
		}
		return mPdfCore;
	}

	public boolean mIsDynamicallyScaled;
	public PointF mTargetSize = new PointF();

	private float determineScaleType(Bundle bundle) {
		float scale = 1;
		/*
		 * Precedence is 1. SCALE 2. WIDTH and/or HEIGHT
		 */
		if (bundle.containsKey(Convert.Extras.SCALE)) {
			scale = bundle.getInt(Convert.Extras.SCALE);
			mIsDynamicallyScaled = false;
			return normalizeScaleValue(scale);
		}

		if (bundle.containsKey(Convert.Extras.WIDTH)
				|| bundle.containsKey(Convert.Extras.HEIGHT)) {
			mIsDynamicallyScaled = true;
			mTargetSize.x = bundle.getInt(Convert.Extras.WIDTH);
			mTargetSize.y = bundle.getInt(Convert.Extras.HEIGHT);
		}

		return scale;
	}

	private float normalizeScaleValue(float scale) {
		scale = scale > SCALE_MAX ? SCALE_MAX : scale;
		scale = scale < SCALE_MIN ? SCALE_MIN : scale;
		return scale;
	}

	/**
	 * Converts the pdf into images.
	 * 
	 * @param convertIntent
	 */
	private void handleConvertAction(Intent convertIntent) {
		Log.d(TAG, "parsing extras");

		Bundle extras = convertIntent.getExtras();
		if (extras == null) {
			extras = new Bundle();
		}

		int pages[] = extras.getIntArray(Convert.Extras.PAGES);

		String outputDirString = extras
				.getString(Convert.Extras.OUTPUT_DIRECTORY);
		if (outputDirString == null) {
			// use the default directory to output
			outputDirString = defaultOutputDir.toString();
		}

		File outputDirectory = new File(outputDirString);
		String outputFileType = extras
				.getString(Convert.Extras.OUTPUT_FILE_TYPE);
		if (outputFileType == null) {
			outputFileType = "png";
		}

		if (!outputDirectory.exists()) {
			outputDirectory.mkdirs();
		}

		// what pages are we displaying?
		// if we weren't given pages, use the single page we were given, if
		// any
		if (pages == null) {
			if (extras.containsKey(Convert.Extras.PAGE)) {
				pages = new int[] { extras.getInt(Convert.Extras.PAGE) };
			}
		}

		mScale = determineScaleType(extras);

		Log.d(TAG, "Done parsing extras");
		try {

			boolean isJpeg = outputFileType.equalsIgnoreCase("jpeg")
					|| outputFileType.equalsIgnoreCase("jpeg");

			final int totalPages = mPdfCore.countPages();
			int totalPagesConverted = 0;
			if (pages == null) {
				// still no pages? they want everything
				Log.d(TAG, "Extracting all pages");
				if (isJpeg) {
					extractPagesInSequenceAsJpegs(outputDirectory, 0,
							totalPages, 90);
				} else {
					extractPagesInSequenceAsPngs(outputDirectory, 0, totalPages);
				}
				totalPagesConverted = totalPages;
			} else {

				// make sure the pages we were given are valid
				for (int i = 0; i < pages.length; i++) {
					int pageInList = pages[i];

					if (pageInList < 0 || pageInList >= totalPages) {
						throw new IndexOutOfBoundsException();
					}
				}

				Log.d(TAG, "Extracting some pages");
				if (isJpeg) {
					extractPageRangeToDirectoryAsJpegs(outputDirectory, pages,
							90);
				} else {
					extractPageRangeToDirectoryAsPngs(outputDirectory, pages);
				}

				totalPagesConverted = pages.length;
			}

			Intent resultIntent = new Intent();
			resultIntent.putExtra(Convert.ResultExtras.PAGE_COUNT,
					totalPagesConverted);
			setResult(RESULT_OK, resultIntent);

		} catch (IndexOutOfBoundsException ex) {
			Log.e(TAG, "error with a page. ", ex);
			setResult(Intents.RESULT_EXCEPTION);
		}
	}

	private void extractPageRangeToDirectoryAsJpegs(File outputDir,
			int[] pages, int jpegQuality) {
		if (jpegQuality < 1 || jpegQuality > 100) {
			jpegQuality = 100;
		}

		for (int i = 0; i < pages.length; i++) {
			String name = String.format("%d.jpg", i);
			File file = new File(outputDir, name);
			Bitmap b = exportImage(file.getPath(), i);
			saveToJpeg(b, jpegQuality, file.getPath());
		}
	}

	private void extractPageRangeToDirectoryAsPngs(File outputDir, int[] pages) {
		for (int i = 0; i < pages.length; i++) {
			String name = String.format("%d.jpg", i);
			File file = new File(outputDir, name);
			Bitmap b = exportImage(file.getPath(), i);
			saveToPng(b, file.getPath());
		}
	}

	private void extractPagesInSequenceAsJpegs(File outputDir, int startIndex,
			int numberOfItems, int jpegQuality) {
		if (jpegQuality < 1 || jpegQuality > 100) {
			jpegQuality = 100;
		}

		for (int i = startIndex; i < numberOfItems; i++) {
			String name = String.format("%d.jpg", i);
			File file = new File(outputDir, name);
			Bitmap b = exportImage(file.getPath(), i);
			saveToJpeg(b, jpegQuality, file.getPath());
		}
	}

	private void extractPagesInSequenceAsPngs(File outputDir, int startIndex,
			int numberOfItems) {

		for (int i = startIndex; i < numberOfItems; i++) {
			String name = String.format("%d.jpg", i);
			File file = new File(outputDir, name);
			Bitmap b = exportImage(file.getPath(), i);
			saveToPng(b, file.getPath());
		}
	}

	// private Bitmap exportImage(String filename, int page) {
	// PointF size = mPdfCore.getPageSize(page);
	// int pageW = (int) (size.x * scale);
	// int pageH = (int) (size.y * scale);
	//
	// int patchX = 0;
	// int patchY = 0;
	// int patchW = pageW;// 300;
	// int patchH = pageH; // 300;
	//
	// if (mBitmap == null
	// || (mBitmap.getWidth() != pageW || mBitmap.getHeight() != pageH)) {
	// mBitmap = Bitmap.createBitmap(pageW, pageH, Config.ARGB_8888);
	// }
	//
	// mPdfCore.drawPage(page, mBitmap, pageW, pageH, patchX, patchY, patchW,
	// patchH);
	//
	// return mBitmap;
	//
	// }

	private PointF resizePageTo(PointF sourcePage, PointF targetPage) {
		float sourceRatio = sourcePage.x / sourcePage.y;
		float targetRatio = targetPage.x / targetPage.y;

		PointF size = new PointF();
		
		// If source < target,  source is taller than the target. Scale by the height
		if(targetPage.x == 0  || sourceRatio < targetRatio){
			size.y = targetPage.y;
			size.x = (sourcePage.x * targetPage.y) / sourcePage.y;
		}
		
		// If source > target, source is wider than the target. scale by the width
		if(targetPage.y == 0 || sourceRatio >= targetRatio){
			size.x = targetPage.x;
			size.y = (sourcePage.y * targetPage.x) / sourcePage.x;
		}
		return size;
	}

	private float mScale = 1;

	private Bitmap exportImage(String filename, int page) {
		PointF size = mPdfCore.getPageSize(page);
		int pageW = (int) (size.x * mScale);
		int pageH = (int) (size.y * mScale);

		int patchX = 0;
		int patchY = 0;
		int patchW = pageW;// 300;
		int patchH = pageH; // 300;

		if (mIsDynamicallyScaled) {
			size = resizePageTo(size, mTargetSize);

			pageW = (int) size.x;
			pageH = (int) size.y;

			patchX = 0;
			patchY = 0;
			patchW = pageW;// 300;
			patchH = pageH; // 300;
		}

		if(BuildConfig.DEBUG){
			Log.d(TAG, String.format("bitmap %d x %d", pageW, pageH));
		}
		if (mBitmap == null
				|| (mBitmap.getWidth() != pageW || mBitmap.getHeight() != pageH)) {
			mBitmap = Bitmap.createBitmap(pageW, pageH, Config.ARGB_8888);
		}

		mPdfCore.drawPage(page, mBitmap, pageW, pageH, patchX, patchY, patchW,
				patchH);

		return mBitmap;

	}

	private void saveToJpeg(Bitmap bitmap, int jpegQuality, String filename) {
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(filename);
			if (BuildConfig.DEBUG) {
				Log.d(TAG, "Saving " + filename);
			}
			bitmap.compress(CompressFormat.JPEG, 100, fos);
		} catch (FileNotFoundException e) {
			Log.e("NFN", "Failed to save bitmap: ", e);
		}
		if (fos != null) {
			try {
				fos.close();
			} catch (IOException e) {
				// swallow the close exception
			}
		}
	}

	private void saveToPng(Bitmap bitmap, String filename) {
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(filename);
			if (BuildConfig.DEBUG) {
				Log.d(TAG, "Saving " + filename);
			}
			bitmap.compress(CompressFormat.PNG, 1, fos);
		} catch (FileNotFoundException e) {
			Log.e("NFN", "Failed to save bitmap: ", e);
		}
		if (fos != null) {
			try {
				fos.close();
			} catch (IOException e) {
				// swallow the close exception
			}
		}
	}

}
