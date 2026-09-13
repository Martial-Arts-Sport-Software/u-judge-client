package org.mass.enums

/**
 * Disciplines
 */
enum class Disciplines(
    val value: String,
    val route: Routes,
    val requiresCategory: Boolean = false,
    val isSelectable: Boolean = true
) {
    KERUGI("discipline_kerugi", Routes.KERUGI_MODE),
    HOSINSOOL("discipline_hosinsool", Routes.HOSINSOOL_MODE, requiresCategory = true),
    TANBON("discipline_tanbon", Routes.TANBON_MODE),
    FREESTYLE_PAIR("discipline_freestyle_pair", Routes.FREESTYLE_MODE, requiresCategory = true),
    FREESTYLE_GROUP("discipline_freestyle_group", Routes.FREESTYLE_MODE),
    FREESTYLE_SWORD("discipline_freestyle_sword", Routes.FREESTYLE_MODE),
    FREESTYLE_POLE("discipline_freestyle_pole", Routes.FREESTYLE_MODE),
    FREESTYLE_NUNCHAKU("discipline_freestyle_nunchaku", Routes.FREESTYLE_MODE),
    FREESTYLE_FANS("discipline_freestyle_fans", Routes.FREESTYLE_MODE),
    // Retained only to avoid dropping drafts created before the four weapon modes were split.
    FREESTYLE_WEAPON("discipline_freestyle_weapon", Routes.FREESTYLE_MODE, isSelectable = false);

    val usesWeaponCriteria: Boolean
        get() = this in setOf(
            FREESTYLE_SWORD,
            FREESTYLE_POLE,
            FREESTYLE_NUNCHAKU,
            FREESTYLE_FANS,
            FREESTYLE_WEAPON
        )
}
