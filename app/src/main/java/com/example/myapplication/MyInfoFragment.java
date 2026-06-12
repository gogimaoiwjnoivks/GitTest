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

            for (UserInfo profile : user.getProviderData()) {
                String providerId = profile.getProviderId();
                if (providerId.equals("google.com")) {
                    isGoogleUser = true;
                    break;
                }
            }

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