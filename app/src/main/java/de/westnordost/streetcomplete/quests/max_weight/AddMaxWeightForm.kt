package de.westnordost.streetcomplete.quests.max_weight

import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import android.text.method.DigitsKeyListener
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner

import de.westnordost.streetcomplete.R
import de.westnordost.streetcomplete.ktx.numberOrNull
import de.westnordost.streetcomplete.quests.AbstractQuestFormAnswerFragment
import de.westnordost.streetcomplete.quests.OtherAnswer
import de.westnordost.streetcomplete.util.TextChangedWatcher

import de.westnordost.streetcomplete.quests.max_weight.Measurement.*

private enum class Measurement { TON, USA }

class AddMaxWeightForm : AbstractQuestFormAnswerFragment<MaxWeightAnswer>() {

    override val otherAnswers = listOf(
            OtherAnswer(R.string.quest_maxweight_answer_other_sign) { onUnsupportedSign() },
            OtherAnswer(R.string.quest_maxweight_answer_sign_with_exceptions) { onUnsupportedSign() },
            OtherAnswer(R.string.quest_maxweight_answer_noSign) { confirmNoSign() }
    )

    private fun onUnsupportedSign() {
        AlertDialog.Builder(activity!!)
                .setMessage(R.string.quest_maxweight_unsupported_sign_request_photo)
                .setPositiveButton(android.R.string.ok) { _, _ -> onClickCantSay() }
                .setNegativeButton(R.string.quest_leave_new_note_no) { _, _ -> skipQuest() }
                .show()

    }

    private fun confirmNoSign() {
        AlertDialog.Builder(activity!!)
                .setTitle(R.string.quest_generic_confirmation_title)
                .setPositiveButton(R.string.quest_generic_confirmation_yes) { _, _ -> applyAnswer(NoMaxWeightSign) }
                .setNegativeButton(R.string.quest_generic_confirmation_no, null)
                .show()
    }

    private var tonInput: EditText? = null
    private var poundInput: EditText? = null
    private var weightUnitSelect: Spinner? = null

    private var tonInputSign: View? = null
    private var poundInputSign: View? = null

    override fun isFormComplete() = getWeightFromInput() != null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState)

        val unit = when(countryInfo.measurementSystemForWeightLimits) {
            listOf("ton") -> TON
            listOf("short_ton_formatted_as_ton", "pounds") -> USA
            else -> throw UnsupportedOperationException("not implemented")
        }
        setMaxWeightSignLayout(R.layout.quest_maxweight, unit)

        return view
    }

    private fun setMaxWeightSignLayout(resourceId: Int, unit: Measurement) {
        val contentView = setContentView(resourceId)

        tonInput = contentView.findViewById(R.id.tonInput)
        poundInput = contentView.findViewById(R.id.poundInput)

        val onTextChangedListener = TextChangedWatcher { checkIsFormComplete() }
        tonInput?.addTextChangedListener(onTextChangedListener)
        poundInput?.addTextChangedListener(onTextChangedListener)

        tonInputSign = contentView.findViewById(R.id.tonInputSign)
        poundInputSign = contentView.findViewById(R.id.poundInputSign)

        weightUnitSelect = contentView.findViewById(R.id.weightUnitSelect)
        val measurementUnits = countryInfo.measurementSystem
        weightUnitSelect?.visibility = if (measurementUnits.size == 1) View.GONE else View.VISIBLE
        weightUnitSelect?.adapter = ArrayAdapter(context!!, R.layout.spinner_item_centered, getSpinnerItems(measurementUnits))
        weightUnitSelect?.setSelection(measurementUnits.size - 1) // prefer metric
        weightUnitSelect?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parentView: AdapterView<*>, selectedItemView: View?, position: Int, id: Long) {
                val weightUnit = if (weightUnitSelect?.selectedItem == "t") TON else USA
                switchLayout(weightUnit)
            }

            override fun onNothingSelected(parentView: AdapterView<*>) {}
        }

        /* Workaround for an Android bug that it assumes the decimal separator to always be the "."
           for EditTexts with inputType "numberDecimal", independent of Locale. See
           https://issuetracker.google.com/issues/36907764 .

           Affected Android versions are all versions till (exclusive) Android Oreo. */

        /* actually, let's not care about which separator the user uses, he might be confused
           whether he should use the one as displayed on the sign or in his phone's locale */
        //char separator = DecimalFormatSymbols.getInstance(getCountryInfo().getLocale()).getDecimalSeparator();
        tonInput?.keyListener = DigitsKeyListener.getInstance("0123456789,.")

        switchLayout(unit)
    }

    private fun switchLayout(unit: Measurement) {
        val isMetric = unit == TON
        val isImperial = unit == USA

        tonInputSign?.visibility = if (isMetric) View.VISIBLE else View.GONE
        poundInputSign?.visibility = if (isImperial) View.VISIBLE else View.GONE

        if (isMetric) tonInput?.requestFocus()
        if (isImperial) poundInput?.requestFocus()
    }

    private fun getSpinnerItems(units: List<String>) = units.mapNotNull {
        when(it) {
            "metric" -> "t"
            "imperial" -> "lbs"
            else -> null
        }
    }

    override fun onClickOk() {
        if (userSelectedUnrealisticWeight()) {
            confirmUnusualInput { applyMaxWeightFormAnswer() }
        } else {
            applyMaxWeightFormAnswer()
        }
    }

    private fun userSelectedUnrealisticWeight(): Boolean {
        val weight = getWeightFromInput() ?: return false
        val w = weight.toTons()
        return w > 25 || w < 2
    }

    private fun applyMaxWeightFormAnswer() {
        applyAnswer(MaxWeight(getWeightFromInput()!!))
    }

    private fun getWeightFromInput(): WeightMeasure? {
        try {
            if (isMetric()) {
                val input = tonInput?.numberOrNull
                if (input != null) {
                    val country = countryInfo.countryCode
                    if ("US" == country) {
                        return UsShortTons(input.toDouble())
                    }
                    return MetricWeightMeasure(input.toDouble())
                }
            } else {
                val poundString = poundInput?.numberOrNull
                if (poundString != null) {
                    return ImperialWeightMeasure(poundString.toInt())
                }
            }
        } catch (e: NumberFormatException) {
            return null;
        }
        return null
    }

    private fun isMetric() =
        weightUnitSelect?.let { it.selectedItem == "t" }
            ?: (countryInfo.measurementSystem[0] == "metric")

    private fun confirmUnusualInput(callback: () -> (Unit)) {
        activity?.let {
            AlertDialog.Builder(it)
                .setTitle(R.string.quest_generic_confirmation_title)
                .setMessage(R.string.quest_maxweight_unusualInput_confirmation_description)
                .setPositiveButton(R.string.quest_generic_confirmation_yes) { _, _ -> callback() }
                .setNegativeButton(R.string.quest_generic_confirmation_no, null)
                .show()
        }
    }
}
