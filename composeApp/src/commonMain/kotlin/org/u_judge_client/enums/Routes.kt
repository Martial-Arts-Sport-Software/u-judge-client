package org.u_judge_client.enums
import org.u_judge_client.screens.Screen

/**
 * App routes, for each of them there is specific [Screen] instance
 */
enum class Routes(val path: String) {
    ENTRY("entry"),

    DISCIPLINE_SELECT("discipline_select"),
    CATEGORY_SELECT("category_select"),

    KERUGI_MODE("kerugi_mode"),
    TANBON_MODE("tanbon_mode"),
    HOSINSOOL_MODE("hosinsool_mode"),
    FREESTYLE_MODE("freestyle_mode"),

    BACK("");

    override fun toString(): String = path
}