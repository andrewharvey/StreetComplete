package de.westnordost.streetcomplete.quests

import de.westnordost.streetcomplete.data.osm.edits.update_tags.StringMapEntryAdd
import de.westnordost.streetcomplete.quests.traffic_signals_ref.AddTrafficSignalsRef
import de.westnordost.streetcomplete.quests.traffic_signals_ref.NoRefVisible
import de.westnordost.streetcomplete.quests.traffic_signals_ref.Ref
import org.junit.Test

class AddTrafficSignalsRefTest {

    private val questType = AddTrafficSignalsRef()

    @Test fun `apply no ref answer`() {
        questType.verifyAnswer(
            NoRefVisible,
            StringMapEntryAdd("ref:signed", "no")
        )
    }

    @Test fun `apply ref answer`() {
        questType.verifyAnswer(
            Ref("12d"),
            StringMapEntryAdd("ref", "12d")
        )
    }

}
