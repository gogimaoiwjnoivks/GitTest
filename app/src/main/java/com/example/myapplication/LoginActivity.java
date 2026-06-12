package com.example.myapplication;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;

/**
 * 이메일/비밀번호 기반 인증 및 Google OAuth2.0 연동 로그인을 총괄하는 액티비티 클래스입니다.
 * 세션 정보 확인을 통한 자동 로그인 및 클라이언트 보안 토큰 처리를 수행합니다.
 */
public class LoginActivity extends AppCompatActivity {

    private EditText etLoginEmail;
    private EditText etLoginPassword;
    private Button btnLogin;
    private TextView tvGoToRegister;
    private Button btnGoogleLogin;

    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;

    /**
     * Google Sign-In 인텐트 요청에 대한 결과를 수신하는 컴포넌트입니다.
     * ActivityResultLauncher 메커니즘을 적용하여 외부 인증 트랜잭션의 상태 결과를 안전하게 처리합니다.
     */
    private final ActivityResultLauncher<Intent> googleSignInLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                    try {
                        GoogleSignInAccount account = task.getResult(ApiException.class);
                        if (account != null) {
                            firebaseAuthWithGoogle(account.getIdToken());
                        }
                    } catch (ApiException e) {
                        Toast.makeText(this, "구글 로그인 실패. 잠시후 재시도해주세요.", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(androidx.appcompat.R.style.Theme_AppCompat_Light_NoActionBar);
        setContentView(R.layout.activity_login);

        // 안드로이드 11(SDK 30) 이상 운영체제에 대응하는 전체 화면 소프트웨어 UI 구현 및 시스템 인셋 통제
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
            android.view.WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.hide(android.view.WindowInsets.Type.navigationBars() | android.view.WindowInsets.Type.statusBars());
            }
        }

        mAuth = FirebaseAuth.getInstance();

        // 현재 클라이언트 기기에 저장된 유효 세션 존재 여부를 검증하여 자동 로그인 전환 처리
        if (mAuth.getCurrentUser() != null) {
            Toast.makeText(this, "자동 로그인되었습니다.", Toast.LENGTH_SHORT).show();
            navigateToMainActivity();
            return;
        }

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("616296559955-os6u55su4dqg3cbgqh2tjoaocte64e24.apps.googleusercontent.com")
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // 무효화된 캐시 세션으로 인해 구글 계정 선택창 팝업이 누락되는 현상을 방지하기 위한 선제적 로그아웃 처리
        mGoogleSignInClient.signOut();

        etLoginEmail = findViewById(R.id.etLoginEmail);
        etLoginPassword = findViewById(R.id.etLoginPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvGoToRegister = findViewById(R.id.tvGoToRegister);
        btnGoogleLogin = findViewById(R.id.btnGoogleLogin);

        tvGoToRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        /**
         * 이메일/비밀번호 데이터 폼 유효성 검증 및 Firebase Authentication 연동 로그인 핸들러입니다.
         */
        btnLogin.setOnClickListener(v -> {
            String email = etLoginEmail.getText().toString().trim();
            String password = etLoginPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "이메일과 비밀번호를 입력해 주세요.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (password.length() < 6) {
                Toast.makeText(this, "비밀번호는 최소 6자리 이상입니다.", Toast.LENGTH_SHORT).show();
                return;
            }

            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(LoginActivity.this, "로그인 성공!", Toast.LENGTH_SHORT).show();
                            navigateToMainActivity();
                        } else {
                            String errorMsg = task.getException() != null ? task.getException().getMessage() : "인증 실패";
                            Toast.makeText(LoginActivity.this, "로그인 실패: " + errorMsg, Toast.LENGTH_LONG).show();
                        }
                    });
        });

        btnGoogleLogin.setOnClickListener(v -> {
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            googleSignInLauncher.launch(signInIntent);
        });
    }

    /**
     * Google OAuth2.0 서버에서 발급된 ID 토큰을 연동하여 Firebase 백엔드 세션을 갱신하는 인증 트랜잭션 메서드입니다.
     * @param idToken 구글 클라이언트에서 수신된 암호화 ID 토큰
     */
    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(LoginActivity.this, "구글 계정 연동 성공!", Toast.LENGTH_SHORT).show();
                        navigateToMainActivity();
                    } else {
                        Toast.makeText(LoginActivity.this, "연동 실패", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * 인증 성공 후 메인 대시보드 화면으로 전환하며, 스택 백 버퍼 메모리를 클리어하는 내비게이션 메서드입니다.
     */
    private void navigateToMainActivity() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}