package com.example.meditrack

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MedicineAdapter(
    private var medicines: List<Medicine>,
    private val onCheckboxChanged: (Medicine, Boolean) -> Unit,
    private val onDeleteClicked: (Medicine) -> Unit
) : RecyclerView.Adapter<MedicineAdapter.MedicineViewHolder>() {

    class MedicineViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView: TextView = itemView.findViewById(R.id.tvMedicineName)
        val doseTextView: TextView = itemView.findViewById(R.id.tvDose)
        val timeTextView: TextView = itemView.findViewById(R.id.tvTime)
        val consumedCheckBox: CheckBox = itemView.findViewById(R.id.cbConsumed)
        val deleteButton: Button = itemView.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MedicineViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_medicine, parent, false)
        return MedicineViewHolder(view)
    }

    override fun onBindViewHolder(holder: MedicineViewHolder, position: Int) {
        val medicine = medicines[position]

        holder.nameTextView.text = medicine.name
        holder.doseTextView.text = "Dose: ${medicine.dose}"
        holder.timeTextView.text = "Time: ${medicine.time}"
        holder.consumedCheckBox.isChecked = medicine.isConsumed

        holder.consumedCheckBox.setOnCheckedChangeListener { _, isChecked ->
            onCheckboxChanged(medicine, isChecked)
        }

        holder.deleteButton.setOnClickListener {
            onDeleteClicked(medicine)
        }
    }

    override fun getItemCount() = medicines.size

    fun updateMedicines(newMedicines: List<Medicine>) {
        medicines = newMedicines
        notifyDataSetChanged()
    }
}