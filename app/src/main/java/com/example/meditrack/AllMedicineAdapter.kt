package com.example.meditrack


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AllMedicineAdapter(
    private var medicines: List<Medicine>,
    private val onDeleteClicked: (Medicine) -> Unit
) : RecyclerView.Adapter<AllMedicineAdapter.AllMedicineViewHolder>() {

    class AllMedicineViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView: TextView = itemView.findViewById(R.id.tvMedicineName)
        val doseTextView: TextView = itemView.findViewById(R.id.tvDose)
        val timeTextView: TextView = itemView.findViewById(R.id.tvTime)
        val daysTextView: TextView = itemView.findViewById(R.id.tvDays)
        val deleteButton: Button = itemView.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AllMedicineViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_all_medicine, parent, false)
        return AllMedicineViewHolder(view)
    }

    override fun onBindViewHolder(holder: AllMedicineViewHolder, position: Int) {
        val medicine = medicines[position]

        holder.nameTextView.text = medicine.name
        holder.doseTextView.text = "Dose: ${medicine.dose}"
        holder.timeTextView.text = "Time: ${medicine.time}"
        holder.daysTextView.text = "Days: ${medicine.days.replace(",", ", ")}"

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
