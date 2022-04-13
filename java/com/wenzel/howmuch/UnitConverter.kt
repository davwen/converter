package com.wenzel.howmuch

import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.concurrent.thread

class UnitConverter{
    fun convertUnits(from : Unit, to : Unit, quantity : BigDecimal) : BigDecimal {
        return solveEquation(to.invRelativityToSI, solveEquation(from.relativityToSI, quantity))
    }

    private fun solveEquation(input : String, x : BigDecimal) : BigDecimal{
        val expression = input.replace("x", x.toPlainString())

        val methods = mutableListOf<String>()
        for (method : CalcMethod in CalcMethod.values()){ methods.add(method.string) }

        val calculationsString = expression.split("/")
        val calculations = mutableListOf<Calculation>()

        var result = BigDecimal.ZERO

        val r: Runnable = object : Runnable {
            override fun run() {
                for (calc: String in calculationsString) {
                    if (calc.contains(":")) {
                        val method = calc.substring(0, calc.indexOf(":"))
                        val factor = calc.substring(calc.indexOf(":") + 1, calc.lastIndex + 1)
                        calculations.add(
                            Calculation(
                                CalcMethod.valueOf(method),
                                factor.toBigDecimal()
                            )
                        )
                    } else {
                        if (calc.isNotEmpty()) {
                            result = BigDecimal(calc)
                        }
                    }
                }

                for(calc : Calculation in calculations){
                    when(calc.method){
                        CalcMethod.ADD -> result = result.add(calc.factor)
                        CalcMethod.SUBTRACT -> result = result.subtract(calc.factor)
                        CalcMethod.MULTIPLY -> result = result.multiply(calc.factor)
                        CalcMethod.DIVIDE -> result = result.divide(calc.factor, 10, RoundingMode.HALF_EVEN)
                    }
                }
            }
        }
        r.run()

        return result
    }
}

class Unit (_nameSingular : String, _namePlural : String, _relativityToSI : String, _invRelativityToSI : String, _quantityType : QuantityType, _localisation : Localisation, _alternateNames : List<String>) {
    val nameSingular = _nameSingular
    val namePlural = _namePlural
    val relativityToSI = _relativityToSI
    val invRelativityToSI = _invRelativityToSI
    val quantityType = _quantityType
    val localisation = _localisation
    val alternateNames = _alternateNames

    fun getNameByType(nameType : UnitNameType) : String{

        when(nameType){
            UnitNameType.SINGULAR -> return nameSingular
            UnitNameType.PLURAL -> return namePlural
            UnitNameType.UNKNOWN -> return "unknown"
            else -> return alternateNames[0]
        }
    }
}

class Calculation (_method : CalcMethod, _factor : BigDecimal) {
    val method = _method
    val factor = _factor
}

enum class QuantityType (val string : String) {
    DISTANCE("Distance"), VOLUME("Volume"), WEIGHT("Weight"), TEMPERATURE("Temperature"), SPEED("Speed"), POWER("Power"), UNKNOWN("Unknown")
}

enum class Localisation (val string : String) {
    METRIC("Metric \uD83C\uDDEA\uD83C\uDDFA"), IMPERIAL("Imperial \uD83C\uDDFA\uD83C\uDDF8"), SCIENTIFIC("Scientific"), OTHER("Other"), UNKNOWN("Unknown")
}

enum class CalcMethod (val string : String) {
    ADD("/ADD:"), SUBTRACT("/SUBTRACT:"), MULTIPLY("/MULTIPLY:"), DIVIDE("/DIVIDE:")
}