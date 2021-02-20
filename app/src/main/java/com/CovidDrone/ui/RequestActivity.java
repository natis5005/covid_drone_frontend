package com.CovidDrone.ui;

import android.content.Intent;
import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.CovidDrone.R;
import com.CovidDrone.UseCases.Request;
import com.CovidDrone.UserClient;
import com.CovidDrone.models.Chatroom;
import com.CovidDrone.models.User;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;


public class RequestActivity extends AppCompatActivity implements OnMapReadyCallback {

    //widgets
    private ProgressBar mProgressBar;

    //vars
    private Request mRequest;
    private Chatroom mChatroom;
    private FirebaseFirestore mDb;
    private MapView mapView;
    private GoogleMap gmap;

    private static final String MAP_VIEW_BUNDLE_KEY = "MapViewBundleKey";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request);

        TextView requestName = findViewById(R.id.request_name);
        TextView date = findViewById(R.id.date);


        Intent intent = getIntent();
        mRequest = (Request) intent.getParcelableExtra(getString(R.string.intent_chatroom));

        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setTimestampsInSnapshotsEnabled(true)
                .setPersistenceEnabled(true)
                .build();

        mDb = FirebaseFirestore.getInstance();
        mDb.setFirestoreSettings(settings);

        initSupportActionBar();

        requestName.setText(mRequest.getTitle());

        getDateTime(date);

        mapView = findViewById(R.id.mapView);

        Bundle mapViewBundle = null;
        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle(MAP_VIEW_BUNDLE_KEY);
        }

        mapView.onCreate(mapViewBundle);
        mapView.getMapAsync(this);

        // buildNewChatroom(requestNameStr);
        ImageView qrCode = findViewById(R.id.qrCode);

        try {
            qrCode.setImageBitmap(TextToImageEncode(mRequest.getUserData().getUser().getUsername()));
        } catch (WriterException e) {
            e.printStackTrace();
        }
    }

    public void onClick(View view){
        if(view.getId() == R.id.cancel_request)
            cancelRequest();
        else if(view.getId() == R.id.chat_request)
            navChatroomActivity();
    }

    private void cancelRequest(){
        DocumentReference userRef = mDb.collection(getString(R.string.collection_requests))
                .document(mRequest.getRequestId());

        userRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Log.d("RequestActivity", "DocumentSnapshot successfully deleted!");
                Intent intent = new Intent(RequestActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w("RequestActivity", "Error deleting document", e);
                    }
        });
    }

    private void navChatroomActivity(){
        Intent intent = new Intent(RequestActivity.this, ChatroomActivity.class);
        intent.putExtra(getString(R.string.intent_chatroom), mChatroom);
        startActivity(intent);
    }

    private void buildNewChatroom(String chatroomName){

        final Chatroom chatroom = new Chatroom();
        chatroom.setTitle(chatroomName);

        DocumentReference newChatroomRef = mDb.collection(getString(R.string.collection_requests))
                .document();

        chatroom.setChatroom_id(newChatroomRef.getId());

        newChatroomRef.set(chatroom).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                hideDialog();

                if(!task.isSuccessful()){
                    View parentLayout = findViewById(android.R.id.content);
                    Snackbar.make(parentLayout, "Something went wrong.", Snackbar.LENGTH_SHORT).show();
                }
            }
        });
    }

    private Bitmap TextToImageEncode(String Value) throws WriterException {
        BitMatrix bitMatrix;
        try {
            bitMatrix = new MultiFormatWriter().encode(
                    Value,
                    BarcodeFormat.DATA_MATRIX.QR_CODE,
                    600, 600, null
            );

        } catch (IllegalArgumentException Illegalargumentexception) {

            return null;
        }
        int bitMatrixWidth = bitMatrix.getWidth();

        int bitMatrixHeight = bitMatrix.getHeight();

        int[] pixels = new int[bitMatrixWidth * bitMatrixHeight];

        for (int y = 0; y < bitMatrixHeight; y++) {
            int offset = y * bitMatrixWidth;

            for (int x = 0; x < bitMatrixWidth; x++) {

                pixels[offset + x] = bitMatrix.get(x, y) ?
                        getResources().getColor(R.color.Black):getResources().getColor(R.color.White);
            }
        }
        Bitmap bitmap = Bitmap.createBitmap(bitMatrixWidth, bitMatrixHeight, Bitmap.Config.ARGB_4444);

        bitmap.setPixels(pixels, 0, 600, 0, 0, bitMatrixWidth, bitMatrixHeight);
        return bitmap;
    }

    private void hideDialog(){
        mProgressBar.setVisibility(View.GONE);
    }

    private void initSupportActionBar(){
        setTitle(mRequest.getTitle());
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        Bundle mapViewBundle = outState.getBundle(MAP_VIEW_BUNDLE_KEY);
        if (mapViewBundle == null) {
            mapViewBundle = new Bundle();
            outState.putBundle(MAP_VIEW_BUNDLE_KEY, mapViewBundle);
        }

        mapView.onSaveInstanceState(mapViewBundle);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    protected void onPause() {
        mapView.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        mapView.onDestroy();
        super.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        gmap = googleMap;
        gmap.setMinZoomPreference(12);
        LatLng here = new LatLng(mRequest.getUserData().getGeo_point().getLatitude(),
                mRequest.getUserData().getGeo_point().getLongitude());
        gmap.moveCamera(CameraUpdateFactory.newLatLng(here));
        gmap.addMarker(new MarkerOptions().position(here));
    }

    private void getDateTime(TextView t){
        DocumentReference userRef = mDb.collection(getString(R.string.collection_requests))
                .document(mRequest.getRequestId());

        userRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if(task.isSuccessful()){
                    Log.d("RequestActivity", "onComplete: successfully got date");
                    Request r = task.getResult().toObject(Request.class);
                    t.setText(r.getUserData().getTimestamp().toString());
                }
            }
        });
    }
}