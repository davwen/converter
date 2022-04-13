package com.wenzel.howmuch

import android.R.attr.label
import android.R.attr.start
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat


const val outputDecimals : Int = 10
val roundingMode = RoundingMode.HALF_EVEN

class MainActivity : AppCompatActivity() {

    var units = mutableListOf<Unit>()



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        this.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager


        //   ---   Quantity type selector   ---

        val quantityTypes = mutableListOf<String>()
        for (quantity : QuantityType in QuantityType.values()){ if(quantity != QuantityType.UNKNOWN) { quantityTypes.add(quantity.string) } }

        val quantityAdapter: ArrayAdapter<String> = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item, quantityTypes
        )

        quantityAdapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item)

        spinner3.adapter = quantityAdapter

        spinner3.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(parentView: AdapterView<*>?, selectedItemView: View?, position: Int, id: Long
            ) {
                updateUnitsList(QuantityType.values()[position])
                updateSpinners(QuantityType.values()[position])
            }
            override fun onNothingSelected(parentView: AdapterView<*>?) {}
        }

        updateUnitsList(QuantityType.values()[spinner3.selectedItemPosition])
        updateSpinners(QuantityType.values()[spinner3.selectedItemPosition])

        inputTxt.addTextChangedListener(object : TextWatcher{
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                convertAndUpdate()
            }
        })

        spinner.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(parentView: AdapterView<*>?, selectedItemView: View?, position: Int, id: Long
            ) {
                convertAndUpdate()
            }
            override fun onNothingSelected(parentView: AdapterView<*>?) {}
        }

        spinner2.onItemSelectedListener = spinner.onItemSelectedListener

        imageView.setOnClickListener{
            val inputPos = spinner.selectedItemPosition
            spinner.setSelection(spinner2.selectedItemPosition)
            spinner2.setSelection(inputPos)
        }

        outputTxt.setOnLongClickListener(object: View.OnLongClickListener{
            override fun onLongClick(v: View?): Boolean {
                val clip = ClipData.newPlainText("Converted output", outputTxt.text)
                clipboard.primaryClip = clip

                Toast.makeText(v?.context, "Copied", Toast.LENGTH_SHORT).show()
                return false
            }

        })


        backBtn.setOnClickListener(){
            finish()
        }
    }

    private fun convertAndUpdate(){
        val inputUnit = units[spinner.selectedItemPosition]
        val outputUnit = units[spinner2.selectedItemPosition]

        var result = BigDecimal("0")

        if(inputTxt.text.isNotEmpty() && inputTxt.text.toString() != "-" &&  inputTxt.text.toString() != "."){
            result = UnitConverter().convertUnits(inputUnit, outputUnit, inputTxt.text.toString().toBigDecimal())
        }

        val suffix : String

        if(result >= BigDecimal("0.0") && result < BigDecimal("1.0") || result > BigDecimal("1.0")){
            suffix = " " + outputUnit.namePlural
        }else{
            suffix = " " + outputUnit.nameSingular
        }

        var prefix = ""
/*
        if(result.precision() == 1 && inputTxt.text.isNotEmpty() && inputTxt.text.toString().toIntOrNull() == null){
            prefix = "ca "
        }*/

        val outputString = StringBuilder()

        outputString.append(prefix)
        outputString.append(DecimalFormat(generateNumberSigns(outputDecimals)).format(result))
        outputString.append(suffix)

        outputTxt.setText(outputString)
    }



    private fun updateSpinners(quantityType: QuantityType){
        val adapter: ArrayAdapter<String> = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item, mutableListOf<String>()
        )

        var i = 0
        while(i < units.size){
            if(units[i].quantityType == quantityType){
                adapter.add(units[i].namePlural)
            }

            i++
        }

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        spinner.adapter = adapter
        spinner2.adapter = adapter
    }

    private fun updateUnitsList(quantityType: QuantityType){
        units.clear()

        var i = 0
        while(i < resources.getStringArray(R.array.namesSingular).size){
            val name = resources.getStringArray(R.array.namesSingular)[i]
            val namePlural = resources.getStringArray(R.array.namesPlural)[i]
            val alternateNames = resources.getStringArray(R.array.alternateNames)[i].split(";")
            val relativity = resources.getStringArray(R.array.relativities)[i]
            val invRelativity = resources.getStringArray(R.array.invRelativities)[i]
            val quantity = QuantityType.values().firstOrNull {it.name == resources.getStringArray(R.array.quantities)[i]}
            val localisation = Localisation.values().firstOrNull {it.name == resources.getStringArray(R.array.localisations)[i]}

            if(quantity == quantityType){
                units.add(Unit(name, namePlural, relativity, invRelativity, quantity, localisation!!, alternateNames))
            }

            i++
        }
    }

    private fun generateNumberSigns(amount: Int): String? {
        var s = "#."
        for (i in 0 until amount) {
            s += "#"
        }
        return s
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        onBackPressed()
        return true
    }
}



