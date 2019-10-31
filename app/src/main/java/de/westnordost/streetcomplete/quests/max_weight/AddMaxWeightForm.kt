package de.westnordost.streetcomplete.quests.max_weight

import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner

import de.westnordost.streetcomplete.R
import de.westnordost.streetcomplete.ktx.allowOnlyNumbers
import de.westnordost.streetcomplete.ktx.numberOrNull
import de.westnordost.streetcomplete.quests.AbstractQuestFormAnswerFragment
import de.westnordost.streetcomplete.quests.OtherAnswer
import de.westnordost.streetcomplete.util.TextChangedWatcher

import de.westnordost.streetcomplete.quests.max_weight.Measurement.*

private enum class Measurement { TON, POUND }

class AddMaxWeightForm : AbstractQuestFormAnswerFragment<MaxWeightAnswer>() {

    override val otherAnswers = listOf(
            OtherAnswer(R.string.quest_maxweight_answer_other_sign) { onUnsupportedSign() },
            OtherAnswer(R.string.quest_maxweight_answer_sign_with_exceptions) { onUnsupportedSign() },
            OtherAnswer(R.string.quest_maxweight_answer_noSign) { confirmNoSign() }
    )

    private var tonInput: EditText? = null
    private var poundInput: EditText? = null
    private var weightUnitSelect: Spinner? = null

    private var tonInputSign: View? = null
    private var poundInputSign: View? = null

    override fun isFormComplete() = getWeightFromInput() != null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState)
        setMaxWeightSignLayout(R.layout.quest_maxweight)
        return view
    }

    private fun setMaxWeightSignLayout(resourceId: Int) {
        val contentView = setContentView(resourceId)

        tonInput = contentView.findViewById(R.id.tonInput)
        poundInput = contentView.findViewById(R.id.poundInput)

        val onTextChangedListener = TextChangedWatcher { checkIsFormComplete() }
        tonInput?.addTextChangedListener(onTextChangedListener)
        poundInput?.addTextChangedListener(onTextChangedListener)

        tonInputSign = contentView.findViewById(R.id.tonInputSign)
        poundInputSign = contentView.findViewById(R.id.poundInputSign)

        weightUnitSelect = contentView.findViewById(R.id.weightUnitSelect)
        val measurementUnits = countryInfo.weightLimitUnits
        val primaryUnit = when(measurementUnits[0]) {
            "ton" -> TON
            "short ton" -> TON
            "pound" -> POUND
            else -> throw UnsupportedOperationException("not implemented")
        }
        switchLayout(primaryUnit)
        weightUnitSelect?.visibility = if (measurementUnits.size == 1) View.GONE else View.VISIBLE
        weightUnitSelect?.adapter = ArrayAdapter(context!!, R.layout.spinner_item_centered, getSpinnerItems(measurementUnits))
        weightUnitSelect?.setSelection(0)
        weightUnitSelect?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parentView: AdapterView<*>, selectedItemView: View?, position: Int, id: Long) {
                val weightUnit = when(weightUnitSelect?.selectedItem) {
                    "t" -> TON
                    "lbs" -> POUND
                    else -> throw UnsupportedOperationException("not implemented")
                }
                switchLayout(weightUnit)
            }

            override fun onNothingSelected(parentView: AdapterView<*>) {}
        }
        tonInput?.allowOnlyNumbers()
    }

    private fun switchLayout(unit: Measurement) {
        val isTon = unit == TON
        val isPound = unit == POUND

        tonInputSign?.visibility = if (isTon) View.VISIBLE else View.GONE
        poundInputSign?.visibility = if (isPound) View.VISIBLE else View.GONE

        if (isTon) tonInput?.requestFocus()
        if (isPound) poundInput?.requestFocus()
    }

    private fun getSpinnerItems(units: List<String>) = units.mapNotNull {
        when(it) {
            "ton" -> "t"
            "short ton" -> "t"
            "pound" -> "lbs"
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
        val w = weight.toMetricTons()
        return w > 25 || w < 2
    }

    private fun applyMaxWeightFormAnswer() {
        applyAnswer(MaxWeight(getWeightFromInput()!!))
    }

    private fun getWeightFromInput(): WeightMeasure? {
        try {
            if (isTon()) {
                val input = tonInput?.numberOrNull
                if (input != null) {
                    if (countryInfo.weightLimitUnits.contains("short ton")) {
                        return ShortTons(input.toDouble())
                    }
                    return MetricTons(input.toDouble())
                }
            } else if (isPound()) {
                val poundString = poundInput?.numberOrNull
                if (poundString != null) {
                    return ImperialPounds(poundString.toInt())
                }
            } else {
                throw UnsupportedOperationException("not implemented")
            }
        } catch (e: NumberFormatException) {
            return null
        }
        return null
    }

    private fun isTon() =
        weightUnitSelect?.let { it.selectedItem == "t" }
            ?: (countryInfo.weightLimitUnits[0] == "ton"
                    || countryInfo.weightLimitUnits[0] == "short ton")
        // weightUnitSelect will give a null for cases where there is a single unit
        // in such cases there is a single unit, so we can use [0] to get it

    private fun isPound() =
            weightUnitSelect?.let { it.selectedItem == "lbs" }
                    ?: (countryInfo.weightLimitUnits[0] == "pound")
    // see comment in the isTon function

    private fun onUnsupportedSign() {
        activity?.let { AlertDialog.Builder(it)
            .setMessage(R.string.quest_maxweight_unsupported_sign_request_photo)
            .setPositiveButton(android.R.string.ok) { _, _ -> onClickCantSay() }
            .setNegativeButton(R.string.quest_leave_new_note_no) { _, _ -> skipQuest() }
            .show()
        }
    }

    private fun confirmNoSign() {
        activity?.let { AlertDialog.Builder(it)
            .setTitle(R.string.quest_generic_confirmation_title)
            .setPositiveButton(R.string.quest_generic_confirmation_yes) { _, _ -> applyAnswer(NoMaxWeightSign) }
            .setNegativeButton(R.string.quest_generic_confirmation_no, null)
            .show()
        }
    }

    private fun confirmUnusualInput(callback: () -> (Unit)) {
        activity?.let { AlertDialog.Builder(it)
            .setTitle(R.string.quest_generic_confirmation_title)
            .setMessage(R.string.quest_maxweight_unusualInput_confirmation_description)
            .setPositiveButton(R.string.quest_generic_confirmation_yes) { _, _ -> callback() }
            .setNegativeButton(R.string.quest_generic_confirmation_no, null)
            .show()
        }
    }
}
