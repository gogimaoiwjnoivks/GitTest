package com.example.myapplication;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.gson.Gson;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private Button btnMainLogout;
    private Button btnMainDeleteAccount;
    private FirebaseAuth mAuth;

    private AppDatabase localDb;
    private FirebaseFirestore firestore;
    private FirebaseStorage storage;

    private static final int REQUEST_IMAGE_CAPTURE = 101;
    private static final int PERMISSION_REQUEST_CAMERA = 102;

    private RecyclerView rvMyPlantList;
    private PlantAdapter plantAdapter;
    private List<Plant> myDisplayPlantList;

    private ScrollView mainScrollView;
    private ImageButton btnAllMenu;
    private DrawerLayout drawerLayout;

    private Button btnMenuNotice;
    private Button btnMenuGuide;
    private Button btnMenuInquiry;

    private Button menuMyPlant;
    private Button menuChatbot;
    private Button menuMyInfo;

    private FrameLayout fragmentContainer;
    private Button btnInitialAddPlant;
    private ImageButton btnTopBarCapture;

    private Bitmap imageBitmap;

    public String detectedPlantName = "";
    public boolean isAutoRegistrationMode = false;

    private String basePlantResultText = "";
    private int registrationClickType = 0;
    private PlantSearch plantSearch;

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build();

    private final Gson gson = new Gson();

    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    isAutoRegistrationMode = false;
                    showNameInputDialog(null, imageUri);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(androidx.appcompat.R.style.Theme_AppCompat_Light_NoActionBar);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(true);
        } else {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }

        mAuth = FirebaseAuth.getInstance();
        localDb = AppDatabase.getInstance(this);
        firestore = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        drawerLayout = findViewById(R.id.drawerLayout);
        mainScrollView = findViewById(R.id.mainScrollView);
        rvMyPlantList = findViewById(R.id.rvMyPlantList);
        fragmentContainer = findViewById(R.id.fragmentContainer);

        View topBar = findViewById(R.id.includedTopBar);
        btnAllMenu = topBar.findViewById(R.id.btnAllMenu);
        btnTopBarCapture = topBar.findViewById(R.id.btnTopBarCapture);

        btnMainLogout = findViewById(R.id.btnMainLogout);
        btnMainDeleteAccount = findViewById(R.id.btnMainDeleteAccount);

        btnMenuNotice = findViewById(R.id.btnMenuNotice);
        btnMenuGuide = findViewById(R.id.btnMenuGuide);

        menuMyPlant = findViewById(R.id.menuMyPlant);
        menuChatbot = findViewById(R.id.menuChatbot);
        menuMyInfo = findViewById(R.id.menuMyInfo);

        btnInitialAddPlant = findViewById(R.id.btnInitialAddPlant);

        myDisplayPlantList = new ArrayList<>();
        plantAdapter = new PlantAdapter(myDisplayPlantList);
        rvMyPlantList.setLayoutManager(new GridLayoutManager(this, 2));
        rvMyPlantList.setAdapter(plantAdapter);

        plantAdapter.setOnPlantLongClickListener((plant, position) -> showPlantManagementDialog(plant, position));
        plantAdapter.setOnPlantClickListener((plant, position) -> new PlantCalendarDialog(this, plant).show());

        loadPlantsFromLocalSQLite();
        syncWithFirebaseServer();

        btnMainLogout.setOnClickListener(v -> performAppLogout());
        btnMainDeleteAccount.setOnClickListener(v -> performAppDeleteAccount());

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (fragmentContainer.getVisibility() == View.VISIBLE) {
                    fragmentContainer.setVisibility(View.GONE);
                    mainScrollView.setVisibility(View.VISIBLE);
                    btnInitialAddPlant.setVisibility(View.VISIBLE);
                    btnTopBarCapture.setVisibility(View.VISIBLE);
                } else if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
                    drawerLayout.closeDrawer(GravityCompat.END);
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });

        btnTopBarCapture.setOnClickListener(v -> {
            isAutoRegistrationMode = false;
            showImageSourceSelectionDialog();
        });

        menuMyPlant.setOnClickListener(v -> {
            fragmentContainer.setVisibility(View.GONE);
            mainScrollView.setVisibility(View.VISIBLE);
            btnInitialAddPlant.setVisibility(View.VISIBLE);
            btnTopBarCapture.setVisibility(View.VISIBLE);
            loadPlantsFromLocalSQLite();
            syncWithFirebaseServer();
        });

        menuChatbot.setOnClickListener(v -> {
            mainScrollView.setVisibility(View.GONE);
            fragmentContainer.setVisibility(View.VISIBLE);
            replaceFragment(new ChatbotFragment());
            btnInitialAddPlant.setVisibility(View.GONE);
            btnTopBarCapture.setVisibility(View.GONE);
        });

        menuMyInfo.setOnClickListener(v -> {
            mainScrollView.setVisibility(View.GONE);
            fragmentContainer.setVisibility(View.VISIBLE);
            replaceFragment(new MyInfoFragment());
            btnInitialAddPlant.setVisibility(View.GONE);
            btnTopBarCapture.setVisibility(View.GONE);
        });

        btnInitialAddPlant.setOnClickListener(v -> showPlantRegistrationOptionsDialog());
        btnAllMenu.setOnClickListener(v -> {
            if (!drawerLayout.isDrawerOpen(GravityCompat.END))
                drawerLayout.openDrawer(GravityCompat.END);
        });
    }

    public void performAppLogout() {
        mAuth.signOut();
        Toast.makeText(this, "로그아웃 되었습니다.", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    public void performAppDeleteAccount() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "유저 정보가 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = user.getUid();

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle("⚠️회원 탈퇴 경고⚠️")
                .setMessage("정말로 탈퇴하시겠습니까? 백업된 모든 식물 리스트와 계정이 '영구' 삭제됩니다.")
                .setPositiveButton("탈퇴", (dialog, which) -> {
                    firestore.collection("users").document(uid).collection("plants")
                            .get()
                            .addOnSuccessListener(queryDocumentSnapshots -> {
                                com.google.firebase.firestore.WriteBatch batch = firestore.batch();
                                for (com.google.firebase.firestore.QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                                    batch.delete(doc.getReference());
                                }

                                batch.commit().addOnSuccessListener(aVoid -> {
                                    Log.d("Firebase", "완전 삭제 성공");
                                    firestore.collection("users").document(uid).delete()
                                            .addOnSuccessListener(aVoid2 -> {
                                                user.delete().addOnCompleteListener(task -> {
                                                    if (task.isSuccessful()) {
                                                        Toast.makeText(MainActivity.this, "회원 탈퇴 완료", Toast.LENGTH_LONG).show();
                                                        new Thread(() -> localDb.clearAllTables()).start();

                                                        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                                                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                                        startActivity(intent);
                                                        finish();
                                                    } else {
                                                        Toast.makeText(MainActivity.this, "재로그인이 필요합니다. 다시 로그인 후 탈퇴해 주세요.", Toast.LENGTH_LONG).show();
                                                        mAuth.signOut();
                                                        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                                                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                                        startActivity(intent);
                                                        finish();
                                                    }
                                                });
                                            });
                                });
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(MainActivity.this, "식물 데이터 조회 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("취소", null);

        AlertDialog deleteDialog = builder.create();
        deleteDialog.show();

        Button positiveButton = deleteDialog.getButton(AlertDialog.BUTTON_POSITIVE);
        Button negativeButton = deleteDialog.getButton(AlertDialog.BUTTON_NEGATIVE);

        if (positiveButton != null) {
            positiveButton.setTextColor(Color.parseColor("#D32F2F"));
            positiveButton.setTypeface(null, android.graphics.Typeface.BOLD);
        }
        if (negativeButton != null) {
            negativeButton.setTextColor(Color.parseColor("#616161"));
        }
    }


    private void showFloatingResultDialog(String initialText, @Nullable Bitmap plantBitmap) {
        plantSearch = new PlantSearch(this, plantBitmap, initialText, detectedPlantName, new PlantSearch.OnButtonClickListener() {
            @Override
            public void onWaterClick(String plantName, TextView resultTextView) {
                String customPrompt = "너는 식물 전문가야. " + plantName + " 식물의 계절별 물 주는 주기 한국어로 친절하게 핵심 위주로 짧게 설명해줘.";
                askTextQuestionToGemini(customPrompt);
            }

            @Override
            public void onFeatureClick(String plantName, TextView resultTextView) {
                String customPrompt = "너는 식물 전문가야. " + plantName + " 식물의 주요 특징과 키울 때 주의할 점을 한국어로 친절하게 핵심 위주로 짧게 알려줘.";
                askTextQuestionToGemini(customPrompt);
            }
        });
        plantSearch.show();
    }

    private void showPlantManagementDialog(Plant targetPlant, int position) {
        String[] selectMenu = {"식물 이름 수정하기", "삭제하기"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(targetPlant.getName() + "관리하기");
        builder.setItems(selectMenu, (dialog, which) -> {
            String userUid = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "anonymous";
            if (which == 0) {
                showPlantRenameDialog(targetPlant, userUid);
            } else if (which == 1) {
                executePlantDelete(targetPlant, userUid);
            }
        });
        builder.show();
    }

    private void showPlantRenameDialog(Plant targetPlant, String uid) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("변경할 이름을 입력하세요.");
        final EditText input = new EditText(this);
        input.setText(targetPlant.getName());
        builder.setView(input);

        builder.setPositiveButton("수정 완료", (dialog, which) -> {
            String newName = input.getText().toString().trim();
            if (newName.isEmpty()) return;

            targetPlant.setName(newName);
            new Thread(() -> {
                localDb.plantDao().update(targetPlant);
                runOnUiThread(() -> {
                    loadPlantsFromLocalSQLite();
                    Toast.makeText(this, "이름을 수정했습니다.", Toast.LENGTH_SHORT).show();
                });

                if (targetPlant.getId() != null && !targetPlant.getId().isEmpty()) {
                    firestore.collection("users").document(uid).collection("plants").document(targetPlant.getId())
                            .update("name", newName);
                }
            }).start();
        });
        builder.setNegativeButton("취소", (dialog, which) -> dialog.dismiss());
        AlertDialog registerDialog = builder.create();
        registerDialog.show();

        Button positiveButton = registerDialog.getButton(AlertDialog.BUTTON_POSITIVE);
        Button negativeButton = registerDialog.getButton(AlertDialog.BUTTON_NEGATIVE);

        if (positiveButton != null) {
            positiveButton.setTextColor(Color.parseColor("#2E7D32"));
            positiveButton.setTypeface(null, android.graphics.Typeface.BOLD);
        }
        if (negativeButton != null) {
            negativeButton.setTextColor(Color.parseColor("#616161"));
        }
    }

    private void showPlantRegistrationOptionsDialog() {
        String[] menuItems = {
                "📸 사진 직접 촬영해서 등록",
                "🖼️ 갤러리 사진 불러와서 등록",
                "식물 이름만 등록 ",
                "✨AI 식물인식 자동 등록"
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("새 식물 등록 방식을 선택하세요");
        builder.setItems(menuItems, (dialog, which) -> {
            if (which == 0) {
                isAutoRegistrationMode = false;
                registrationClickType = 1;
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
                    openCamera();
                else
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, PERMISSION_REQUEST_CAMERA);
            } else if (which == 1) {
                isAutoRegistrationMode = false;
                registrationClickType = 2;
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                galleryLauncher.launch(intent);
            } else if (which == 2) {
                isAutoRegistrationMode = false;
                registrationClickType = 0;
                showNameInputDialog(null, null);
            } else if (which == 3) {
                isAutoRegistrationMode = true;
                registrationClickType = 0;
                detectedPlantName = "";
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
                    openCamera();
                else
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, PERMISSION_REQUEST_CAMERA);
            }
        });
        builder.show();
    }

    private void executePlantDelete(Plant targetPlant, String uid) {
        new Thread(() -> {
            if (targetPlant.getImageUrl() != null && targetPlant.getImageUrl().startsWith("http")) {
                try {
                    StorageReference fileRef = storage.getReferenceFromUrl(targetPlant.getImageUrl());
                    fileRef.delete().addOnSuccessListener(aVoid -> Log.d("FirebaseStorage", "원본 이미지 파일 삭제 성공"));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            localDb.plantDao().delete(targetPlant);

            if (targetPlant.getId() != null && !targetPlant.getId().isEmpty()) {
                firestore.collection("users").document(uid).collection("plants").document(targetPlant.getId())
                        .delete()
                        .addOnSuccessListener(aVoid -> runOnUiThread(() -> {
                            Toast.makeText(MainActivity.this, "삭제되었습니다.", Toast.LENGTH_SHORT).show();
                            myDisplayPlantList.remove(targetPlant);
                            plantAdapter.notifyDataSetChanged();
                        }));
            } else {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "삭제되었습니다.", Toast.LENGTH_SHORT).show();
                    myDisplayPlantList.remove(targetPlant);
                    plantAdapter.notifyDataSetChanged();
                });
            }
        }).start();
    }

    private void loadPlantsFromLocalSQLite() {
        new Thread(() -> {
            String currentUserUid = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "anonymous";
            List<Plant> localList = localDb.plantDao().getPlantsByUser(currentUserUid);

            runOnUiThread(() -> {
                myDisplayPlantList.clear();

                for (Plant newPlant : localList) {
                    boolean isAlreadyExists = false;

                    for (Plant existingPlant : myDisplayPlantList) {
                        if (existingPlant.getId().equals(newPlant.getId())) {
                            isAlreadyExists = true;
                            break;
                        }
                    }

                    if (!isAlreadyExists) {
                        myDisplayPlantList.add(newPlant);
                    }
                }

                plantAdapter.notifyDataSetChanged();
            });
        }).start();
    }

    public void showNameInputDialog(@Nullable Bitmap cameraBitmap, @Nullable Uri galleryUri) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("새 식물 등록하기");

        final EditText inputField = new EditText(this);
        inputField.setHint("예: 바질");

        if (isAutoRegistrationMode && !detectedPlantName.isEmpty()) {
            inputField.setText(detectedPlantName);
            inputField.setSelection(detectedPlantName.length());
        }
        builder.setView(inputField);

        builder.setPositiveButton("저장하기", (dialog, which) -> {
            String plantName = inputField.getText().toString().trim();
            if (plantName.isEmpty()) return;

            isAutoRegistrationMode = false;
            String currentUserUid = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "anonymous";

            String documentId = firestore.collection("users").document(currentUserUid).collection("plants").document().getId();
            String initialStatusPath = (cameraBitmap != null || galleryUri != null) ? "uploading" : "";

            Plant localTempPlant = new Plant(documentId, plantName, initialStatusPath);
            localTempPlant.setUserUid(currentUserUid);

            new Thread(() -> {
                localDb.plantDao().insert(localTempPlant);
                runOnUiThread(() -> {
                    loadPlantsFromLocalSQLite();
                    Toast.makeText(this, "등록 완료!", Toast.LENGTH_SHORT).show();
                });

                if (cameraBitmap != null) {
                    String fileName = "plant_images/" + UUID.randomUUID().toString() + ".jpg";
                    StorageReference fileRef = storage.getReference().child(fileName);

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    cameraBitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
                    byte[] dataBytes = baos.toByteArray();

                    fileRef.putBytes(dataBytes)
                            .addOnSuccessListener(taskSnapshot -> fileRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                                uploadToFirestoreAndAndFixLocal(currentUserUid, localTempPlant, plantName, downloadUri.toString(), documentId);
                            }))
                            .addOnFailureListener(e -> Log.e("FirebaseStorage", "카메라 전송 실패"));

                } else if (galleryUri != null) {
                    String fileName = "plant_images/" + UUID.randomUUID().toString() + ".jpg";
                    StorageReference fileRef = storage.getReference().child(fileName);

                    fileRef.putFile(galleryUri)
                            .addOnSuccessListener(taskSnapshot -> fileRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                                uploadToFirestoreAndAndFixLocal(currentUserUid, localTempPlant, plantName, downloadUri.toString(), documentId);
                            }))
                            .addOnFailureListener(e -> Log.e("FirebaseStorage", "갤러리 전송 실패"));

                } else {
                    uploadToFirestoreAndAndFixLocal(currentUserUid, localTempPlant, plantName, "", documentId);
                }
            }).start();
        });

        builder.setNegativeButton("취소", (dialog, which) -> {
            isAutoRegistrationMode = false;
            dialog.dismiss();
        });
        AlertDialog registerDialog = builder.create();
        registerDialog.show();

        Button positiveButton = registerDialog.getButton(AlertDialog.BUTTON_POSITIVE);
        Button negativeButton = registerDialog.getButton(AlertDialog.BUTTON_NEGATIVE);

        if (positiveButton != null) {
            positiveButton.setTextColor(Color.parseColor("#2E7D32"));
            positiveButton.setTypeface(null, android.graphics.Typeface.BOLD);
        }
        if (negativeButton != null) {
            negativeButton.setTextColor(Color.parseColor("#616161"));
        }
    }


    private void uploadToFirestoreAndAndFixLocal(String uid, Plant localPlant, String name, String finalServerImageUrl, String targetDocId) {
        Plant serverPlant = new Plant(targetDocId, name, finalServerImageUrl);
        serverPlant.setUserUid(uid);

        firestore.collection("users").document(uid).collection("plants").document(targetDocId)
                .set(serverPlant)
                .addOnSuccessListener(aVoid -> {
                    new Thread(() -> {
                        localPlant.setImageUrl(finalServerImageUrl);
                        localDb.plantDao().update(localPlant);

                        runOnUiThread(() -> {
                            loadPlantsFromLocalSQLite();

                        });
                    }).start();
                });
    }

    private void syncWithFirebaseServer() {
        String currentUserUid = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
        if (currentUserUid == null) return;

        firestore.collection("users").document(currentUserUid).collection("plants")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        List<Plant> serverList = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Plant p = document.toObject(Plant.class);
                            if (p.getId() == null || p.getId().isEmpty()) {
                                p.setId(document.getId());
                            }
                            p.setUserUid(currentUserUid);
                            serverList.add(p);
                        }

                        new Thread(() -> {
                            if (!serverList.isEmpty()) {
                                for (Plant sp : serverList) {
                                    try {
                                        localDb.plantDao().insert(sp);
                                    } catch(Exception e) {
                                        localDb.plantDao().update(sp);
                                    }
                                }
                            }
                            runOnUiThread(() -> {
                                loadPlantsFromLocalSQLite();
                            });
                        }).start();
                    }
                });
    }

    private void showImageSourceSelectionDialog() {
        String[] options = {"📷 사진 촬영 (카메라)", "🖼️ 갤러리에서 선택"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("식물 분석 수단 선택 🌿");
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                isAutoRegistrationMode = false;
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
                    openCamera();
                else
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, PERMISSION_REQUEST_CAMERA);
            } else if (which == 1) {
                isAutoRegistrationMode = false;
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intent, 999);
            }
        });
        builder.show();
    }

    private void replaceFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction().replace(R.id.fragmentContainer, fragment).commit();
    }

    private void openCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null)
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
    }

    private String encodeBitmapToBase64(Bitmap bitmap) {
        int maxSize = 800;
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        if (width > maxSize || height > maxSize) {
            float bitmapRatio = (float) width / (float) height;
            if (bitmapRatio > 1) {
                width = maxSize;
                height = (int) (width / bitmapRatio);
            } else {
                height = maxSize;
                width = (int) (height * bitmapRatio);
            }
            bitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);
        }
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 60, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.NO_WRAP);
    }

    private void analyzePlantImageWithGemini(String base64Image) {
        String apiKey = "AQ.Ab8RN6KPqavUEN-FxJrwBhgMWgj-WAZk4-9Cp7jOQc2z4bsnQQ";
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-flash-lite:generateContent?key=" + apiKey;

        Map<String, Object> root = new HashMap<>();
        List<Map<String, Object>> contents = new ArrayList<>();
        Map<String, Object> contentMap = new HashMap<>();
        List<Map<String, Object>> parts = new ArrayList<>();

        Map<String, Object> textPart = new HashMap<>();
        textPart.put("text", "너는 식물 전문가야. 이 사진 속 식물의 이름이 뭐야? 문장 말고 '단어'로 식물 도감상 이름만 알려줘.");
        parts.add(textPart);

        Map<String, Object> imagePart = new HashMap<>();
        Map<String, Object> inlineData = new HashMap<>();
        inlineData.put("mimeType", "image/jpeg");
        inlineData.put("data", base64Image);
        imagePart.put("inlineData", inlineData);
        parts.add(imagePart);

        contentMap.put("parts", parts);
        contents.add(contentMap);
        root.put("contents", contents);
        String jsonRequestBody = gson.toJson(root);
        sendOkHttpRequest(url, jsonRequestBody, true);
    }

    private void askTextQuestionToGemini(String promptText) {
        String apiKey = "AQ.Ab8RN6KPqavUEN-FxJrwBhgMWgj-WAZk4-9Cp7jOQc2z4bsnQQ";
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-flash-lite:generateContent?key=" + apiKey;

        Map<String, Object> root = new HashMap<>();
        List<Map<String, Object>> contents = new ArrayList<>();
        Map<String, Object> contentMap = new HashMap<>();
        List<Map<String, Object>> parts = new ArrayList<>();

        Map<String, Object> textPart = new HashMap<>();
        textPart.put("text", promptText);

        parts.add(textPart);
        contentMap.put("parts", parts);
        contents.add(contentMap);
        root.put("contents", contents);
        String jsonRequestBody = gson.toJson(root);
        sendOkHttpRequest(url, jsonRequestBody, false);
    }

    private void sendOkHttpRequest(String url, String jsonBody, boolean isFirstStepImageAnalysis) {
        RequestBody requestBody = RequestBody.create(jsonBody, MediaType.parse("application/json; charset=utf-8"));
        Request request = new Request.Builder().url(url).post(requestBody).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    if (plantSearch != null) {
                        plantSearch.updateResultText("서버 통신에 실패했습니다.");
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String responseString = response.body().string();
                    try {
                        Map<String, Object> responseMap = gson.fromJson(responseString, Map.class);
                        List<Map<String, Object>> candidates = (List<Map<String, Object>>) responseMap.get("candidates");
                        Map<String, Object> firstCandidate = candidates.get(0);
                        Map<String, Object> content = (Map<String, Object>) firstCandidate.get("content");
                        List<Map<String, Object>> resParts = (List<Map<String, Object>>) content.get("parts");

                        final String resultText = (String) resParts.get(0).get("text");

                        runOnUiThread(() -> {
                            if (resultText != null) {
                                if (isAutoRegistrationMode && imageBitmap != null) {
                                    detectedPlantName = resultText.trim();
                                    showNameInputDialog(imageBitmap, null);
                                } else if (isFirstStepImageAnalysis) {
                                    detectedPlantName = resultText.trim();
                                    basePlantResultText = "인식된 식물: " + detectedPlantName + "\n\n알고 싶은 항목을 선택하세요!";
                                    if (plantSearch != null) {
                                        plantSearch.updateResultText(basePlantResultText);
                                    }
                                } else {
                                    if (plantSearch != null) {
                                        plantSearch.updateResultText(basePlantResultText + "\n\n[상세 정보]\n" + resultText.trim());
                                        plantSearch.expandBottomSheetHeight();
                                    }
                                }
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK && data != null) {
            Bundle extras = data.getExtras();
            imageBitmap = (Bitmap) extras.get("data");

            if (isAutoRegistrationMode) {
                analyzePlantImageWithGemini(encodeBitmapToBase64(imageBitmap));
                resetTargetButtons();
                Toast.makeText(this, "AI가 식물을 인식 중입니다. 완료되면 등록창이 뜹니다.", Toast.LENGTH_SHORT).show();
            } else if (registrationClickType == 1) {
                resetTargetButtons();
                showNameInputDialog(imageBitmap, null);
            } else {
                analyzePlantImageWithGemini(encodeBitmapToBase64(imageBitmap));
                resetTargetButtons();
                showFloatingResultDialog("식물을 판독하고 있습니다.", imageBitmap);
            }

            registrationClickType = 0;
        }

        if (requestCode == 999 && resultCode == RESULT_OK && data != null) {
            Uri selectedUri = data.getData();
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ImageDecoder.Source source = ImageDecoder.createSource(this.getContentResolver(), selectedUri);
                    imageBitmap = ImageDecoder.decodeBitmap(source);
                } else {
                    imageBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedUri);
                }

                resetTargetButtons();

                if (isAutoRegistrationMode) {
                    analyzePlantImageWithGemini(encodeBitmapToBase64(imageBitmap));
                    Toast.makeText(this, "AI가 갤러리 사진을 분석 중입니다. 완료되면 등록창이 뜹니다.", Toast.LENGTH_SHORT).show();
                } else if (registrationClickType == 2) {
                    showNameInputDialog(null, selectedUri);
                } else {
                    showFloatingResultDialog("구글 AI가 갤러리 사진을 분석 중입니다.", imageBitmap);
                    String base64Image = encodeBitmapToBase64(imageBitmap);
                    analyzePlantImageWithGemini(base64Image);
                }

                registrationClickType = 0;

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void resetTargetButtons() {
        detectedPlantName = "";
        basePlantResultText = "";
    }
}