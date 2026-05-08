package com.example.mayarpg;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.mayarpg.network.ApiClient;
import com.example.mayarpg.network.SessionManager;
import com.example.mayarpg.network.services.AuthService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth firebaseAuth;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        firebaseAuth = FirebaseAuth.getInstance();
        sessionManager = new SessionManager(this);

        EditText etUsuario = findViewById(R.id.etUsuario);
        EditText etSenha = findViewById(R.id.etSenha);
        Button btnLogin = findViewById(R.id.btnLogin);
        Button btnVisitante = findViewById(R.id.btnVisitante);
        TextView tvCadastro = findViewById(R.id.tvCadastro);

        // Login real com Firebase Authentication (Email/Senha).
        btnLogin.setOnClickListener(v -> {
            String usuario = etUsuario.getText().toString().trim().toLowerCase();
            String senha = etSenha.getText().toString().trim();

            if (usuario.isEmpty() || senha.isEmpty()) {
                Toast.makeText(this, getString(R.string.erro_campos_obrigatorios), Toast.LENGTH_SHORT).show();
                return;
            }

            firebaseAuth.signInWithEmailAndPassword(usuario, senha)
                    .addOnCompleteListener(task -> {
                        if (!task.isSuccessful()) {
                            Toast.makeText(this, getString(R.string.login_falhou), Toast.LENGTH_SHORT).show();
                            return;
                        }

                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        if (user == null) {
                            Toast.makeText(this, getString(R.string.login_falhou), Toast.LENGTH_SHORT).show();
                            return;
                        }

                        if (!user.isEmailVerified()) {
                            user.sendEmailVerification();
                            firebaseAuth.signOut();
                            Toast.makeText(this, getString(R.string.email_nao_verificado), Toast.LENGTH_LONG).show();
                            return;
                        }

                        // Sincroniza tambem com backend para obter JWT usado nas rotas da API.
                        loginBackendAndOpenHome(usuario, senha, user);
                    });
        });

        tvCadastro.setOnClickListener(v -> {
            Intent intent = new Intent(this, RegisterActivity.class);
            startActivity(intent);
        });

        btnVisitante.setOnClickListener(v -> {
            Intent intent = new Intent(this, GuestActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser user = firebaseAuth.getCurrentUser();
        // Evita entrar na Home sem token JWT local para chamadas no backend.
        if (user != null && user.isEmailVerified() && sessionManager.getToken() != null) {
            openHome(user);
        }
    }

    private void loginBackendAndOpenHome(String email, String senha, FirebaseUser firebaseUser) {
        AuthService.LoginRequest body = new AuthService.LoginRequest(email, senha);
        ApiClient.authService(this).login(body).enqueue(new Callback<AuthService.LoginResponse>() {
            @Override
            public void onResponse(Call<AuthService.LoginResponse> call, Response<AuthService.LoginResponse> response) {
                if (!response.isSuccessful() || response.body() == null || response.body().token == null) {
                    Toast.makeText(MainActivity.this, "Login backend falhou.", Toast.LENGTH_SHORT).show();
                    return;
                }
                sessionManager.saveToken(response.body().token);
                openHome(firebaseUser);
            }

            @Override
            public void onFailure(Call<AuthService.LoginResponse> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Erro ao conectar com API.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openHome(FirebaseUser user) {
        String nome = user.getDisplayName();
        if (nome == null || nome.trim().isEmpty()) {
            nome = user.getEmail() != null ? user.getEmail() : "Paciente";
        }

        Intent intent = new Intent(this, HomeActivity.class);
        intent.putExtra("nome_usuario", nome);
        startActivity(intent);
        finish();
    }
}