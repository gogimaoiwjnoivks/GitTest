package com.example.myapplication;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;

/**
 * 로그인된 유저의 프로필 정보를 렌더링하고 비밀번호 변경 및 탈퇴 트랜잭션을 중계하는 프래그먼트 클래스입니다.
 * OAuth2.0(Google) 유저와 익명/이메일 가입 유저의 프로바이더 상태를 동적으로 분기하여 가시성을 제어합니다.
 */
public class MyInfoFragment extends Fragment {

    private TextView tvUserEmail;
    private Button btnChangePassword;
    private TextView tvPasswordHint;
    private Button btnInfoDeleteAccount;
    private FirebaseAuth mAuth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_my_info, container, false);

        mAuth = FirebaseAuth.getInstance();
        tvUserEmail = view.findViewById(R.id.tvUserEmail);
        btnChangePassword = view.findViewById(R.id.btnChangePassword);
        tvPasswordHint = view.findViewById(R.id.tvPasswordHint);
        btnInfoDeleteAccount = view.findViewById(R.id.btnInfoDeleteAccount);

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null && user.getEmail() != null) {
            tvUserEmail.setText(user.getEmail());

            boolean isGoogleUser = false;

            // 멀티 인증 공급자(Provider) 데이터 리스트를 순회하여 Google OAuth 가입 여부를 검증합니다.
            for (UserInfo profile : user.getProviderData()) {
                String providerId = profile.getProviderId();
                if (providerId.equals("google.com")) {
                    isGoogleUser = true;
                    break;
                }
            }

            // 구글 소셜 로그인 유저는 백엔드 보안 정책상 직접적인 패스워드 재설정을 제한하므로 관련 UI 컴포넌트를 비활성화합니다.
            if (isGoogleUser) {
                btnChangePassword.setVisibility(View.GONE);
                if (tvPasswordHint != null) {
                    tvPasswordHint.setVisibility(View.GONE);
                }
            } else {
                btnChangePassword.setVisibility(View.VISIBLE);
                if (tvPasswordHint != null) {
                    tvPasswordHint.setVisibility(View.VISIBLE);
                }
            }

        } else {
            tvUserEmail.setText("로그인 정보 없음");
            btnChangePassword.setVisibility(View.GONE);
        }

        /**
         * 이메일 인증 가입 유저를 위한 비밀번호 재설정 링크 발송 이벤트 핸들러입니다.
         * Firebase Auth의 원격 메일 발송 템플릿 파이프라인을 구동합니다.
         */
        btnChangePassword.setOnClickListener(v -> {
            if (user != null && user.getEmail() != null) {
                String emailAddress = user.getEmail();

                mAuth.sendPasswordResetEmail(emailAddress)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Toast.makeText(getContext(), emailAddress + " 계정으로 비밀번호 변경 메일을 보냈습니다. ✉️", Toast.LENGTH_LONG).show();
                            } else {
                                String error = task.getException() != null ? task.getException().getMessage() : "발송 실패";
                                Toast.makeText(getContext(), "메일 발송 실패: " + error, Toast.LENGTH_SHORT).show();
                            }
                        });
            } else {
                Toast.makeText(getContext(), "로그인 상태를 확인해 주세요.", Toast.LENGTH_SHORT).show();
            }
        });

        /**
         * 상위 호스트 액티비티(MainActivity)의 영속성 컨텍스트를 동적으로 낚아채어
         * 중앙 통제형 회원탈퇴 프로세스를 강제 연동시키는 이벤트 핸들러 구역입니다.
         */
        if (btnInfoDeleteAccount != null) {
            btnInfoDeleteAccount.setOnClickListener(v -> {
                Context currentContext = getContext();

                if (currentContext instanceof MainActivity) {
                    MainActivity mainActivity = (MainActivity) currentContext;

                    // 백그라운드 작업 분기 엇박자를 원천 방어하기 위해 메인 UI 스레드 Looper 큐에 직접 위임 인젝션 처리
                    mainActivity.runOnUiThread(() -> {
                        mainActivity.performAppDeleteAccount();
                    });
                }
            });
        }

        return view;
    }
}