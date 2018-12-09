/*
 * Copyright 2018 Google LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.co.appoly.sceneform_example;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.ar.core.Frame;
import com.google.ar.core.Plane;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.UnavailableException;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.Scene;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.ViewRenderable;

import java.util.GregorianCalendar;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import uk.co.appoly.arcorelocation.LocationMarker;
import uk.co.appoly.arcorelocation.LocationScene;
import uk.co.appoly.arcorelocation.rendering.LocationNode;
import uk.co.appoly.arcorelocation.rendering.LocationNodeRender;
import uk.co.appoly.arcorelocation.utils.ARLocationPermissionHelper;

import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;

/**
 * This is a simple example that shows how to create an augmented reality (AR) application using the
 * ARCore and Sceneform APIs.
 */
public class LocationActivity extends AppCompatActivity implements LocationListener, Scene.OnUpdateListener {
    private boolean installRequested;
    private boolean hasFinishedLoading = false;

    private Snackbar loadingMessageSnackbar = null;

    private ArSceneView arSceneView;

    // Renderables for this example
    private ModelRenderable andyRenderable;
    private ViewRenderable exampleLayoutRenderable;
    private ViewRenderable testViewRenderable;

    // Our ARCore-Location scene
    private LocationScene locationScene;
    private Location myLocation;
    public Location andyLocat = new Location("");

    TextView textView1;
    TextView Precision;
    Button button_supp;
    Button button_obj;

    Boolean boolGen = true;

    public Location setLatAndyLocat(double lat2) {
        andyLocat.setLatitude(lat2);
        return andyLocat;
    }
    public Location setLonAndyLocat(double lon2) {
        andyLocat.setLongitude(lon2);
        return andyLocat;
    }


    @Override
    @SuppressWarnings({"AndroidApiChecker", "FutureReturnValueIgnored"})
    // CompletableFuture requires api level 24
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sceneform);
        arSceneView = findViewById(R.id.ar_scene_view);

        //arSceneView.getScene().setOnUpdateListener(this);

        textView1 = (TextView) findViewById(R.id.textView1);
        Precision = (TextView) findViewById(R.id.Precision);
        button_supp = (Button) findViewById(R.id.button_supp);
        button_obj = (Button) findViewById(R.id.button_obj);

        andyLocat = setLonAndyLocat(49.02600400000001);
        andyLocat = setLatAndyLocat(2.364103999999975);

        button_supp.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                locationScene.mLocationMarkers.clear();
                //vanish = false
                boolGen = false;
            }
        });

        button_obj.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                ViewRenderable.builder()
                        .setView(LocationActivity.this, R.layout.test_view)
                        .build()
                        .thenAccept(renderable -> testViewRenderable = renderable);
                locationScene.mLocationMarkers.add(
                        new LocationMarker(
                                //49.02600400000001,
                                //2.3641039999999975,
                                andyLocat.getLongitude(),
                                andyLocat.getLatitude(),
                                getObject()));
            }
        });


        LocationManager lm;
        lm = (LocationManager) this.getSystemService(getApplicationContext().LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000, 5, this);
        Criteria criteria = new Criteria();
        String provider = lm.getBestProvider(criteria, true);
        myLocation = lm.getLastKnownLocation(provider);

        // Build a renderable from a 2D View.
        CompletableFuture<ViewRenderable> exampleLayout =
                ViewRenderable.builder()
                        .setView(this, R.layout.example_layout)
                        .build();

        // When you build a Renderable, Sceneform loads its resources in the background while returning
        // a CompletableFuture. Call thenAccept(), handle(), or check isDone() before calling get().
        CompletableFuture<ModelRenderable> andy = ModelRenderable.builder()
                .setSource(this, R.raw.andy)
                .build();


        CompletableFuture.allOf(
                exampleLayout,
                andy)
                .handle(
                        (notUsed, throwable) -> {
                            // When you build a Renderable, Sceneform loads its resources in the background while
                            // returning a CompletableFuture. Call handle(), thenAccept(), or check isDone()
                            // before calling get().

                            if (throwable != null) {
                                DemoUtils.displayError(this, "Unable to load renderables", throwable);
                                return null;
                            }

                            try {
                                exampleLayoutRenderable = exampleLayout.get();
                                andyRenderable = andy.get();
                                hasFinishedLoading = true;

                            } catch (InterruptedException | ExecutionException ex) {
                                DemoUtils.displayError(this, "Unable to load renderables", ex);
                            }

                            return null;
                        });

        // Set an update listener on the Scene that will hide the loading message once a Plane is
        // detected.
        //arSceneView.getScene().setOnUpdateListener(this);

        // Lastly request CAMERA & fine location permission which is required by ARCore-Location.
        ARLocationPermissionHelper.requestPermission(this);
    }

    /**
     * Example node of a layout
     *
     * @return
     */
    private Node getExampleView() {
        Node base = new Node();
        base.setRenderable(exampleLayoutRenderable);
        Context c = this;
        // Add  listeners etc here
        View eView = exampleLayoutRenderable.getView();
        eView.setOnTouchListener((v, event) -> {
            Toast.makeText(
                    c, "Location marker touched.", Toast.LENGTH_LONG)
                    .show();
            return false;
        });

        return base;
    }

    private Node getObject() {
        Node node = new Node();
        node.setParent(arSceneView.getScene());
        node.setRenderable(testViewRenderable);
        return node;
    }

    /***
     * Example Node of a 3D model
     *
     * @return
     */
    private Node getAndy() {
        Node base = new Node();
        base.setRenderable(andyRenderable);
        Context c = this;
        base.setOnTapListener((v, event) -> {
            Toast.makeText(
                    c, "Andy touched.", Toast.LENGTH_LONG)
                    .show();
            AlertDialog.Builder builder = new AlertDialog.Builder(LocationActivity.this, R.style.Theme_AppCompat);
            builder.setTitle("Informations");
            builder.setMessage("Voulez-vous afficher les informations relatives à l'objet?");
            builder.setPositiveButton("Afficher", new DialogInterface.OnClickListener() { //android.R.string.yes -> "yes"
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Toast.makeText(getApplicationContext(),"Positif", Toast.LENGTH_LONG).show();
                    startActivity(new Intent(LocationActivity.this,pageInfo.class));
                }
            });
            builder.setNegativeButton("Retour", new DialogInterface.OnClickListener() { //android.R.string.no -> "no"
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Toast.makeText(getApplicationContext(),"Negatif", Toast.LENGTH_LONG).show();
                }
            });
            builder.show();


        });
        return base;
    }

    /**
     * Make sure we call locationScene.resume();
     */
    @Override
    protected void onResume() {
        super.onResume();

        if (locationScene != null) {
            locationScene.resume();
        }

        if (arSceneView.getSession() == null) {
            // If the session wasn't created yet, don't resume rendering.
            // This can happen if ARCore needs to be updated or permissions are not granted yet.
            try {
                Session session = DemoUtils.createArSession(this, installRequested);
                if (session == null) {
                    installRequested = ARLocationPermissionHelper.hasPermission(this);
                    return;
                } else {
                    arSceneView.setupSession(session);
                }
            } catch (UnavailableException e) {
                DemoUtils.handleSessionException(this, e);
            }
        }

        try {
            arSceneView.resume();
        } catch (CameraNotAvailableException ex) {
            DemoUtils.displayError(this, "Unable to get camera", ex);
            finish();
            return;
        }

        if (arSceneView.getSession() != null) {
            showLoadingMessage();
        }
    }

    /**
     * Make sure we call locationScene.pause();
     */
    @Override
    public void onPause() {
        super.onPause();

        if (locationScene != null) {
            locationScene.pause();
        }

        arSceneView.pause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        arSceneView.destroy();
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] results) {
        if (!ARLocationPermissionHelper.hasPermission(this)) {
            if (!ARLocationPermissionHelper.shouldShowRequestPermissionRationale(this)) {
                // Permission denied with checking "Do not ask again".
                ARLocationPermissionHelper.launchPermissionSettings(this);
            } else {
                Toast.makeText(
                        this, "Camera permission is needed to run this application", Toast.LENGTH_LONG)
                        .show();
            }
            finish();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            // Standard Android full-screen functionality.
            getWindow()
                    .getDecorView()
                    .setSystemUiVisibility(
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    private void showLoadingMessage() {
        if (loadingMessageSnackbar != null && loadingMessageSnackbar.isShownOrQueued()) {
            return;
        }

        loadingMessageSnackbar =
                Snackbar.make(
                        LocationActivity.this.findViewById(android.R.id.content),
                        R.string.plane_finding,
                        Snackbar.LENGTH_INDEFINITE);
        loadingMessageSnackbar.getView().setBackgroundColor(0xbf323232);
        loadingMessageSnackbar.show();
    }

    private void hideLoadingMessage() {
        if (loadingMessageSnackbar == null) {
            return;
        }

        loadingMessageSnackbar.dismiss();
        loadingMessageSnackbar = null;
    }

    @Override
    public void onLocationChanged(Location location) {
        myLocation = location;
        double lat = myLocation.getLatitude();
        double lon = myLocation.getLongitude();
        double acc = myLocation.getAccuracy();
        Precision.setText("Pre: " + acc);
        if (acc >= 10.0) {
            Toast.makeText(
                    getApplicationContext(), "Fail.", Toast.LENGTH_SHORT)
                    .show();
            //locationScene.mLocationMarkers.clear();
        }
        else {
            arSceneView.getScene().setOnUpdateListener(this);
        }
        java.util.GregorianCalendar calendar = new GregorianCalendar();
        java.util.Date time = calendar.getTime();
        textView1.setText("LAT: " + lat + " LON: " + lon + " HEURE= " + time);
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }

    @Override
    public void onUpdate(FrameTime frameTime) {
            if (!hasFinishedLoading) {
                return;
            }

            if (locationScene == null) {
                // If our locationScene object hasn't been setup yet, this is a good time to do it
                // We know that here, the AR components have been initiated.
                locationScene = new LocationScene(this, this, arSceneView);

                if(myLocation != null) {
                    // Now lets create our location markers.
                    // First, a layout
                    LocationMarker layoutLocationMarker = new LocationMarker(
                            //myLocation.getLongitude(),
                            //myLocation.getLatitude(),
                            49.025925,
                            2.363721,
                            getExampleView()
                    );

                    // An example "onRender" event, called every frame
                    // Updates the layout with the markers distance
                    layoutLocationMarker.setRenderEvent(new LocationNodeRender() {
                        @Override
                        public void render(LocationNode node) {
                            View eView = exampleLayoutRenderable.getView();
                            TextView distanceTextView = eView.findViewById(R.id.textView2);
                            distanceTextView.setText(node.getDistance() + "M");
                        }
                    });
                    // Adding the marker
                    locationScene.mLocationMarkers.add(layoutLocationMarker);
                }


                    // Adding a simple location marker of a 3D model
                    locationScene.mLocationMarkers.add(
                            new LocationMarker(
                                    //49.02600400000001,
                                    //2.3641039999999975,
                                    andyLocat.getLongitude(),
                                    andyLocat.getLatitude(),
                                    getAndy()));
            }


            Frame frame = arSceneView.getArFrame();
            if (frame == null) {
                return;
            }

            if (frame.getCamera().getTrackingState() != TrackingState.TRACKING) {
                return;
            }

            if (locationScene != null) {
                locationScene.processFrame(frame);
            }

            if (loadingMessageSnackbar != null) {
                for (Plane plane : frame.getUpdatedTrackables(Plane.class)) {
                    if (plane.getTrackingState() == TrackingState.TRACKING) {
                        hideLoadingMessage();
                    }
                }
            }
    }
}
