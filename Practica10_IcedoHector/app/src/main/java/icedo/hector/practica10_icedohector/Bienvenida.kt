package icedo.hector.practica10_icedohector

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth

class Bienvenida : AppCompatActivity() {

    private lateinit var evCorreo: TextView
    private lateinit var evProveedor: TextView
    private lateinit var btnSalir: Button

    companion object {
        private const val PREFERENCIAS_COMPARTIDAS = "sharedpreferences"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_bienvenida)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Inicializar vistas
        initViews()

        // Obtener datos del Intent
        obtenerDatosUsuario()

        // Configurar listeners
        setupListeners()
    }

    private fun initViews() {
        evCorreo = findViewById(R.id.evCorreo)
        evProveedor = findViewById(R.id.evProveedor)
        btnSalir = findViewById(R.id.btnSalir)
    }

    private fun obtenerDatosUsuario() {
        // Obtener datos del Intent
        val correo = intent.getStringExtra("Correo")
        val proveedor = intent.getStringExtra("Proveedor")

        // Mostrar la información en los TextViews
        evCorreo.text = "Correo: ${correo ?: "No disponible"}"
        evProveedor.text = "Proveedor: ${proveedor ?: "No disponible"}"

        // Si no hay datos en el Intent, intentar obtenerlos de SharedPreferences
        if (correo == null || proveedor == null) {
            obtenerDatosDesdePreferencias()
        }
    }

    private fun obtenerDatosDesdePreferencias() {
        val sharedPreferences = getSharedPreferences(PREFERENCIAS_COMPARTIDAS, MODE_PRIVATE)
        val correoGuardado = sharedPreferences.getString("Correo", "No disponible")
        val proveedorGuardado = sharedPreferences.getString("Proveedor", "No disponible")

        evCorreo.text = "Correo: $correoGuardado"
        evProveedor.text = "Proveedor: $proveedorGuardado"
    }

    private fun setupListeners() {
        btnSalir.setOnClickListener {
            cerrarSesion()
        }
    }

    private fun cerrarSesion() {
        // Mostrar diálogo de confirmación
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Cerrar Sesión")
            .setMessage("¿Estás seguro de que deseas cerrar sesión?")
            .setPositiveButton("Sí") { _, _ ->
                // Limpiar SharedPreferences
                limpiarPreferencias()

                // Cerrar sesión en Firebase
                FirebaseAuth.getInstance().signOut()

                // Regresar a MainActivity
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()

                Toast.makeText(this, "Sesión cerrada exitosamente", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun limpiarPreferencias() {
        val sharedPreferences = getSharedPreferences(PREFERENCIAS_COMPARTIDAS, MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.clear()
        editor.apply()
    }

    override fun onBackPressed() {
        // Interceptar el botón de retroceso para evitar que regrese al login
        // sin cerrar sesión
        super.onBackPressed()
        // Opcional: puedes mostrar el diálogo de cerrar sesión aquí también
        // cerrarSesion()
    }
}