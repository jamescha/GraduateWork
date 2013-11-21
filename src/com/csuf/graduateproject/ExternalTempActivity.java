
package com.csuf.graduateproject;
import slickdevlabs.apps.usb2seriallib.AdapterConnectionListener;
import slickdevlabs.apps.usb2seriallib.SlickUSB2Serial;
import slickdevlabs.apps.usb2seriallib.USB2SerialAdapter;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.Toast;

import com.androidplot.Plot;
import com.androidplot.ui.SizeLayoutType;
import com.androidplot.ui.SizeMetrics;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.PointLabelFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYPlot;

public class ExternalTempActivity extends Activity implements 
OnClickListener,AdapterConnectionListener, USB2SerialAdapter.DataListener, OnItemSelectedListener {

	private static final String TAG = "ExternalTempActivity";
	
	private USB2SerialAdapter ad;
	//XYPlot Set up
	private XYPlot plot = null;
    private SimpleXYSeries datalineSen = null;
    private LineAndPointFormatter datalineFormat;
    
    //Buttons
    private Button onBoardTempButton;
    private Button externalTempButton;
    private Button humidityButton;
    
    
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate Started");
        setContentView(R.layout.activity_main);
        
       
        final Intent mainIntent = new Intent(this, MainActivity.class);
        final Intent humidityIntent = new Intent(this, HumidityActivity.class);        
        
        datalineFormat = new LineAndPointFormatter();
        datalineFormat.setPointLabelFormatter(new PointLabelFormatter());
        datalineFormat.configure(getApplicationContext(), R.xml.line_point_formater);
        
        datalineSen = new SimpleXYSeries("Sensor Temperature");
        datalineSen.useImplicitXVals();
        
        plot = (XYPlot) findViewById(R.id.XYPlot);
        plot.setBorderStyle(Plot.BorderStyle.NONE, null, null);
        plot.getGraphWidget().getDomainLabelPaint().setColor(Color.BLACK);
        plot.getGraphWidget().getRangeLabelPaint().setColor(Color.BLACK);
        plot.getGraphWidget().getDomainOriginLabelPaint().setColor(Color.BLACK);
        plot.getGraphWidget().getDomainOriginLinePaint().setColor(Color.BLACK);
        plot.getGraphWidget().getRangeOriginLinePaint().setColor(Color.BLACK);
        plot.setRangeBoundaries(0, 100, BoundaryMode.FIXED);
        plot.setDomainBoundaries(0, 30, BoundaryMode.FIXED);
        plot.getGraphWidget().getGridBackgroundPaint().setColor(Color.TRANSPARENT);
        plot.getBackgroundPaint().setColor(Color.TRANSPARENT);
        plot.getGraphWidget().getBackgroundPaint().setColor(Color.TRANSPARENT);
        
        plot.addSeries(datalineSen,  new LineAndPointFormatter(Color.rgb(
        		 														 new Double(Math.random()*255).intValue(),
        		 														 new Double(Math.random()*255).intValue(), 
        		 														 new Double(Math.random()*255).intValue()), Color.BLACK, null, null));
        plot.setPadding(0, 0, 0, 0);
        plot.setPlotMargins(0, 0, 0, 0);
        plot.setDomainStepValue(5);
        plot.setTicksPerRangeLabel(3);
        plot.setDomainLabel("Duration");
        plot.getDomainLabelWidget().pack();
        plot.getDomainLabelWidget().getLabelPaint().setColor(Color.BLACK);
        plot.setRangeLabel("Temperature/Humidity");
        plot.getRangeLabelWidget().pack();
        plot.getRangeLabelWidget().getLabelPaint().setColor(Color.BLACK);
        Paint bgPaint = new Paint();
        bgPaint.setColor(Color.BLACK);
        bgPaint.setStyle(Paint.Style.FILL);
        bgPaint.setAlpha(140);
        plot.getLegendWidget().setBackgroundPaint(bgPaint);
        plot.getLegendWidget().setSize(new SizeMetrics(25, SizeLayoutType.ABSOLUTE, 400, SizeLayoutType.ABSOLUTE));
        
        SlickUSB2Serial.initialize(this);
        SlickUSB2Serial.autoConnect(ExternalTempActivity.this);
        
        onBoardTempButton =  (Button) findViewById(R.id.onboard);
        onBoardTempButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				startActivity(mainIntent);
			}
		});
        
        externalTempButton = (Button) findViewById(R.id.external);
        externalTempButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				
				ad.setCommSettings(SlickUSB2Serial.BaudRate.BAUD_115200, 
						   SlickUSB2Serial.DataBits.DATA_8_BIT, 
						   SlickUSB2Serial.ParityOption.PARITY_NONE,
						   SlickUSB2Serial.StopBits.STOP_1_BIT);
			}
		});
        
        humidityButton = (Button) findViewById(R.id.humidity);
        humidityButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				startActivity(humidityIntent);
				
			}
		});
        
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    
    public void onDestory(){
    	SlickUSB2Serial.cleanup(this);
    	super.onDestroy();
    }

	@Override
	public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2,
			long arg3) {
		// TODO Auto-generated method stub
	}


	@Override
	public void onNothingSelected(AdapterView<?> arg0) {
		// TODO Auto-generated method stub
	}
	
	@Override
	public void onDataReceived(int id, byte[] data) {
		final String newText = SlickUSB2Serial.convertByte2String(data);
		final String[] newTextArray = newText.split(" ", -1);
		final Integer length = newTextArray.length;
		
		Integer countSen = 0;
		Integer sumSen = 0;
		Double averageSen = 0.0;
		Double temperatureSen = 0.0;
		
		
		if (length == 42) {
			for(Integer i = 0; i<20; i+=2) {
				if (Integer.parseInt((newTextArray[18+i] + newTextArray[19+i]),16) > 5500) { //Sensirian
					sumSen += Integer.parseInt((newTextArray[18+i] + newTextArray[19+i]),16);
					countSen++;
				}
			}
			
			averageSen = sumSen/(countSen * 1.0);
			
			temperatureSen = (((-38.4 + (averageSen * 0.0098)) * 9.0) / 5.0) + 32.0;
			
			
			Log.d("SENTEMPERATURE", temperatureSen.toString());
		}
		Log.d("LENGTH", length.toString());
		final Double tempSen = temperatureSen;
		runOnUiThread(new Runnable(){
			@Override
			public void run() {
				if (datalineSen.size()>30)
					datalineSen.removeFirst();
				if(length == 42)
				{
					datalineSen.addLast(null, tempSen);
					plot.redraw();
				}
			}
		});
	}

	@Override
	public void onAdapterConnected(USB2SerialAdapter adapter) {
		adapter.setDataListener(this);
		adapter.setCommSettings(SlickUSB2Serial.BaudRate.BAUD_115200, 
				   SlickUSB2Serial.DataBits.DATA_8_BIT, 
				   SlickUSB2Serial.ParityOption.PARITY_NONE,
				   SlickUSB2Serial.StopBits.STOP_1_BIT);
		ad=adapter;
		Toast.makeText(ExternalTempActivity.this, "Adapter "+adapter.getDeviceId()+" Connected!", Toast.LENGTH_SHORT).show();
	}

	@Override
	public void onAdapterConnectionError(int arg0, String arg1) {
		Toast.makeText(ExternalTempActivity.this, R.string.connection_error, Toast.LENGTH_SHORT).show();
	}

	@Override
	public void onClick(View arg0) {
		// TODO Auto-generated method stub
	}
    
}
