package com.wenzel.howmuch

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.res.Resources

class MultiChoiceDialog {
    fun getDialog(context: Context, choices : Array<String>, title : String, checkedChoices : BooleanArray) : AlertDialog.Builder {

            val builder: AlertDialog.Builder = AlertDialog.Builder(context)


            // Convert the choice array to list
            val choicesList: List<String> = choices.asList()

              /*  builder.setMultiChoiceItems(
                choices,
                checkedChoices,
                DialogInterface.OnMultiChoiceClickListener() { dialog, which, isChecked -> // Update the current focused item's checked status
                    checkedChoices[which] = isChecked


                    // Get the current focused item
                    val currentItem = choicesList[which]
                })*/

        builder.setSingleChoiceItems(
            choices, 0, DialogInterface.OnClickListener() {
                dialog, which ->
                val currentItem = choicesList[which]
                checkedChoices[0] = false
                checkedChoices[1] = false
                checkedChoices[which] = true

                dialog.cancel()

            }

        )

            // Specify the dialog is not cancelable
            builder.setCancelable(false)

            // Set a title for alert dialog
            builder.setTitle(title)



            // Set the positive/yes button click listener


            return builder
    }
}