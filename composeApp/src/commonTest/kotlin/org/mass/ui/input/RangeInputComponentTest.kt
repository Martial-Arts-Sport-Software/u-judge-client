package org.mass.ui.input

import kotlin.test.Test
import kotlin.test.assertEquals

class RangeInputComponentTest {
    @Test
    fun activeCriterionSliderStartsAtOneTenth() {
        assertEquals((1..10).toList(), rangeInputStepValues(showSlider = true, steps = 10))
    }

    @Test
    fun staticScaleStillShowsZero() {
        assertEquals((0..10).toList(), rangeInputStepValues(showSlider = false, steps = 10))
    }
}
