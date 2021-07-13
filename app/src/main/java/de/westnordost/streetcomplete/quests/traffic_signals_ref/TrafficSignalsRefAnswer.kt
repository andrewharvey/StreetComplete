package de.westnordost.streetcomplete.quests.traffic_signals_ref

sealed class TrafficSignalsRefAnswer

data class Ref(val ref:String) : TrafficSignalsRefAnswer()
object NoRefVisible : TrafficSignalsRefAnswer()
