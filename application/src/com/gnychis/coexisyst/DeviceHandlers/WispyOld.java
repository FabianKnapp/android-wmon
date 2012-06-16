package com.gnychis.coexisyst.DeviceHandlers;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.concurrent.Semaphore;

import com.gnychis.coexisyst.CoexiSyst;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

public class WispyOld {
	public static final int WISPY_CONNECT = 0;
	public static final int WISPY_DISCONNECT = 1;
	public static final int WISPY_POLL = 2;
	public static final int WISPY_POLL_FAIL = 3;
	public static final int WISPY_POLL_THREAD = 4;
	
	public static final int PASSES = 20;
	
	File _root;
	FileOutputStream _wispyOut;
	PrintStream _wispyPrint;
	
	public boolean _device_connected;
	public boolean _is_polling;
	public boolean _reset_max;
	boolean _save_scans;
	public int _poll_count;
	public int _maxresults[];
	
	Semaphore _lock;
	
	public WispyOld() {
		_lock = new Semaphore(1,true);
        _device_connected=false;
        _is_polling=false;
        _reset_max=false;
        _poll_count=0;
        _save_scans=false;
        _maxresults = new int[256];
        for(int i=0; i<256; i++)
        	_maxresults[i]=-200;
        
        // For writing to SD card
        try {
	        _root = Environment.getExternalStorageDirectory();
	        _wispyOut = new FileOutputStream(new File(_root, "wispy.dat"));
	        _wispyPrint = new PrintStream(_wispyOut);
        } catch(Exception e) {
        	//Log.e(TAG, "Error opening output file", e);
        }
	}
	
	public void getResultsBlock(int count_length) {
		
		Log.d("wispy", "attempting to get results");
		// Reset everything here, making sure not to conflict with the polling thread
		try {
			_lock.acquire();

			_poll_count=0;
			_reset_max=false;
			for(int i=0; i<256; i++)
	        	_maxresults[i]=-200;
			_save_scans=true;
			_lock.release();
		} catch (Exception e) {
			Log.d("wispy", "error acquiring lock to reset results");
		}

		// Take a second lock which 
		try {
			while(true) {
				_lock.acquire();
				if(_poll_count==count_length) {
					_save_scans=false;
					break;
				}
				_lock.release();
			}
			_lock.release();
		} catch (Exception e) {
			Log.d("wispy", "error acquiring lock to not save any more scans");
		}
		Log.d("wispy", "finished getting results!");
	}
	
	// A class to handle USB worker like things
	public class WispyThread extends AsyncTask<Context, Integer, String>
	{
		Context parent;
		CoexiSyst coexisyst;	
		
		@Override
		protected String doInBackground( Context... params )
		{
			parent = params[0];
			coexisyst = (CoexiSyst) params[0];
			
			//publishProgress(CoexiSyst.WISPY_POLL_THREAD);
			
			if(initWiSpyDevices()==1) {
				publishProgress(WispyOld.WISPY_POLL);
			} else {
				publishProgress(WispyOld.WISPY_POLL_FAIL);
				_is_polling = false;
				return "FAIL";
			}
			
			while(true) {
				int[] scan_res = pollWiSpy();
				
				if(scan_res==null) {
					publishProgress(WispyOld.WISPY_POLL_FAIL);
					_is_polling = false;
					break;
				}
				
				//publishProgress(CoexiSyst.WISPY_POLL);		
				
				// What to do once we get a response!
				try {
					_lock.acquire();
					if(scan_res.length==256 && _save_scans) {
						for(int i=0; i<scan_res.length; i++)
							if(scan_res[i] > _maxresults[i]) 
								_maxresults[i] = scan_res[i];
						
						_poll_count++;
						Log.d("wispy_thread", "saved result from wispy thread");
					}
					_lock.release();
				} catch (Exception e) {
					Log.e("Wispy", "exception trying to claim lock to save new results",e);
				}
			}
			
			return "OK";
		}
		
		@Override
		protected void onProgressUpdate(Integer... values)
		{
			super.onProgressUpdate(values);
			int event = values[0];
			
			if(event==WispyOld.WISPY_POLL_THREAD) {
				//Toast.makeText(parent, "In WiSpy poll thread...",
				//		Toast.LENGTH_LONG).show();
			}
			else if(event==WispyOld.WISPY_POLL) {
				//Toast.makeText(parent, "WiSpy started polling...",
				//		Toast.LENGTH_LONG).show();
				//textStatus.append(".");
			}
			else if(event==WispyOld.WISPY_POLL_FAIL) {
				Toast.makeText(parent, "--- WiSpy poll failed ---",
						Toast.LENGTH_LONG).show();
			}
		}
	}
	
	public native int getWiSpy();
	public native String[] getWiSpyList();
	public native int initWiSpyDevices();
	public native int[] pollWiSpy();
}
