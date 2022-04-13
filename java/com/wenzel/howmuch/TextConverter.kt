package com.wenzel.howmuch

import android.content.*
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_text_converter.*
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*
import java.util.regex.Pattern


class TextConverter : AppCompatActivity() {

    var units = mutableListOf<com.wenzel.howmuch.Unit>()

    var separatorChar = DecimalFormatSymbols.getInstance().decimalSeparator
    private val numberPattern = Pattern.compile("-?[0-9]+[.,]?[0-9]*")
    private val unitPattern = Pattern.compile("$numberPattern\\ ?([a-zA-Z_/\\-]+)")

    private var lastUnitNameTypes = mutableListOf(UnitNameType.UNKNOWN)

    val excludedOutputUnits = mutableListOf<String>("yard", "decimeter")

    val constantSuitableUnits = mutableListOf<String>("foot", "meter", "foot")

    val excludedOutputLocalisations = mutableListOf<Localisation>(Localisation.UNKNOWN, Localisation.OTHER, Localisation.SCIENTIFIC)

    var outputLocalisations = mutableListOf<Localisation>()

    val currentConversions = mutableListOf<Conversion>()

    val unitReplacementBuilder = StringBuilder()

    var shownSeparatorMsg = false

    override fun onCreate(savedInstanceState: Bundle?) {

        // --- Setup ---
        this.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_text_converter)
        updateUnitsList(QuantityType.DISTANCE, false)
        updateUnitsList(QuantityType.WEIGHT, false)
        updateUnitsList(QuantityType.TEMPERATURE, false)
        updateUnitsList(QuantityType.SPEED, false)
        updateUnitsList(QuantityType.VOLUME, false)
        updateUnitsList(QuantityType.POWER, false)


        editText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                lastUnitNameTypes.clear()
                convertAndUpdate(outputLocalisations)

            }
        })

        // --- Buttons ---

        copyBtn.setOnClickListener{
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("conversion result", outputTxt2.text)
            clipboard.primaryClip = clip

            Toast.makeText(this, R.string.copied_msg,
                Toast.LENGTH_LONG).show()
        }

        unitsBtn.setOnClickListener{
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        // --- Localisation selection ---

        val localisationChoices = mutableListOf<String>()

        var i = 0
        while(i < Localisation.values().count()){
            if(!excludedOutputLocalisations.contains(Localisation.values()[i])){
                localisationChoices.add(Localisation.values()[i].string)
            }

            i++
        }

        val checkedChoices = Collections.nCopies(localisationChoices.count(), false).toBooleanArray()

        chosenOutputBtn.setOnClickListener{
            val dialog = MultiChoiceDialog().getDialog(this, localisationChoices.toTypedArray(), "", checkedChoices)

            dialog.setSingleChoiceItems(localisationChoices.toTypedArray(), checkedChoices.indexOf(true), DialogInterface.OnClickListener() { dialog, which -> // Update the current focused item's checked status
                        checkedChoices[0] = false
                        checkedChoices[1] = false
                        checkedChoices[which] = true

                        outputLocalisations.clear()

                        val outputLocalisationsString = mutableListOf<String>()

                        i = 0
                        while(i < localisationChoices.count()){
                            if(checkedChoices[i]){
                                outputLocalisations.add(Localisation.values()[i])
                                outputLocalisationsString.add(Localisation.values()[i].string)
                            }
                            i++
                        }


                        chosenOutputTxt.text = outputLocalisationsString.joinToString()

                        if(!checkedChoices.contains(true)){
                            chosenOutputTxt.text = getString(R.string.choose_output)
                        }


                        convertAndUpdate(outputLocalisations)

                    dialog.cancel()


            })

            dialog.create().show()
        }


        // Debug stuff

    }

    private fun getQuantitiesInString(input : String) : MutableList<QuantityInString>{
        val result = mutableListOf<QuantityInString>()

        val unitMatcher = unitPattern.matcher(input)

        outputTxt2.setText(input)

        while(unitMatcher.find()){
            val unitString = unitMatcher.group() // actually getting the unit
                .filter { it.isLetter() }.toLowerCase() // formatting

            val numberMatcher = numberPattern.matcher(unitMatcher.group())
            numberMatcher.find()

            if(numberMatcher.group().contains(',') && !shownSeparatorMsg){
                Toast.makeText(this, "Use full stop (.) to separate decimals.",
                    Toast.LENGTH_LONG).show()
                shownSeparatorMsg = true
            }
            result.add(QuantityInString(getUnitFromString(unitString), unitString, BigDecimal(numberMatcher.group().replace(",", "")), unitMatcher.start(), unitMatcher.end()))
            result.removeAll { it.unit.quantityType == QuantityType.UNKNOWN }

        }



        return result
    }

    private fun convertAndUpdate(outputLocalisations: MutableList<Localisation>){
        val quantities = getQuantitiesInString(editText.text.toString())

        var lengthChange = 0

        currentConversions.clear()

        var i = 0
        while(i < quantities.size){

            if(quantities[i].unit.quantityType != QuantityType.UNKNOWN){
                val lastLength = outputTxt2.text.length

                val suitingUnit = findMostSuitingUnit(quantities[i].unit, quantities[i].amount, outputLocalisations)

                var convertedAmount = UnitConverter().convertUnits(quantities[i].unit, suitingUnit, quantities[i].amount)

                convertedAmount = convertedAmount.setScale(3, RoundingMode.HALF_EVEN)

                var convertedUnitName = suitingUnit.namePlural

                if(lastUnitNameTypes[i] != UnitNameType.UNKNOWN){
                    convertedUnitName = suitingUnit.getNameByType(lastUnitNameTypes[i])
                }

                if(lastUnitNameTypes[i] != UnitNameType.UNKNOWN && lastUnitNameTypes[i] != UnitNameType.ALTERNATE){
                    if(convertedAmount != BigDecimal("1")){
                        convertedUnitName = suitingUnit.namePlural
                    }else{
                        convertedUnitName = suitingUnit.nameSingular
                    }

                }

                unitReplacementBuilder.clear()

                val format = DecimalFormat("#.##", DecimalFormatSymbols(Locale.US))

                unitReplacementBuilder.append(format.format(convertedAmount)).append(" ")

                unitReplacementBuilder.append(convertedUnitName)

                outputTxt2.setText(outputTxt2.text.replaceRange(
                    quantities[i].startIndex + lengthChange,
                    quantities[i].endIndex + lengthChange,
                     unitReplacementBuilder))

                if(lastLength != outputTxt2.text.length){
                    lengthChange += outputTxt2.text.length - lastLength
                }

                currentConversions.add(Conversion(quantities[i].unit, suitingUnit))

                Log.d("resultZwei", quantities[i].unit.namePlural)
            }else{Log.d("resultZwei", "is null")}

            i++
        }

    }

    private fun getUnitFromString(unitString: String):com.wenzel.howmuch.Unit {
        var i = 0
        while(i < units.size){
            if(units[i].nameSingular == unitString || units[i].namePlural == unitString || units[i].alternateNames.contains(unitString)){

                when(unitString){
                    units[i].nameSingular -> lastUnitNameTypes.add(UnitNameType.SINGULAR)
                    units[i].namePlural -> lastUnitNameTypes.add(UnitNameType.PLURAL)
                    else -> lastUnitNameTypes.add(UnitNameType.ALTERNATE)
                }

                return units[i]
            }
            if(i == units.size - 1){
                lastUnitNameTypes.add(UnitNameType.UNKNOWN)
                return Unit("unknown", "unknown", "", "", QuantityType.UNKNOWN, Localisation.UNKNOWN, emptyList())

            }

            i++
        }

        return Unit("unknown", "unknown", "", "", QuantityType.UNKNOWN, Localisation.UNKNOWN, emptyList())
    }

    private fun updateUnitsList(quantityType: QuantityType, clearOld : Boolean){
        if(clearOld){units.clear()}

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

    private fun findMostSuitingUnit(inputUnit: Unit, amount: BigDecimal, outputLocalisations : MutableList<Localisation>) : Unit{


        val conversions = mutableListOf<ConversionResult>()

        var i = 0
        while(i < units.size){
            conversions.add(ConversionResult(
                units[i],
                UnitConverter().convertUnits(inputUnit, units[i], amount)))

            i++
        }



        conversions.removeAll { !outputLocalisations.contains(it.unit.localisation)}
        conversions.removeAll { it.unit.quantityType != inputUnit.quantityType }
        conversions.removeAll { excludedOutputUnits.contains(it.unit.nameSingular) }

        if(constantSuitableUnits.contains(inputUnit.nameSingular)){
            val constant = conversions.find { it.unit.nameSingular == constantSuitableUnits[constantSuitableUnits.indexOf(inputUnit.nameSingular) + 1] }
            if(constant != null){
                return constant.unit
            }

        }

        val originalConversions = mutableListOf<ConversionResult>()

        for(conversion in conversions){ // I do this instead of just equalising conversions and originalConversions because else they would always be equal, meaning when I make a change in one, the other changes too.
            originalConversions.add(conversion)
        }

        conversions.removeAll { it.amount >= BigDecimal.ZERO && it.amount < amount.divide(BigDecimal("2")) || it.amount < BigDecimal.ZERO && it.amount > amount.divide(BigDecimal("2")) } // if the converted value is at least more than half of the input, I consider them suitable units.

        if(conversions.size != 0){
            if (conversions.minBy { it.amount }!!.amount >= BigDecimal.ZERO) {
                return conversions.minBy { it.amount }!!.unit
            }
            if (conversions.minBy { it.amount < BigDecimal.ZERO }!!.amount < BigDecimal.ZERO){
                conversions.removeAll{it.amount >= BigDecimal.ZERO}
                return conversions.maxBy { it.amount }!!.unit
            }
        }else{
            return when(originalConversions.size){
                0 -> inputUnit
                else -> originalConversions.maxBy { it.amount }!!.unit
            }

        }

        return inputUnit
    }
}

class QuantityInString(val unit : com.wenzel.howmuch.Unit, val unitString : String, val amount : BigDecimal, val startIndex : Int, val endIndex : Int, val isPlural : Boolean = false)

class ConversionResult(val unit : com.wenzel.howmuch.Unit, val amount : BigDecimal)

enum class UnitNameType(val string : String){
    SINGULAR("SINGULAR"), PLURAL("PLURAL"), ALTERNATE("ALTERNATE"), UNKNOWN("UNKNOWN")
}

class Conversion(val input : Unit, val output : Unit)