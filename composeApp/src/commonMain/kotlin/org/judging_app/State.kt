package org.judging_app

object State {
    var currentScreen: Routes = Routes.ENTRY
    enum class Routes(val path: String) {
        ENTRY("entry"),
        DISCIPLINE_MODE("discipline_mode");

        override fun toString(): String = path
    }
}