package com.tosc.pkbg.ar;

import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.net.Uri;
import android.os.SystemClock;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.ar.core.Anchor;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.Renderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private CustomArFragment fragment;
    private Anchor cloudAnchor;

    private enum AppAnchorState {
        NONE,
        HOSTING,
        HOSTED,
        RESOLVING,
        RESOLVED
    }

    private AppAnchorState appAnchorState = AppAnchorState.NONE;

    private SnackbarHelper snackbarHelper = new SnackbarHelper();

    private StorageManager storageManager;

    private DatabaseReference gameRef = FirebaseDatabase.getInstance().getReference("games");
    private DatabaseReference gameWorldObjectsRef;
    private DatabaseReference gamePlayersRef;
    private DatabaseReference gameHitsRef;

    private Game game;
    private MLKit mlKit;
    private String gameId;

    private TextView tvHealth;
    private TextView tvGameStatus;
    private View btnShoot;
    private int currentHealth = -1;

    private static final int TF_OD_API_INPUT_SIZE = 300;
    private static final String TF_OD_API_MODEL_FILE =
            "file:///android_asset/ssd_mobilenet_v1_android_export.pb";
    private static final String TF_OD_API_LABELS_FILE = "file:///android_asset/coco_labels_list.txt";

    private static final float MINIMUM_CONFIDENCE_TF_OD_API = 0.6f;
    private static final float MINIMUM_CONFIDENCE_MULTIBOX = 0.1f;
    private static final float MINIMUM_CONFIDENCE_YOLO = 0.25f;

    private Classifier detector;

    private Bitmap rgbFrameBitmap = null;
    private Bitmap croppedBitmap = null;
    private Bitmap cropCopyBitmap = null;

    protected int previewWidth = 0;
    protected int previewHeight = 0;
    int cropSize = TF_OD_API_INPUT_SIZE;

    private Matrix frameToCropTransform;
    private Matrix cropToFrameTransform;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initClassifier();
        tvHealth = findViewById(R.id.tv_health);
        tvGameStatus = findViewById(R.id.game_status);

        game = new Game();
        mlKit = new MLKit(this);
        game.gameWorldObject = new ArrayList<>();


        fragment = (CustomArFragment) getSupportFragmentManager().findFragmentById(R.id.sceneform_fragment);
        //To add
        fragment.getPlaneDiscoveryController().hide();
        fragment.getArSceneView().getScene().setOnUpdateListener(this::onUpdateFrame);

        Button clearButton = findViewById(R.id.clear_button);
        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setCloudAnchor(null);
            }
        });


        Button resolveButton = findViewById(R.id.resolve_button);
        resolveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (cloudAnchor != null){
                    snackbarHelper.showMessageWithDismiss(getParent(), "Please clear Anchor");
                    return;
                }
                ResolveDialogFragment dialog = new ResolveDialogFragment();
                dialog.setOkListener(MainActivity.this::onResolveOkPressed);
                dialog.show(getSupportFragmentManager(), "Resolve");

            }
        });

        FrameLayout mainLayout = findViewById(R.id.layout_main);

        btnShoot = findViewById(R.id.shoot_button);
        btnShoot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fragment.captureBitmap(bitmap -> {
                    mlKit.detectFace(bitmap);
                    detectImage(bitmap);
                }, false);
            }
        });

        Button faceButton = findViewById(R.id.face_button);
        faceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fragment.captureBitmap(bitmap -> {
//                    mlKit.detectFace(bitmap);
                    detectImage(bitmap);
                }, false);
            }
        });

        fragment.setOnTapArPlaneListener(
                (HitResult hitResult, Plane plane, MotionEvent motionEvent) -> {

                    Anchor localAnchor = hitResult.createAnchor();

                    if (appAnchorState == AppAnchorState.NONE) {
                        Anchor newAnchor = fragment.getArSceneView().getSession().hostCloudAnchor(localAnchor);
                        setCloudAnchor(newAnchor);

                        appAnchorState = AppAnchorState.HOSTING;
                        snackbarHelper.showMessage(this, "Now hosting anchor...");

                        placeObject(fragment, cloudAnchor, Uri.parse("USMC_flag.sfb"), false);

                        return;
                    }

                    placeObject(fragment, localAnchor, Uri.parse("Crate1.sfb"), true);

                }
        );

        storageManager = new StorageManager(this);
    }

    private void initClassifier() {

        try {
            detector = TensorFlowObjectDetectionAPIModel.create(
                    getAssets(), TF_OD_API_MODEL_FILE, TF_OD_API_LABELS_FILE, TF_OD_API_INPUT_SIZE);
        } catch (final IOException e) {
            Log.e("lol", "Exception initializing classifier!");
            Toast toast =
                    Toast.makeText(
                            getApplicationContext(), "Classifier could not be initialized", Toast.LENGTH_SHORT);
            toast.show();
            finish();
        }
    }

    private void detectImage(Bitmap bitmap) {

        previewWidth = bitmap.getWidth();
        previewHeight = bitmap.getHeight();

        frameToCropTransform =
                Utils.getTransformationMatrix(
                        previewWidth, previewHeight,
                        cropSize, cropSize,
                        0, false);

        cropToFrameTransform = new Matrix();
        frameToCropTransform.invert(cropToFrameTransform);

        rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Bitmap.Config.ARGB_8888);
        croppedBitmap = Bitmap.createBitmap(cropSize, cropSize, Bitmap.Config.ARGB_8888);

        final Canvas canvass = new Canvas(croppedBitmap);
        canvass.drawBitmap(rgbFrameBitmap, frameToCropTransform, null);


        final List<Classifier.Recognition> results = detector.recognizeImage(bitmap);

        float minimumConfidence = MINIMUM_CONFIDENCE_TF_OD_API;

        boolean isHit = false;
        for (final Classifier.Recognition result : results) {
            final RectF location = result.getLocation();
            if (location != null && result.getConfidence() >= minimumConfidence) {
                Log.e("lol", "found " + result.getTitle());
                Log.e("lol", "x "+ location.left);
                Log.e("lol", "y "+ location.top);
                if (result.getTitle().equals("person")) {
                    if (isRectInCrosshair(location)) {
                        isHit = true;
                        onHitAttempted(true);
                        break;
                    }
                }
            }
        }

        if (!isHit) onHitAttempted(false);
    }

    private void onResolveOkPressed(String dialogValue){
        int shortCode = Integer.parseInt(dialogValue);
        setupNewGame(shortCode);
        storageManager.getCloudAnchorID(shortCode,(cloudAnchorId) -> {
            Anchor resolvedAnchor = fragment.getArSceneView().getSession().resolveCloudAnchor(cloudAnchorId);
            setCloudAnchor(resolvedAnchor);
            placeObject(fragment, cloudAnchor, Uri.parse("USMC_flag.sfb"), false);
            snackbarHelper.showMessage(this, "Now Resolving Anchor...");
            appAnchorState = AppAnchorState.RESOLVING;
            addChildSyncing();
        });
    }

    private boolean isRectInCrosshair(RectF location) {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int height = displayMetrics.heightPixels;
        int width = displayMetrics.widthPixels;

        height = 300;
        width = 300;
        return location.contains(width /2, height / 2);
    }

    private void setCloudAnchor (Anchor newAnchor){
        if (cloudAnchor != null){
            cloudAnchor.detach();
        }

        cloudAnchor = newAnchor;
        appAnchorState = AppAnchorState.NONE;
        snackbarHelper.hide(this);
    }

    private void onUpdateFrame(FrameTime frameTime){
        checkUpdatedAnchor();
    }

    private synchronized void checkUpdatedAnchor(){
        if (appAnchorState != AppAnchorState.HOSTING && appAnchorState != AppAnchorState.RESOLVING){
            return;
        }
        Anchor.CloudAnchorState cloudState = cloudAnchor.getCloudAnchorState();
        if (appAnchorState == AppAnchorState.HOSTING) {
            if (cloudState.isError()) {
                snackbarHelper.showMessageWithDismiss(this, "Error hosting anchor.. "
                        + cloudState);
                appAnchorState = AppAnchorState.NONE;
            } else if (cloudState == Anchor.CloudAnchorState.SUCCESS) {
                storageManager.nextShortCode((shortCode) -> {
                    if (shortCode == null){
                        snackbarHelper.showMessageWithDismiss(this, "Could not get shortCode");
                        return;
                    }
                    storageManager.storeUsingShortCode(shortCode, cloudAnchor.getCloudAnchorId());
                    setupNewGame(shortCode);

                    snackbarHelper.showMessageWithDismiss(this, "Anchor hosted! Cloud Short Code: " +
                            shortCode);

                    addChildSyncing();
                });

                appAnchorState = AppAnchorState.HOSTED;
            }
        }

        else if (appAnchorState == AppAnchorState.RESOLVING){
            if (cloudState.isError()) {
                snackbarHelper.showMessageWithDismiss(this, "Error resolving anchor.. "
                        + cloudState);
                appAnchorState = AppAnchorState.NONE;
            } else if (cloudState == Anchor.CloudAnchorState.SUCCESS){
                snackbarHelper.showMessageWithDismiss(this, "Anchor resolved successfully");
                appAnchorState = AppAnchorState.RESOLVED;
            }
        }

    }


    private void placeObject(ArFragment fragment, Anchor anchor, Uri model, boolean shouldSync) {

        ModelRenderable.builder()
                .setSource(fragment.getContext(), model)
                .build()
                .thenAccept(renderable -> addNodeToScene(fragment, anchor, renderable, shouldSync))
                .exceptionally((throwable -> {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setMessage(throwable.getMessage())
                            .setTitle("Error!");
                    AlertDialog dialog = builder.create();
                    dialog.show();
                    return null;
                }));
    }

    private void addNodeToScene(ArFragment fragment, Anchor anchor, Renderable renderable, boolean shouldSync) {
        AnchorNode anchorNode = new AnchorNode(anchor);
        TransformableNode node = new TransformableNode(fragment.getTransformationSystem());
        node.setRenderable(renderable);
        node.setParent(anchorNode);
        fragment.getArSceneView().getScene().addChild(anchorNode);
        node.select();
        if (shouldSync)
            syncNewObject(anchorNode);
    }

    private void setupNewGame(int shortCode) {
        gameId = String.valueOf(shortCode);
        game.id = gameId;
        gameWorldObjectsRef = gameRef.child(gameId).child("objects");
        gamePlayersRef = gameRef.child(gameId).child("players");
        gameHitsRef = gameRef.child(gameId).child("hits");

        GamePlayer player = new GamePlayer();
        player.playerId = getDeviceId();
        player.health = 100;

        gameRef.child(gameId).child("winnerId").setValue("");

        gamePlayersRef.child(getDeviceId()).setValue(player);

        gamePlayersRef.child(getDeviceId()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                updateGameState(dataSnapshot.getValue(GamePlayer.class));
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        gameRef.child(gameId).child("winnerId").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String winnerId = dataSnapshot.getValue(String.class);
                if (winnerId == null || winnerId.equals("") || winnerId.equals(" ")) return;
                btnShoot.setVisibility(View.GONE);
                tvGameStatus.setVisibility(View.VISIBLE);
                findViewById(R.id.iv_crosshair).setVisibility(View.GONE);
                if (winnerId.equals(getDeviceId())) {
                    tvGameStatus.setText("WINNER WINNER CHICKEN DINNER");
                } else  {
                    tvGameStatus.setText("LOST!");
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void syncNewObject(AnchorNode anchorNode) {
        Vector3 position = anchorNode.getWorldPosition();
        Quaternion rotation = anchorNode.getWorldRotation();
        DatabaseReference newObjectRef = gameWorldObjectsRef.push();
        GameWorldObject worldObject = new GameWorldObject(position, rotation, newObjectRef.getKey(),getDeviceId());
        game.gameWorldObject.add(worldObject);
        newObjectRef.setValue(worldObject);
    }

    private void addChildSyncing() {
        gameWorldObjectsRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                try {
                    GameWorldObject worldObject = dataSnapshot.getValue(GameWorldObject.class);
                    if (worldObject.addedByDeviceId.equals(getDeviceId())) {
                        return;
                    }
                    Session session = fragment.getArSceneView().getSession();
                    Anchor anchor = session.createAnchor(new Pose(getArray(worldObject.position), getArray(worldObject.rotation)));
                    placeObject(fragment, anchor, Uri.parse("Crate1.sfb"), false);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
        gameWorldObjectsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    try {
                        GameWorldObject worldObject = snapshot.getValue(GameWorldObject.class);
                        Session session = fragment.getArSceneView().getSession();
                        Anchor anchor = session.createAnchor(new Pose(getArray(worldObject.position), getArray(worldObject.rotation)));
                        placeObject(fragment, anchor, Uri.parse("Crate1.sfb"), false);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private float[] getArray(ArrayList<Float> list) {
        float[] floatArray = new float[list.size()];
        int i = 0;

        for (Float f : list) {
            floatArray[i++] = (f != null ? f : Float.NaN);
        }

        return floatArray;
    }

    private String getDeviceId() {
        return Settings.Secure.getString(this.getContentResolver(),
                Settings.Secure.ANDROID_ID).substring(0, 5);
    }

    private void onHitAttempted(boolean isHit) {
        Utils.playFireSound(this);
        if (isHit) {
            GameHit hit = new GameHit(getDeviceId(), 1);
            gameHitsRef.push().setValue(hit);
        }
    }

    private void updateGameState(GamePlayer player) {
        if (currentHealth != -1) {
            if (player.health < currentHealth) {
                Utils.playHitSound(this);
                Utils.vibrate(this);
            }
        }
        currentHealth = player.health;
        tvHealth.setText(String.valueOf(player.health));
    }
}
