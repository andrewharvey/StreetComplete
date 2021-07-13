package de.westnordost.streetcomplete.quests.traffic_signals_ref

import de.westnordost.streetcomplete.R
import de.westnordost.streetcomplete.data.quest.NoCountriesExcept
import de.westnordost.streetcomplete.data.osm.osmquests.OsmFilterQuestType
import de.westnordost.streetcomplete.data.osm.edits.update_tags.StringMapChangesBuilder
import de.westnordost.streetcomplete.ktx.arrayOfNotNull
import de.westnordost.streetcomplete.ktx.containsAnyKey

class AddTrafficSignalsRef : OsmFilterQuestType<TrafficSignalsRefAnswer>() {

    override val elementFilter = "nodes with highway=traffic_signals and !ref and !ref:signed"

    override val commitMessage = "Add traffic signal refs"
    override val wikiLink = "Tag:highway=traffic_signals"
    override val icon = R.drawable.ic_quest_mail_ref
    override val isDeleteElementEnabled = true

    override val enabledInCountries = NoCountriesExcept(
        "AU"
    )

    override fun getTitle(tags: Map<String, String>): Int =
        R.string.quest_trafficSignalsRef_title

    override fun createForm() = AddTrafficSignalsRefForm()

    override fun applyAnswerTo(answer: TrafficSignalsRefAnswer, changes: StringMapChangesBuilder) {
        when(answer) {
            is NoRefVisible -> changes.add("ref:signed", "no")
            is Ref ->          changes.add("ref", answer.ref)
        }
    }
}
