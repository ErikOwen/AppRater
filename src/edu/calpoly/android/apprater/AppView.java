package edu.calpoly.android.apprater;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.CheckBox;
import android.widget.RatingBar;
import android.widget.Toast;
import android.widget.RatingBar.OnRatingBarChangeListener;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class AppView extends RelativeLayout implements OnRatingBarChangeListener {

	/** The data behind this View. Contains the app's information. */
	private App m_app;
	
	/** The container ViewGroup for all other Views in an AppView.
	 * Used to set the view's background color dynamically. */
	private RelativeLayout m_vwContainer;
	
	/** Indicates whether or not the App is installed.
	 * This must be set to non-interactive in the XML layout file. */
	private CheckBox m_vwInstalledCheckBox;
	
	/** Shows the user's current rating for the application. */
	private RatingBar m_vwAppRatingBar;
	
	/** The name of the App. */
	private TextView m_vwAppName;
	
	/** The context this view is in. Used for checking install status. */
	private Context context;

	/** Interface between this AppView and the database it's stored in. */
	private OnAppChangeListener m_onAppChangeListener;
	
	public AppView(Context context, App app) {
		super(context);

		this.context = context;

		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		inflater.inflate(R.layout.app_view, this);
		
		this.m_vwContainer = (RelativeLayout) findViewById(R.id.appLayout);
		this.m_vwInstalledCheckBox = (CheckBox) findViewById(R.id.installedCheckbox);
		this.m_vwAppRatingBar = (RatingBar) findViewById(R.id.appRatingBar);
		this.m_vwAppName = (TextView) findViewById(R.id.appName);
		
		setApp(app);
		
		this.m_onAppChangeListener = null;
		this.m_vwAppRatingBar.setOnRatingBarChangeListener(this);
	}
	
	public App getApp() {
		return m_app;
	}
	
	public void setApp(App app) {
		this.m_app = app;
		PackageManager manager = this.context.getPackageManager();
		Log.w("edu.calpoly.android.apprater", "getInstallUri is: " + this.m_app.getInstallURI());
		String packageName = this.m_app.getInstallURI().substring(this.m_app.getInstallURI().indexOf("=") + 1);
		try {
			PackageInfo info = manager.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
			this.m_app.setInstalled(true);
			this.m_vwInstalledCheckBox.setChecked(true);
		}
		catch (NameNotFoundException e) {
			this.m_app.setInstalled(false);
			this.m_vwInstalledCheckBox.setChecked(false);
		}
		notifyOnAppChangeListener();
		this.m_vwAppName.setText(this.m_app.getName());
		
		if (!this.m_app.isInstalled()) {
			this.m_vwContainer.setBackgroundColor(getResources().getColor(R.color.new_app));
		}
		else {
			if (this.m_app.getRating() == 0) {
				this.m_vwContainer.setBackgroundColor(getResources().getColor(R.color.installed_app));
			}
			else {
				this.m_vwContainer.setBackgroundColor(getResources().getColor(R.color.rated_app));
			}
		}
		
		this.m_vwAppRatingBar.setRating(this.m_app.getRating());
		/*try {
			PackageInfo info = manager.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
			this.m_app.setInstalled(true);
			this.m_vwInstalledCheckBox.setChecked(true);
			this.m_vwAppRatingBar.setIsIndicator(false);
			this.m_vwAppRatingBar.setRating(this.m_app.getRating());
			
			if (this.m_app.getRating() == 0) {
				this.m_vwContainer.setBackgroundColor(getResources().getColor(R.color.installed_app));
			}
			else {
				this.m_vwContainer.setBackgroundColor(getResources().getColor(R.color.rated_app));
			}
			
		}
		catch (NameNotFoundException e) {
			this.m_app.setInstalled(false);
			this.m_vwAppRatingBar.setIsIndicator(false);
			this.m_vwAppRatingBar.setRating(0);
			this.m_vwInstalledCheckBox.setChecked(false);
			this.m_vwContainer.setBackgroundColor(getResources().getColor(R.color.new_app));
		}*/
		
		this.notifyOnAppChangeListener();
	}
	
	/**
	 * Mutator method for changing the OnAppChangeListener object this AppView
	 * notifies when the state its underlying App object changes.
	 * 
	 * It is possible and acceptable for m_onAppChangeListener to be null, you
	 * should allow for this.
	 * 
	 * @param listener
	 *            The OnAppChangeListener object that should be notified when
	 *            the underlying App changes state.
	 */
	public void setOnAppChangeListener(OnAppChangeListener listener) {
		this.m_onAppChangeListener = listener;
	}

	/**
	 * This method should always be called after the state of m_app is changed.
	 * 
	 * It is possible and acceptable for m_onAppChangeListener to be null, you
	 * should test for this.
	 */
	protected void notifyOnAppChangeListener() {
		if (m_onAppChangeListener != null) {
			m_onAppChangeListener.onAppChanged(this, m_app);
		}
	}
	
	/**
	 * Interface definition for a callback to be invoked when the underlying
	 * App is changed in this AppView object.
	 */
	public static interface OnAppChangeListener {

		/**
		 * Called when the underlying App in an AppView object changes state.
		 * 
		 * @param view
		 *            The AppView in which the App was changed.
		 * @param app
		 *            The App that was changed.
		 */
		public void onAppChanged(AppView view, App app);
	}

	@Override
	public void onRatingChanged(RatingBar bar, float rating, boolean fromUser) {
		this.m_app.setRating(rating);
		//this.m_vwAppRatingBar.setRating(rating);
		//Toast.makeText(getContext(), "Rating button changed", Toast.LENGTH_LONG).show();
		notifyOnAppChangeListener();
		
	}
}