package me.gegenbauer.catspy.ui.filter

class FilterAndExpression(
    val left: FilterExpression,
    val right: FilterExpression,
    override val start: Int = 0,
    override val end: Int = 0,
): FilterExpression()