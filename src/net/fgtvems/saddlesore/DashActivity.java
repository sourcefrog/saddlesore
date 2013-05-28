package net.fgtvems.saddlesore;

import net.fgtvems.saddlesore.DashActivity.ActivityCallbacks;
import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentSender;
import android.location.Location;
import android.os.Bundle;
import android.text.format.Time;
import android.util.Log;
import android.view.Menu;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.ActivityRecognitionClient;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;

public class DashActivity extends Activity implements GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener, LocationListener {

    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
    private LocationClient mLocationClient;
    private TextView mAddress;
    private LocationRequest mLocationRequest;

    private PendingIntent mActivityRecognitionPendingIntent;
    private ActivityRecognitionClient mActivityRecognitionClient;
    private TextView mActivityText;
    private ActivityCallbacks mActivityCallbacks;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dash);
        checkServicesConnected();

        mLocationClient = new LocationClient(this, this, this);
        mAddress = (TextView) findViewById(R.id.positionText);
        mActivityText = (TextView) findViewById(R.id.activityText);

        mLocationRequest = LocationRequest.create();
        // Use high accuracy
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        mLocationRequest.setInterval(1000 * 60 * 5);
        mLocationRequest.setFastestInterval(1000 * 60 * 1);

        mActivityCallbacks = new ActivityCallbacks();
        mActivityRecognitionClient = new ActivityRecognitionClient(this, mActivityCallbacks, this);

        Intent intent = new Intent(this, ActivityRecognitionIntentService.class);

        mActivityRecognitionPendingIntent = PendingIntent.getService(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        mActivityRecognitionClient.connect();
    }

    class ActivityCallbacks implements GooglePlayServicesClient.ConnectionCallbacks {

        @Override
        public void onConnected(Bundle arg0) {
            mActivityRecognitionClient.requestActivityUpdates(1000 * 60 * 5,
                    mActivityRecognitionPendingIntent);
        }

        @Override
        public void onDisconnected() {

        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mLocationClient.connect();
    }

    @Override
    protected void onStop() {
        mLocationClient.disconnect();
        super.onStop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.dash, menu);
        return true;
    }

    // TODO: Move this to its own class.
    private boolean checkServicesConnected() {
        // Check that Google Play services is available
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        // If Google Play services is available
        if (ConnectionResult.SUCCESS == resultCode) {
            // In debug mode, log the status
            Log.d("Location Updates", "Google Play services is available.");
            // Continue
            return true;
            // Google Play services was not available for some reason
        } else {
            // Get the error code
            // Get the error dialog from Google Play services
            Dialog errorDialog = GooglePlayServicesUtil.getErrorDialog(resultCode, this,
                    CONNECTION_FAILURE_RESOLUTION_REQUEST);

            // If Google Play services can provide an error dialog
            if (errorDialog != null) {
                // Create a new DialogFragment for the error dialog
                ErrorDialogFragment errorFragment = new ErrorDialogFragment();
                // Set the dialog in the DialogFragment
                errorFragment.setDialog(errorDialog);
                // Show the error dialog in the DialogFragment
                errorFragment.show(getFragmentManager(), "Location Updates");

            }
            return false;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
        case CONNECTION_FAILURE_RESOLUTION_REQUEST:
            if (resultCode == Activity.RESULT_OK) {
                // Try the request again
                checkServicesConnected();
            }
        }
    }

    // Define a DialogFragment that displays the error dialog
    public static class ErrorDialogFragment extends DialogFragment {
        // Global field to contain the error dialog
        private Dialog mDialog;

        // Default constructor. Sets the dialog field to null
        public ErrorDialogFragment() {
            super();
            mDialog = null;
        }

        // Set the dialog to display
        public void setDialog(Dialog dialog) {
            mDialog = dialog;
        }

        // Return a Dialog to the DialogFragment.
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return mDialog;
        }
    }

    /*
     * Called by Location Services when the request to connect the client
     * finishes successfully. At this point, you can request the current
     * location or start periodic updates
     */
    @Override
    public void onConnected(Bundle dataBundle) {
        showCurrentLocation(mLocationClient.getLastLocation(), null);
        mLocationClient.requestLocationUpdates(mLocationRequest, this);
    }

    void showCurrentLocation(Location currentLocation, String address) {
        if (currentLocation == null)
            return;
        Time fixTime = new Time();
        fixTime.set(currentLocation.getTime());
        String s = currentLocation.toString() + "\n" + fixTime.format3339(false);
        if (address != null) {
            s += "\n" + address;
        }
        mAddress.setText(s);
    }

    @Override
    public void onDisconnected() {
        // Display the connection status
        Toast.makeText(this, "Disconnected. Please re-connect.", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onLocationChanged(Location location) {
        // TODO: Only do lookups when the display is visible?
        (new AddressLookupTask(this, this)).execute(location);
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        /*
         * Google Play services can resolve some errors it detects. If the error
         * has a resolution, try sending an Intent to start a Google Play
         * services activity that can resolve error.
         */
        if (connectionResult.hasResolution()) {
            try {
                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(this,
                        CONNECTION_FAILURE_RESOLUTION_REQUEST);
                /*
                 * Thrown if Google Play services canceled the original
                 * PendingIntent
                 */
            } catch (IntentSender.SendIntentException e) {
                // Log the error
                e.printStackTrace();
            }
        } else {
            Toast.makeText(this, "No resolution available.", Toast.LENGTH_SHORT).show();
        }
    }

    void showActivity(String activityName) {
        mActivityText.setText(activityName);
    }
}
