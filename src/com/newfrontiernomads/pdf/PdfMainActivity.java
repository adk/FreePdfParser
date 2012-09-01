package com.newfrontiernomads.pdf;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.newfrontiernomads.pdf.Intents.Convert;

public class PdfMainActivity extends Activity {
	private final String TAG = "NFN";
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.scr_main);
		
		Intent intent = getIntent();
		if(intent != null && intent.getAction().equals(Intent.ACTION_VIEW)){
			Log.d(TAG, "Data: "+intent.getData());
			Intent parseIntent = new Intent(Convert.ACTION);
			parseIntent.setDataAndType(intent.getData(), "application/pdf");
			parseIntent.putExtra(Convert.Extras.WIDTH, 50);
			parseIntent.putExtra(Convert.Extras.HEIGHT, 75);
			startActivityForResult(parseIntent, 0);
		}
		//setIntent(null);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.d(TAG, "ResultCode: " + resultCode );
		if(data != null){
			Log.d(TAG, "Number of pages: " + data.getExtras().getInt(Convert.ResultExtras.PAGE_COUNT));
		}
		finish();
	}
	
}
