package com.example.myklinikadnin.ui.staff

import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.example.myklinikadnin.R
import com.example.myklinikadnin.data.Patient
import com.google.firebase.firestore.FirebaseFirestore

class PatientAdapter(
    private val patients: List<Patient>,
    private val role: String // Doctor or Nurse
) : RecyclerView.Adapter<PatientAdapter.PatientViewHolder>() {

    inner class PatientViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textName: TextView = view.findViewById(R.id.textName)
        val textEmail: TextView = view.findViewById(R.id.textEmail)
        val textPhone: TextView = view.findViewById(R.id.textPhone)
        val detailsLayout: LinearLayout = view.findViewById(R.id.detailsLayout)
        val textBloodType: TextView = view.findViewById(R.id.textBloodType)
        val textBloodPressure: TextView = view.findViewById(R.id.textBloodPressure)
        val textHeight: TextView = view.findViewById(R.id.textHeight)
        val textWeight: TextView = view.findViewById(R.id.textWeight)
        val textNric: TextView = view.findViewById(R.id.textNric)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PatientViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_patient, parent, false)
        return PatientViewHolder(view)
    }

    override fun onBindViewHolder(holder: PatientViewHolder, position: Int) {
        val patient = patients[position]

        holder.textName.text = patient.name
        holder.textEmail.text = patient.email
        holder.textPhone.text = if (patient.phone.isNotEmpty()) patient.phone else "-"

        holder.textBloodType.text = "Blood Type: ${patient.bloodType}"
        holder.textBloodPressure.text = "Blood Pressure: ${patient.bloodPressure}"
        holder.textHeight.text = "Height: ${patient.height} cm"
        holder.textWeight.text = "Weight: ${patient.weight} kg"
        holder.textNric.text = "NRIC: ${patient.nric}"

        holder.detailsLayout.visibility = View.GONE

        holder.itemView.setOnClickListener {
            if (role == "Nurse") {
                showEditDialog(holder.itemView, patient)
            } else {
                holder.detailsLayout.visibility =
                    if (holder.detailsLayout.visibility == View.GONE) View.VISIBLE else View.GONE
            }
        }
    }

    override fun getItemCount() = patients.size

    private fun showEditDialog(view: View, patient: Patient) {
        val context = view.context
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_edit_patient, null)

        val spinnerBloodType = dialogView.findViewById<Spinner>(R.id.spinnerBloodType)
        val editBloodPressure = dialogView.findViewById<EditText>(R.id.editBloodPressure)
        val editHeight = dialogView.findViewById<EditText>(R.id.editHeight)
        val editWeight = dialogView.findViewById<EditText>(R.id.editWeight)

        val bloodTypes = listOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-")
        val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, bloodTypes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerBloodType.adapter = adapter
        spinnerBloodType.setSelection(bloodTypes.indexOf(patient.bloodType))

        editBloodPressure.setText(patient.bloodPressure)
        editHeight.setText(patient.height.toString())
        editWeight.setText(patient.weight.toString())

        AlertDialog.Builder(context)
            .setTitle("Edit Patient: ${patient.name}")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val updated = mapOf(
                    "bloodType" to spinnerBloodType.selectedItem.toString(),
                    "bloodPressure" to editBloodPressure.text.toString(),
                    "height" to (editHeight.text.toString().toIntOrNull() ?: 0),
                    "weight" to (editWeight.text.toString().toIntOrNull() ?: 0)
                )

                FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(patient.uid)
                    .update(updated)
                    .addOnSuccessListener {
                        Toast.makeText(context, "Updated successfully", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, "Update failed", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
