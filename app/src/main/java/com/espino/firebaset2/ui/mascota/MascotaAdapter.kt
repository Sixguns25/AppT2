package com.espino.firebaset2.ui.mascota

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.espino.firebaset2.data.model.Mascota
import com.espino.firebaset2.databinding.ItemMascotaBinding

class MascotaAdapter(
    private val onEditClick: (Mascota) -> Unit,
    private val onDeleteClick: (String, String) -> Unit // Modificado para pasar ID y nombre
) : ListAdapter<Mascota, MascotaAdapter.MascotaViewHolder>(PetDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MascotaViewHolder {
        val binding = ItemMascotaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MascotaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MascotaViewHolder, position: Int) {
        val mascota = getItem(position)
        holder.bind(mascota)
    }

    inner class MascotaViewHolder(private val binding: ItemMascotaBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.btnEditPet.setOnClickListener {
                onEditClick(getItem(adapterPosition))
            }
            binding.btnDeletePet.setOnClickListener {
                val pet = getItem(adapterPosition)
                onDeleteClick(pet.id, pet.name) // Pasa ID y nombre al callback
            }
        }

        fun bind(pet: Mascota) {
            binding.tvPetName.text = pet.name
            binding.tvPetType.text = "Tipo: ${pet.type}"
            binding.tvPetAge.text = "Edad: ${pet.age} años"
            // Mostrar imagen si existe
            pet.imageBase64?.let { base64 ->
                val bitmap = try {
                    val decodedBytes = Base64.decode(base64, Base64.DEFAULT)
                    BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                } catch (e: Exception) {
                    null
                }
                binding.ivPetImage.setImageBitmap(bitmap)
            } ?: run {
                binding.ivPetImage.setImageResource(android.R.drawable.ic_menu_gallery) // Imagen por defecto
            }
        }
    }

    class PetDiffCallback : DiffUtil.ItemCallback<Mascota>() {
        override fun areItemsTheSame(oldItem: Mascota, newItem: Mascota): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Mascota, newItem: Mascota): Boolean {
            return oldItem == newItem
        }
    }
}