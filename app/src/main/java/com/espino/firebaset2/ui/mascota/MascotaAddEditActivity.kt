package com.espino.firebaset2.ui.mascota

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.espino.firebaset2.R
import com.espino.firebaset2.data.model.Mascota
import com.espino.firebaset2.data.repository.AuthRepository
import com.espino.firebaset2.data.repository.MascotaRepository
import com.espino.firebaset2.databinding.ActivityMascotaAddEditBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.ByteArrayOutputStream

class MascotaAddEditActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMascotaAddEditBinding
    private lateinit var addEditPetViewModel: MascotaAddEditViewModel
    private var petId: String? = null
    private var selectedImageBase64: String? = null

    // Registro para seleccionar imagen
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { uri ->
                try {
                    val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
                    binding.ivPetImage.setImageBitmap(bitmap)
                    selectedImageBase64 = bitmapToBase64(bitmap)
                } catch (e: Exception) {
                    Toast.makeText(this, "Error al cargar la imagen: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Registro para solicitar permiso
    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            pickImageLauncher.launch(intent)
        } else {
            Toast.makeText(this, "Permiso de almacenamiento denegado", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMascotaAddEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Inicializa Firebase Auth y Firestore
        val firebaseAuth = FirebaseAuth.getInstance()
        val firestore = FirebaseFirestore.getInstance()

        // Inicializa los repositorios
        val authRepository = AuthRepository(firebaseAuth, firestore)
        val petRepository = MascotaRepository(firestore)

        // Inicializa el ViewModel
        addEditPetViewModel = ViewModelProvider(this, AddEditPetViewModelFactory(petRepository, authRepository))
            .get(MascotaAddEditViewModel::class.java)

        // Configurar título y campos según si es edición o creación
        petId = intent.getStringExtra("PET_ID")
        if (petId != null) {
            binding.tvTitle.text = "Editar Mascota"
            binding.etPetName.setText(intent.getStringExtra("PET_NAME"))
            binding.etPetType.setText(intent.getStringExtra("PET_TYPE"))
            binding.etPetAge.setText(intent.getIntExtra("PET_AGE", 0).toString())
            val imageBase64 = intent.getStringExtra("PET_IMAGE")
            if (!imageBase64.isNullOrEmpty()) {
                val bitmap = base64ToBitmap(imageBase64)
                binding.ivPetImage.setImageBitmap(bitmap)
                selectedImageBase64 = imageBase64
            }
        } else {
            binding.tvTitle.text = "Añadir Nueva Mascota"
        }

        // Listener para seleccionar imagen
        binding.btnSelectImage.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                pickImageLauncher.launch(intent)
            } else {
                requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        // Listener para guardar mascota
        binding.btnSavePet.setOnClickListener {
            savePet()
        }

        // Observar resultado de guardar
        addEditPetViewModel.saveResult.observe(this) { result ->
            result.onSuccess {
                Toast.makeText(this, "Mascota guardada exitosamente", Toast.LENGTH_SHORT).show()
                finish()
            }.onFailure { exception ->
                Toast.makeText(this, "Error al guardar mascota: ${exception.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun savePet() {
        val name = binding.etPetName.text.toString().trim()
        val type = binding.etPetType.text.toString().trim()
        val ageString = binding.etPetAge.text.toString().trim()

        if (name.isEmpty() || type.isEmpty() || ageString.isEmpty()) {
            Toast.makeText(this, "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        val age = ageString.toIntOrNull()
        if (age == null || age <= 0) {
            Toast.makeText(this, "La edad debe ser un número válido y positivo", Toast.LENGTH_SHORT).show()
            return
        }

        val pet = Mascota(
            id = petId ?: "",
            name = name,
            type = type,
            age = age,
            ownerId = "",
            imageBase64 = selectedImageBase64
        )

        addEditPetViewModel.savePet(pet)
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)
    }

    private fun base64ToBitmap(base64: String): Bitmap? {
        return try {
            val decodedBytes = Base64.decode(base64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) {
            null
        }
    }
}

class AddEditPetViewModelFactory(
    private val petRepository: MascotaRepository,
    private val authRepository: AuthRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MascotaAddEditViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MascotaAddEditViewModel(petRepository, authRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}