package com.example.myapplication;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;

/**
 * 신규 유저의 이메일 크레덴셜 식별 정보를 파이어베이스 클라우드 인증 서버로 안전하게 이송하여
 * 유니크 세션 식별 아이디(UID)를 부여받는 회원가입 액티비티 컴포넌트입니다.
 */
public class RegisterActivity extends AppCompatActivity {

    private EditText etRegEmail;
    private EditText etRegPassword;
    private EditText etRegPasswordConfirm;
    private Button btnRegisterSubmit;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(androidx.appcompat.R.style.Theme_AppCompat_Light_NoActionBar);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();

        etRegEmail = findViewById(R.id.etRegEmail);
        etRegPassword = findViewById(R.id.etRegPassword);
        etRegPasswordConfirm = findViewById(R.id.etRegPasswordConfirm);
        btnRegisterSubmit = findViewById(R.id.btnRegisterSubmit);

        /**
         * 입력 폼 유효성(입력 누락 체크, 패스워드 일치성, 최소 자릿수 규칙 보장)을 다각도 샌드박스 검증한 뒤,
         * 원격 파이어베이스 서버의 회원 생성을 유발하는 데이터 전송 이벤트 리스너 구역입니다.
         */
        btnRegisterSubmit.setOnClickListener(v -> {
            String email = etRegEmail.getText().toString().trim();
            String password = etRegPassword.getText().toString().trim();
            String passwordConfirm = etRegPasswordConfirm.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty() || passwordConfirm.isEmpty()) {
                Toast.makeText(this, "모든 항목을 입력해주세요.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (password.length() < 6) {
                Toast.makeText(this, "비밀번호는 최소 6자리 이상이어야 합니다.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!password.equals(passwordConfirm)) {
                Toast.makeText(this, "비밀번호가 일치하지 않습니다.", Toast.LENGTH_SHORT).show();
                return;
            }

            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(RegisterActivity.this, "회원가입이 완료되었습니다!", Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            String errorMessage = task.getException() != null ? task.getException().getMessage() : "알 수 없는 오류";
                            Toast.makeText(RegisterActivity.this, "가입 실패: " + errorMessage, Toast.LENGTH_LONG).show();
                        }
                    });
        });
    }
}



