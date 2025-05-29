package com.espino.firebaset2.ui.mascota

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.espino.firebaset2.R
import com.espino.firebaset2.data.model.Mascota
import com.espino.firebaset2.data.repository.AuthRepository
import com.espino.firebaset2.data.repository.MascotaRepository
import com.espino.firebaset2.databinding.ActivityMascotaListBinding
import com.espino.firebaset2.ui.auth.LoginActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MascotaListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMascotaListBinding
    private lateinit var viewModel: MascotaListViewModel
    private lateinit var adapter: MascotaAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMascotaListBinding.inflate(layoutInflater)
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
        viewModel = ViewModelProvider(this, MascotaListViewModelFactory(petRepository, authRepository))
            .get(MascotaListViewModel::class.java)

        // Configura el RecyclerView
        adapter = MascotaAdapter(
            onEditClick = { pet ->
                val intent = Intent(this, MascotaAddEditActivity::class.java).apply {
                    putExtra("PET_ID", pet.id)
                    putExtra("PET_NAME", pet.name)
                    putExtra("PET_TYPE", pet.type)
                    putExtra("PET_AGE", pet.age)
                    putExtra("PET_IMAGE", pet.imageBase64)
                }
                startActivity(intent)
            },
            onDeleteClick = { petId, petName ->
                showDeleteConfirmationDialog(petId, petName) // Muestra el diálogo de confirmación
            }
        )
        binding.rvPets.layoutManager = LinearLayoutManager(this)
        binding.rvPets.adapter = adapter

        // Observa la lista de mascotas
        viewModel.pets.observe(this) { result ->
            result.onSuccess { pets ->
                adapter.submitList(pets)
            }.onFailure { exception ->
                Toast.makeText(this, "Error al cargar mascotas: ${exception.message}", Toast.LENGTH_LONG).show()
            }
        }

        // Observa el resultado de eliminación
        viewModel.deleteResult.observe(this) { result ->
            result.onSuccess {
                Toast.makeText(this, "Mascota eliminada exitosamente", Toast.LENGTH_SHORT).show()
            }.onFailure { exception ->
                Toast.makeText(this, "Error al eliminar mascota: ${exception.message}", Toast.LENGTH_LONG).show()
            }
        }

        // Observa si el usuario no está autenticado
        viewModel.isUserNotAuthenticated.observe(this) { isNotAuthenticated ->
            if (isNotAuthenticated) {
                Toast.makeText(this, "Sesión no iniciada", Toast.LENGTH_SHORT).show()
                navigateToLogin()
            }
        }

        // Listener para cerrar sesión
        binding.btnCerrarSesion.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show()
            navigateToLogin()
        }

        // Listener para añadir nueva mascota
        binding.btnAddPet.setOnClickListener {
            val intent = Intent(this, MascotaAddEditActivity::class.java)
            startActivity(intent)
        }
    }

    private fun showDeleteConfirmationDialog(petId: String, petName: String) {
        AlertDialog.Builder(this)
            .setTitle("Confirmar eliminación")
            .setMessage("¿Estás seguro que deseas eliminar a tu mascota $petName?")
            .setPositiveButton("Sí") { _, _ ->
                viewModel.deletePet(petId) // Procede con la eliminación
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss() // Cierra el diálogo sin hacer nada
            }
            .setCancelable(true)
            .show()
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}

class MascotaListViewModelFactory(
    private val petRepository: MascotaRepository,
    private val authRepository: AuthRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MascotaListViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MascotaListViewModel(petRepository, authRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}