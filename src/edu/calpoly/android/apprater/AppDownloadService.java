package edu.calpoly.android.apprater;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

/**
 * The Service that performs app information downloading, performing the check again
 * occasionally over time. It adds an application to the list of apps and is also
 * responsible for telling the BroadcastReceiver when it has done so.
 */
public class AppDownloadService extends android.app.IntentService {

	/** The ID for the Notification that is generated when a new App is added. */
	public static final int NEW_APP_NOTIFICATION_ID = 1;
	
	/** The Timer thread which will execute the check for new Apps. Acts like a Thread
	 * that can be told to start at a specific time and/or at specific time intervals. */
	private Timer m_updateTimer;
	
	/** The TimerTask which encapsulates the logic that will check for new Apps. This ends
	 * up getting run by the Timer in the same way that a Thread runs a Runnable. */
	private TimerTask m_updateTask;

	/** The time frequency at which the service should check the server for new Apps. */
	private static final long UPDATE_FREQUENCY = 10000L;

	/** A String containing the URL from which to download the list of all Apps. */
	public static final String GET_APPS_URL = "http://www.simexusa.com/aac/getAll.php";

	/**
	 * Note that the constructor that takes a String will NOT be properly instantiated.
	 * Use the constructor that takes no parameters instead, and pass in a String that
	 * contains the name of the service to the super() call.
	 */
	public AppDownloadService() {
		super("AppDownloadService");
	}

	@Override
	public void onCreate() {
		this.m_updateTimer = new Timer();
		this.m_updateTask = new TimerTask() {
			@Override
			public void run() {
				getAppsFromServer();
			}
		};
		
		super.onCreate();
	}
	
	@Override
	public void onDestroy() {
		this.m_updateTimer.cancel();
		super.onDestroy();
	}
	
	/**
	 * This method downloads all of the Apps from the App server. For each App,
	 * it checks the AppContentProvider to see if it has already been downloaded
	 * before. If it is new, then it adds it to the AppContentProvider by
	 * calling addNewApp.
	 */
	private void getAppsFromServer() {
		try {
			URL url = new URL(GET_APPS_URL);
			Scanner scan = new Scanner(url.openStream());
			scan.useDelimiter(";");
			
			while (scan.hasNext()) {
				String appInfo = scan.next();
				if (appInfo.length() > 1) {
					String appName = appInfo.substring(0, appInfo.indexOf(","));
					String appInstall = appInfo.substring(appInfo.indexOf(',') + 1);
					
					Log.w("edu.calpoly.android.apprater", "appName is: " + appName);
					Log.w("edu.calpoly.android.apprater", "appInstall is: " + appInstall);
					
					addNewApp(new App(appName, appInstall));
				}
			}
			
		}
		catch (MalformedURLException e) {
			Log.w("edu.calpoly.android.apprater", e.getMessage());
		}
		catch (IOException e) {
			Log.w("edu.calpoly.android.apprater", e.getMessage());
		}
	}

	/**
	 * This method adds a new App to the AppContentProvider.
	 * 
	 * @param app
	 *            The new App object to add to the ContentProvider.
	 */
	private void addNewApp(App app) {
		ContentResolver resolver = getContentResolver();
		Uri uri = Uri.parse(AppContentProvider.CONTENT_URI + "/apps/" + app.getName());
		String [] projection = {AppTable.APP_KEY_NAME, AppTable.APP_KEY_INSTALLED};
		Cursor cursor = resolver.query(uri, projection, null, null, null);
		
		if(cursor.getCount() == 0) {
			ContentValues values = new ContentValues();
			values.put(AppTable.APP_KEY_NAME, app.getName());
			values.put(AppTable.APP_KEY_RATING, app.getRating());
			values.put(AppTable.APP_KEY_INSTALLURI, app.getInstallURI());
			if (app.isInstalled()) {
				values.put(AppTable.APP_KEY_INSTALLED, 1);
			}
			else {
				values.put(AppTable.APP_KEY_INSTALLED, 0);
			}
			
			Uri toInsertUri = Uri.parse(AppContentProvider.CONTENT_URI + "/apps/" + app.getID());
			Uri insertionResultUri = resolver.insert(toInsertUri, values);
			
			app.setID(Long.valueOf(insertionResultUri.getLastPathSegment()));
			
			announceNewApp();
			
			cursor.close();
		}
	}

	/**
	 * This method broadcasts an intent with a specific Action String. This method should be
	 * called when a new App has been downloaded and added successfully.
	 */
	private void announceNewApp() {
		Intent newAppIntent = new Intent(AppRater.DownloadCompleteReceiver.ACTION_NEW_APP_TO_REVIEW);
		newAppIntent.addCategory(Intent.CATEGORY_DEFAULT);
		sendBroadcast(newAppIntent);
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		this.m_updateTimer.scheduleAtFixedRate(this.m_updateTask, 0, UPDATE_FREQUENCY);
		
	}
}
