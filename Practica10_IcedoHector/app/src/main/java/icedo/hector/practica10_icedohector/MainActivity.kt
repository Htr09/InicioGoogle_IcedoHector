package icedo.hector.practica10_icedohector


import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.lifecycleScope
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch


class MainActivity : AppCompatActivity() {

    private lateinit var etCorreo: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var btnCrearCuenta: Button
    private lateinit var btnLoginGoogle: Button

    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Verificar si ya hay una sesión abierta
        verificar_sesion_abierta()

        // Inicializar vistas
        initViews()

        // Configurar listeners
        setupListeners()
    }

    private fun initViews() {
        etCorreo = findViewById(R.id.etCorreo)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btn_login)
        btnCrearCuenta = findViewById(R.id.btn_crearcuenta)
        btnLoginGoogle = findViewById(R.id.btnLoginGoogle)
    }

    private fun setupListeners() {
        // Botón de login con email y contraseña
        btnLogin.setOnClickListener {
            val correo = etCorreo.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (correo.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Por favor completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            login_firebase(correo, password)
        }

        // Botón de crear cuenta
        btnCrearCuenta.setOnClickListener {
            val correo = etCorreo.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (correo.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Por favor completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            crearCuentaFirebase(correo, password)
        }

        // Botón de login con Google
        btnLoginGoogle.setOnClickListener {
            lifecycleScope.launch {
                loginGoogle(this@MainActivity)
            }
        }
    }

    object Global {
        var preferencias_compartidas = "sharedpreferences"
    }

    private fun verificar_sesion_abierta() {
        val sesionAbierta: SharedPreferences = this.getSharedPreferences(
            Global.preferencias_compartidas,
            MODE_PRIVATE
        )
        val correo = sesionAbierta.getString("Correo", null)
        val proveedor = sesionAbierta.getString("Proveedor", null)

        if (correo != null && proveedor != null) {
            val intent = Intent(applicationContext, Bienvenida::class.java)
            intent.putExtra("Correo", correo)
            intent.putExtra("Proveedor", proveedor)
            startActivity(intent)
            finish() // Terminar esta actividad para que no regrese al presionar atrás
        }
    }

    private fun guardar_sesion(correo: String, proveedor: String) {
        val guardarSesion: SharedPreferences.Editor = this.getSharedPreferences(
            Global.preferencias_compartidas,
            MODE_PRIVATE
        ).edit()
        guardarSesion.putString("Correo", correo)
        guardarSesion.putString("Proveedor", proveedor)
        guardarSesion.apply()
    }

    private suspend fun loginGoogle(context: Context) {
        val credentialManager = CredentialManager.create(context)

        val signInWithGoogleOption: GetSignInWithGoogleOption =
            GetSignInWithGoogleOption.Builder(context.getString(R.string.web_client))
                .setNonce("nonce")
                .build()

        val request: GetCredentialRequest = GetCredentialRequest.Builder()
            .addCredentialOption(signInWithGoogleOption)
            .build()

        try {
            val result = credentialManager.getCredential(
                request = request,
                context = context
            )
            handleSignIn(result)
        } catch (e: GetCredentialException) {
            Toast.makeText(
                context,
                "Error al obtener la credencial: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun handleSignIn(result: GetCredentialResponse) {
        val credential = result.credential
        when (credential) {
            is CustomCredential -> {
                if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    try {
                        val googleIdTokenCredential = GoogleIdTokenCredential
                            .createFrom(credential.data)
                        val credencial =
                            GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)

                        FirebaseAuth.getInstance().signInWithCredential(credencial)
                            .addOnCompleteListener(this) { task ->
                                if (task.isSuccessful) {
                                    val intent = Intent(applicationContext, Bienvenida::class.java)
                                    intent.putExtra(
                                        "Correo",
                                        FirebaseAuth.getInstance().currentUser?.email.toString()
                                    )
                                    intent.putExtra("Proveedor", "Google")
                                    startActivity(intent)
                                    guardar_sesion(task.result.user?.email.toString(), "Google")
                                    finish()
                                } else {
                                    Toast.makeText(
                                        applicationContext,
                                        "Error en la autenticación con Firebase",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                    } catch (e: GoogleIdTokenParsingException) {
                        Log.e(TAG, "Received an invalid google id token response", e)
                        Toast.makeText(
                            applicationContext,
                            "Error al procesar las credenciales de Google",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
            else -> {
                Log.e(TAG, "Unexpected type of credential")
                Toast.makeText(
                    applicationContext,
                    "Tipo de credencial no compatible",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun login_firebase(correo: String, pass: String) {
        FirebaseAuth.getInstance().signInWithEmailAndPassword(correo, pass)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val intent = Intent(applicationContext, Bienvenida::class.java)
                    intent.putExtra("Correo", task.result.user?.email)
                    intent.putExtra("Proveedor", "Usuario/Contraseña")
                    startActivity(intent)
                    guardar_sesion(task.result.user?.email.toString(), "Usuario/Contraseña")
                    finish()
                } else {
                    Toast.makeText(
                        applicationContext,
                        "Usuario/Contraseña incorrecto(s)",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    private fun crearCuentaFirebase(correo: String, password: String) {
        FirebaseAuth.getInstance().createUserWithEmailAndPassword(correo, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(
                        applicationContext,
                        "Cuenta creada exitosamente",
                        Toast.LENGTH_SHORT
                    ).show()
                    // Limpiar campos después de crear la cuenta
                    etCorreo.text.clear()
                    etPassword.text.clear()
                } else {
                    Toast.makeText(
                        applicationContext,
                        "Error al crear la cuenta: ${task.exception?.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }
}