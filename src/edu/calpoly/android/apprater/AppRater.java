package edu.calpoly.android.apprater;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Builder;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import edu.calpoly.android.apprater.AppView.OnAppChangeListener;

public class AppRater extends SherlockFragmentActivity implements OnAppChangeListener, OnItemClickListener, LoaderManager.LoaderCallbacks<Cursor> {
	
	/** The ListView that contains the List of AppViews. */
	private ListView m_vwAppList;

	/** The CursorAdapter used to bind the Cursor to AppViews. */
	private AppCursorAdapter m_appAdapter;

	/** The BroadcastReceiver used to listen for new Apps that have been added by
	 * the AppDownloadService. */
	private DownloadCompleteReceiver m_receiver; 
	
	/** The ID of the CursorLoader to be initialized in the LoaderManager and used to load a Cursor. */
	private static final int LOADER_ID = 1;
	
	private static final int MARKET_REQUEST_CODE = 2;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.app_list);
        
        // Initialize cursor adapter
        this.m_appAdapter = new AppCursorAdapter(this, null, 0);
        
        //Initialize the LoaderManager, causing it to set up a CursorLoader.
		this.getSupportLoaderManager().initLoader(LOADER_ID, null, this);
        
        //Initialize View Components, similar to how initLayout() was done in the past.
        this.m_vwAppList = (ListView)findViewById(R.id.m_vwAppList);
        this.m_vwAppList.setOnItemClickListener(this);
        this.m_vwAppList.setAdapter(this.m_appAdapter);
    }
    
    @Override
    public void onResume() {
    	IntentFilter filter = new IntentFilter(DownloadCompleteReceiver.ACTION_NEW_APP_TO_REVIEW);
    	filter.addCategory(Intent.CATEGORY_DEFAULT);
    	this.m_receiver = new DownloadCompleteReceiver();
    	registerReceiver(this.m_receiver, filter);
    	
    	fillData();
    	
    	super.onResume();
    }
    
    @Override
    public void onPause() {
    	unregisterReceiver(this.m_receiver);
    	
    	super.onPause();
    }
	
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {		
		App clickedApp = ((AppView) view).getApp();
		
		if (clickedApp.isInstalled() == false) {
			Intent playStoreIntent = new Intent(Intent.ACTION_VIEW);
			String installAppUri = clickedApp.getInstallURI();
			playStoreIntent.setData(Uri.parse(installAppUri));
			startActivityForResult(playStoreIntent, MARKET_REQUEST_CODE);
		}
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == MARKET_REQUEST_CODE) {
			fillData();
		}
	}
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	MenuInflater inflater = this.getSupportMenuInflater();
		inflater.inflate(R.menu.mainmenu, menu);
		return true;
    }
    
    @Override
	public boolean onOptionsItemSelected(MenuItem item) {
    	if (item.getItemId() == R.id.menu_startDownload) {
    		//Toast.makeText(this, "Start Download button pressed.", Toast.LENGTH_LONG).show();
    		Intent downloadIntent = new Intent(this, AppDownloadService.class);
    		startService(downloadIntent);
    	}
    	else if (item.getItemId() == R.id.menu_stopDownload) {
    		Intent stopDownloadIntent = new Intent(this, AppDownloadService.class);
    		stopService(stopDownloadIntent);
    	}
    	else if (item.getItemId() == R.id.menu_removeAll) {
    		Uri removeAllAppsUri = Uri.parse(AppContentProvider.CONTENT_URI + "/apps");
    		getContentResolver().delete(removeAllAppsUri, null, null);
    	}
    	
    	return super.onOptionsItemSelected(item);
    }

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		String[] projection = { AppTable.APP_KEY_ID, AppTable.APP_KEY_NAME, AppTable.APP_KEY_RATING,
			AppTable.APP_KEY_INSTALLURI, AppTable.APP_KEY_INSTALLED };
		
		Uri uri = Uri.parse(AppContentProvider.CONTENT_URI + "/apps");
		
		CursorLoader cursorLoader = new CursorLoader(this, uri, projection, null, null, AppTable.ORDER_BY_STRING);
		return cursorLoader;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		this.m_appAdapter.swapCursor(cursor);
		this.m_appAdapter.setOnAppChangeListener(this);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		this.m_appAdapter.swapCursor(null);
	}

	/**
	 * The event handler for managing App changes. Updates the database app table and the
	 * AppView bound to the underlying App that got changed.<br><br>
	 * 
	 * Specified by <b>onAppChanged(...)</b> in AppView.<br><br>
	 */
	@Override
	public void onAppChanged(AppView view, App app) {
		Uri uri = Uri.parse(AppContentProvider.CONTENT_URI + "/apps/" + app.getID());
		
		ContentValues cv = new ContentValues();
		cv.put(AppTable.APP_KEY_NAME, app.getName());
		cv.put(AppTable.APP_KEY_RATING, app.getRating());
		cv.put(AppTable.APP_KEY_INSTALLURI, app.getInstallURI());
		cv.put(AppTable.APP_KEY_INSTALLED, app.isInstalled() ? 1 : 0);
		
		this.getContentResolver().update(uri, cv, null, null);
		
		this.m_appAdapter.setOnAppChangeListener(null);
		
		fillData();
	}
	
	/**
	 * Update and refresh all list data in the application: The Cursor from the CursorLoader,
	 * the adapter (through <b>restartLoader()</b>) and sets the latter as the ListView's new
	 * adapter.
	 */
	public void fillData() {
		this.getSupportLoaderManager().restartLoader(LOADER_ID, null, this);
		this.m_appAdapter.notifyDataSetChanged();
	}
	
	/**
	 * BroadcastReceiver that checks for broadcasted Intents with a specific action as
	 * specified in AppDownloadService. Also responsible for producing and showing
	 * Notifications in the Notification Bar when an app is added.
	 */
	public class DownloadCompleteReceiver extends BroadcastReceiver {

		public static final String ACTION_NEW_APP_TO_REVIEW = "edu.calpoly.android.apprater.MESSAGE_PROCESSED";
		
		@Override
		public void onReceive(Context context, Intent intent) {
			if(intent.getAction().equals(ACTION_NEW_APP_TO_REVIEW)) {
				//Toast.makeText(getBaseContext(), getResources().getString(R.string.newAppToast), Toast.LENGTH_LONG).show();
				fillData();
				showNotification(getBaseContext());
			}
		}

		/**
		 * Creates, initializes and sends a Notification to the Notification Bar.
		 * 
		 * @param context
		 * 					The context this receiver runs in. 
		 */
		private void showNotification(Context context) {
			Resources resource = getResources();
			PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, new Intent(context, AppRater.class), 0);
			NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
			builder.setContentTitle(resource.getString(R.string.newAppNotificationOriginName));
			builder.setContentText(resource.getString(R.string.newAppNotificationText));
			builder.setTicker(resource.getString(R.string.newAppNotificationTicker));
			builder.setSmallIcon(R.drawable.icon);
			
			builder.setContentIntent(pendingIntent);
			builder.setAutoCancel(true);
			builder.setDefaults(Notification.DEFAULT_ALL);
			
			NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
			manager.notify(0, builder.build());
		}
	}
}