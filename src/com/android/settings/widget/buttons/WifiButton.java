package com.android.settings.widget.buttons;

import com.android.settings.R;
import com.android.settings.widget.SettingsAppWidgetProvider;
import com.android.settings.widget.StateTracker;
import com.android.settings.widget.WidgetSettings;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.util.Log;

public class WifiButton extends WidgetButton{

	static WifiButton ownButton=null;


	private static final StateTracker sWifiState = new WifiStateTracker();

	/**
	 * Subclass of StateTracker to get/set Wifi state.
	 */
	private static final class WifiStateTracker extends StateTracker {
		@Override
		public int getActualState(Context context) {
			//SettingsAppWidgetProvider.logD("Wifi: getActualState");		
			WifiManager wifiManager = (WifiManager) context
			.getSystemService(Context.WIFI_SERVICE);
			if (wifiManager != null) {
				return wifiStateToFiveState(wifiManager.getWifiState());
			}
			return SettingsAppWidgetProvider.STATE_UNKNOWN;
		}

		@Override
		protected void requestStateChange(Context context,
				final boolean desiredState) {
			//SettingsAppWidgetProvider.logD("Wifi: requestStateChange");		

			final WifiManager wifiManager = (WifiManager) context
			.getSystemService(Context.WIFI_SERVICE);
			if (wifiManager == null) {
				Log.d(SettingsAppWidgetProvider.TAG, "No wifiManager.");
				return;
			}

			// Actually request the wifi change and persistent
			// settings write off the UI thread, as it can take a
			// user-noticeable amount of time, especially if there's
			// disk contention.
			new AsyncTask<Void, Void, Void>() {
				@Override
				protected Void doInBackground(Void... args) {
					/**
					 * Disable tethering if enabling Wifi
					 */
					int wifiApState = wifiManager.getWifiApState();
					if (desiredState
							&& ((wifiApState == WifiManager.WIFI_AP_STATE_ENABLING) || (wifiApState == WifiManager.WIFI_AP_STATE_ENABLED))) {
						wifiManager.setWifiApEnabled(null, false);
					}

					wifiManager.setWifiEnabled(desiredState);
					return null;
				}
			}.execute();
		}

		@Override
		public void onActualStateChange(Context context, Intent intent) {
			//SettingsAppWidgetProvider.logD("Wifi: onActualStateChange");		

			if (!WifiManager.WIFI_STATE_CHANGED_ACTION.equals(intent
					.getAction())) {
				return;
			}
			int wifiState = intent
			.getIntExtra(WifiManager.EXTRA_WIFI_STATE, -1);
			int widgetState=wifiStateToFiveState(wifiState);
			setCurrentState(context, widgetState);
		}

		/**
		 * Converts WifiManager's state values into our Wifi/Bluetooth-common
		 * state values.
		 */
		private static int wifiStateToFiveState(int wifiState) {
			switch (wifiState) {
			case WifiManager.WIFI_STATE_DISABLED:
				return SettingsAppWidgetProvider.STATE_DISABLED;
			case WifiManager.WIFI_STATE_ENABLED:
				return SettingsAppWidgetProvider.STATE_ENABLED;
			case WifiManager.WIFI_STATE_DISABLING:
				return SettingsAppWidgetProvider.STATE_TURNING_OFF;
			case WifiManager.WIFI_STATE_ENABLING:
				return SettingsAppWidgetProvider.STATE_TURNING_ON;
			default:
				return SettingsAppWidgetProvider.STATE_UNKNOWN;
			}
		}
	}



	public void updateState(Context context,
			SharedPreferences globalPreferences, int[] appWidgetIds) {	
		//SettingsAppWidgetProvider.logD("Wifi: updateState");

		currentState=sWifiState.getTriState(context);
		switch (currentState) {
		case SettingsAppWidgetProvider.STATE_DISABLED:
			currentIcon=R.drawable.ic_appwidget_settings_wifi_off;
			break;
		case SettingsAppWidgetProvider.STATE_ENABLED:
			currentIcon=R.drawable.ic_appwidget_settings_wifi_on;
			break;
		case SettingsAppWidgetProvider.STATE_INTERMEDIATE:
			// In the transitional state, the bottom green bar
			// shows the tri-state (on, off, transitioning), but
			// the top dark-gray-or-bright-white logo shows the
			// user's intent. This is much easier to see in
			// sunlight.
			if (sWifiState.isTurningOn()) {
				currentIcon=R.drawable.ic_appwidget_settings_wifi_on;
			} else {
				currentIcon=R.drawable.ic_appwidget_settings_wifi_off;
			}
			break;
		}
	}


	public void onReceive(Context context, Intent intent) {
		//SettingsAppWidgetProvider.logD("Wifi: onReceive");		
		sWifiState.onActualStateChange(context, intent);		
	}


	public void toggleState(Context context) {
		//SettingsAppWidgetProvider.logD("Wifi: toggleState");

		int realstate = sWifiState.getActualState(context);		
		sWifiState.toggleState(context);

		SharedPreferences preferences = context.getSharedPreferences(WidgetSettings.WIDGET_PREF_MAIN,
				Context.MODE_PRIVATE);

		if (realstate==SettingsAppWidgetProvider.STATE_DISABLED && preferences.getBoolean(WidgetSettings.AUTO_DISABLE_3G_WITH_WIFI, false)){
			//SettingsAppWidgetProvider.logD("Wifi: will enable Sync");
			NetworkModeButton.getInstance().toggleState(context, SettingsAppWidgetProvider.STATE_DISABLED);
		} else if (realstate==SettingsAppWidgetProvider.STATE_ENABLED && preferences.getBoolean(WidgetSettings.AUTO_ENABLE_3G_WITH_WIFI, false)){
			//SettingsAppWidgetProvider.logD("Wifi: will disable Sync");
			NetworkModeButton.getInstance().toggleState(context, SettingsAppWidgetProvider.STATE_ENABLED);
		} 
		
		if (realstate==SettingsAppWidgetProvider.STATE_DISABLED && preferences.getBoolean(WidgetSettings.AUTO_ENABLE_SYNC_WITH_WIFI, false)){
			//SettingsAppWidgetProvider.logD("Wifi: will enable Sync");
			SyncButton.getInstance().toggleState(context, SettingsAppWidgetProvider.STATE_ENABLED);
		} else if (realstate==SettingsAppWidgetProvider.STATE_ENABLED && preferences.getBoolean(WidgetSettings.AUTO_DISABLE_SYNC_WITH_WIFI, false)){
			//SettingsAppWidgetProvider.logD("Wifi: will disable Sync");
			SyncButton.getInstance().toggleState(context, SettingsAppWidgetProvider.STATE_DISABLED);				
		}
	}


	public static WifiButton getInstance() {
		//SettingsAppWidgetProvider.logD("Wifi: Getting Instance");

		if (ownButton==null) {
			//SettingsAppWidgetProvider.logD("Wifi: New Wifi instance");
			ownButton = new WifiButton();
		}

		return ownButton;
	}

	@Override
	void initButton() {
		//SettingsAppWidgetProvider.logD("Wifi: Init Button");
		buttonID=WidgetButton.BUTTON_WIFI;
		isDefault=true;
		preferenceName=WidgetSettings.TOGGLE_WIFI;

		buttonLayout=R.id.btn_wifi;
		buttonSep=R.id.sep_wifi;
		buttonIcon=R.id.img_wifi;
		buttonState=R.id.ind_wifi;

	}


}
