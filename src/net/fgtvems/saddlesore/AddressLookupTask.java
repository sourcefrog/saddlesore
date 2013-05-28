package net.fgtvems.saddlesore;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.util.Log;


/**
 * A subclass of AsyncTask that calls getFromLocation() in the
 * background. The class definition has these generic types:
 * Location - A Location object containing
 * the current location.
 * Void     - indicates that progress units are not used
 * String   - An address passed to onPostExecute()
 */
class AddressLookupTask extends
AsyncTask<Location, Void, String> {
	Context mContext;
	private DashActivity mActivity;
	private Location mLocation;
	public AddressLookupTask(Context context, DashActivity activity) {
		super();
		mContext = context;
		mActivity = activity;
	}

	/**
	 * Get a Geocoder instance, get the latitude and longitude
	 * look up the address, and return it
	 *
	 * @params params One or more Location objects
	 * @return A string containing the address of the current
	 * location, or an empty string if no address can be found,
	 * or an error message
	 */
	// TODO: Retry a little on errors?
	@Override
	protected String doInBackground(Location... params) {
		Geocoder geocoder =
				new Geocoder(mContext, Locale.getDefault());
		// Get the current location from the input parameter list
		Location loc = params[0];
		mLocation = loc;
		// Create a list to contain the result address
		List<Address> addresses = null;
		try {
			addresses = geocoder.getFromLocation(loc.getLatitude(),
					loc.getLongitude(), 1);
		} catch (IOException e1) {
			Log.e("LocationSampleActivity",
					"IO Exception in getFromLocation()");
			e1.printStackTrace();
			return ("IO Exception trying to get address");
		} catch (IllegalArgumentException e2) {
			// Error message to post in the log
			String errorString = "Illegal arguments " +
					Double.toString(loc.getLatitude()) +
					" , " +
					Double.toString(loc.getLongitude()) +
					" passed to address service";
			Log.e("LocationSampleActivity", errorString);
			e2.printStackTrace();
			return errorString;
		}
		// If the reverse geocode returned an address
		if (addresses != null && addresses.size() > 0) {
			// Get the first address
			Address address = addresses.get(0);
			/*
			 * Format the first line of address (if available),
			 * city, and country name.
			 */
			String addressText = String.format(
					"%s, %s, %s",
					address.getLocality(),		// Locality is usually a city
					address.getAdminArea(), // state
					address.getCountryName());
			// Return the text
			return addressText;
		} else {
			return "No address found";
		}
	}

	@Override
	protected void onPostExecute(String address) {
		// Display the results of the lookup.
		mActivity.showCurrentLocation(mLocation, address);
	}
}