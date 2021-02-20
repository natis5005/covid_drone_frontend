package com.CovidDrone.ui;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.arch.lifecycle.ViewModelProvider;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.CovidDrone.R;
import com.CovidDrone.UseCases.Request;
import com.CovidDrone.UserClient;
import com.CovidDrone.adapters.RequestRecyclerAdapter;
import com.CovidDrone.models.User;
import com.CovidDrone.models.UserLocation;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.type.Date;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nullable;

import static com.CovidDrone.Constants.ERROR_DIALOG_REQUEST;
import static com.CovidDrone.Constants.PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION;
import static com.CovidDrone.Constants.PERMISSIONS_REQUEST_ENABLE_GPS;


public class MainActivity extends AppCompatActivity implements
        View.OnClickListener,
        RequestRecyclerAdapter.RequestRecyclerClickListener
{

    private static final String TAG = "MainActivity";
    public static final String EXTRA_MESSAGE = "com.CovidDrone.ui.requestName";

    //widgets
    private ProgressBar mProgressBar;

    //vars
    /*private ArrayList<Chatroom> mChatrooms = new ArrayList<>();
    private Set<String> mChatroomIds = new HashSet<>();
    private ChatroomRecyclerAdapter mChatroomRecyclerAdapter;
    private RecyclerView mChatroomRecyclerView;
    private ListenerRegistration mChatroomEventListener;
    */

    private ArrayList<Request> mRequests = new ArrayList<>();
    private Set<String> mRequestIds = new HashSet<>();
    private RequestRecyclerAdapter mRequestRecyclerAdapter;
    private RecyclerView mRequestRecyclerView;
    private ListenerRegistration mRequestEventListener;

    private FirebaseFirestore mDb;
    private boolean mLocationPermissionGranted = false;
    private FusedLocationProviderClient mFusedLocationClient;
    private UserLocation mUserData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);


        mProgressBar = findViewById(R.id.progressBar);
        // mChatroomRecyclerView = findViewById(R.id.chatrooms_recycler_view);
        mRequestRecyclerView = findViewById(R.id.chatrooms_recycler_view);
        findViewById(R.id.fab_create_request).setOnClickListener(this);

        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setTimestampsInSnapshotsEnabled(true)
                .setPersistenceEnabled(true)
                .build();

        mDb = FirebaseFirestore.getInstance();
        mDb.setFirestoreSettings(settings);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mUserData = new UserLocation();
        mUserData.setUser(((UserClient)(getApplicationContext())).getUser());

        initSupportActionBar();
        initRequestRecyclerView();
    }

    @Override
    public void onStart() {
        super.onStart();

        if(checkMapServices()){
            if(!mLocationPermissionGranted){
                getLocationPermission();
            }
        }

        getUserDetails();
        getRequests();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        mLocationPermissionGranted = false;
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mLocationPermissionGranted = true;
                }
            }
        }
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.fab_create_request) {
            newRequestDialog();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mRequestEventListener != null){
            mRequestEventListener.remove();
        }
    }

    @Override
    public void onRequestSelected(int position) {
        navRequestActivity(mRequests.get(position));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case R.id.action_sign_out:{
                signOut();
                return true;
            }
            case R.id.action_profile:{
                startActivity(new Intent(this, ProfileActivity.class));
                return true;
            }
            default:{
                return super.onOptionsItemSelected(item);
            }
        }

    }

    private void getLastKnownLocation() {
        Log.d(TAG, "getLastKnownLocation: called1.");


        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mFusedLocationClient.getLastLocation().addOnCompleteListener(new OnCompleteListener<android.location.Location>() {
            @Override
            public void onComplete(@NonNull Task<android.location.Location> task) {
                if (task.isSuccessful()) {
                    Log.i(TAG, "task success");
                    Location location = task.getResult();
                    if (location == null){
                        Log.i(TAG, "location null");
                    }
                    GeoPoint geoPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
                    mUserData.setGeo_point(geoPoint);
                    mUserData.setTimestamp(null);
                    saveUserLocation();
                }
            }
        });

    }

    private void saveUserLocation(){

        if(mUserData != null){
            DocumentReference locationRef = mDb
                    .collection(getString(R.string.collection_user_locations))
                    .document(FirebaseAuth.getInstance().getUid());

            locationRef.set(mUserData).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if(task.isSuccessful()){
                        Log.d(TAG, "saveUserLocation: \ninserted user location into database." +
                                "\n latitude: " + mUserData.getGeo_point().getLatitude() +
                                "\n longitude: " + mUserData.getGeo_point().getLongitude());
                    }
                }
            });
        }
    }

    private boolean checkMapServices(){
        if(isServicesOK()){
            if(isMapsEnabled()){
                return true;
            }
        }
        return false;
    }

    public boolean isMapsEnabled(){
        final LocationManager manager = (LocationManager) getSystemService( Context.LOCATION_SERVICE );

        if ( !manager.isProviderEnabled( LocationManager.GPS_PROVIDER ) ) {
            buildAlertMessageNoGps();
            return false;
        }
        return true;
    }

    private void buildAlertMessageNoGps() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("This application requires GPS to work properly, do you want to enable it?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        Intent enableGpsIntent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivityForResult(enableGpsIntent, PERMISSIONS_REQUEST_ENABLE_GPS);
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }

    private void getLocationPermission() {
        /*
         * Request location permission, so that we can get the location of the
         * device. The result of the permission request is handled by a callback,
         * onRequestPermissionsResult.
         */
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }

    public boolean isServicesOK(){
        Log.d(TAG, "isServicesOK: checking google services version");

        int available = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(MainActivity.this);

        if(available == ConnectionResult.SUCCESS){
            //everything is fine and the user can make map requests
            Log.d(TAG, "isServicesOK: Google Play Services is working");
            return true;
        }
        else if(GoogleApiAvailability.getInstance().isUserResolvableError(available)){
            //an error occured but we can resolve it
            Log.d(TAG, "isServicesOK: an error occured but we can fix it");
            Dialog dialog = GoogleApiAvailability.getInstance().getErrorDialog(MainActivity.this, available, ERROR_DIALOG_REQUEST);
            dialog.show();
        }else{
            Toast.makeText(this, "You can't make map requests", Toast.LENGTH_SHORT).show();
        }
        return false;
    }

    private void initSupportActionBar(){
        setTitle("Requests");
    }

    private void initRequestRecyclerView(){
        mRequestRecyclerAdapter = new RequestRecyclerAdapter(mRequests, this);
        mRequestRecyclerView.setAdapter(mRequestRecyclerAdapter);
        mRequestRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    private void getUserDetails(){
        if(mUserData != null || mUserData.getUser() != null){
            Log.e(TAG, "user is not null");
            getLastKnownLocation();
        }
        else{
            Log.d(TAG, "user is null");
            mUserData = new UserLocation();
            getLastKnownLocation();
            DocumentReference docRef = mDb.collection(getString(R.string.collection_users))
                    .document(FirebaseAuth.getInstance().getUid());
            docRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                @Override
                public void onSuccess(DocumentSnapshot documentSnapshot) {
                    User user = documentSnapshot.toObject(User.class);
                    mUserData.setUser(user);
                }
            });

        }
    }

    private void getRequests(){
        CollectionReference requestsCollection = mDb
                .collection(getString(R.string.collection_requests));

        mRequestEventListener = requestsCollection.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                Log.d(TAG, "onEvent: called.");

                if (e != null) {
                    Log.e(TAG, "onEvent: Listen failed.", e);
                    return;
                }

                if(queryDocumentSnapshots != null){
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {

                        Request request = doc.toObject(Request.class);
                        //only add requests this user has made
                        if(!mRequestIds.contains(request.getRequestId()) &&
                        request.getUserData().getUser().getUsername().equals(mUserData.getUser().getUsername())){

                            mRequestIds.add(request.getRequestId());
                            mRequests.add(request);
                        }
                    }
                    Log.d(TAG, "onEvent: number of requests: " + mRequests.size());
                    mRequestRecyclerAdapter.notifyDataSetChanged();
                }

            }
        });
    }

    private void buildNewRequest(final String requestName){

        DocumentReference newRequestRef = mDb
                .collection(getString(R.string.collection_requests))
                .document();

        final Request request = new Request(requestName, mUserData, newRequestRef.getId());

        newRequestRef.set(request).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                hideDialog();

                if(task.isSuccessful()){
                    navRequestActivity(request);
                }else{
                    View parentLayout = findViewById(android.R.id.content);
                    Snackbar.make(parentLayout, "Something went wrong.", Snackbar.LENGTH_SHORT).show();
                }
            }
        });

        CollectionReference requestsCollection = mDb.collection(getString(R.string.collection_requests));

        mRequestEventListener = requestsCollection.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                Log.d(TAG, "onEvent queeeeerryyyyy: called.");

                if (e != null) {
                    Log.e(TAG, "onEvent: Listen failed.", e);
                    return;
                }

                if (queryDocumentSnapshots != null) {
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {

                        Request request = doc.toObject(Request.class);

                        if (request.getTitle().equals(requestName)){
                            Log.d(TAG, " new chatroom id to encode on QR code:" + request.getRequestId());
                        }
                    }
                }
            }
        });
    }

    private void navRequestActivity(Request request){
        Intent intent = new Intent(MainActivity.this, RequestActivity.class);
        intent.putExtra(getString(R.string.intent_chatroom), request);
        startActivity(intent);
    }

   /* private void newChatroomDialog(){

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter a request name");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setPositiveButton("CREATE", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(!input.getText().toString().equals("")){
                    buildNewChatroom(input.getText().toString());
                }
                else {
                    Toast.makeText(MainActivity.this, "Enter a request name", Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }*/

    private void newRequestDialog() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Create new request for COVID-19 swab?");

        builder.setPositiveButton("CREATE", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String requestName = "Request" + mUserData.getUser().getUsername() + mRequests.size();
                buildNewRequest(requestName);
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    private void signOut(){
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showDialog(){
        mProgressBar.setVisibility(View.VISIBLE);
    }

    private void hideDialog(){
        mProgressBar.setVisibility(View.GONE);
    }

}