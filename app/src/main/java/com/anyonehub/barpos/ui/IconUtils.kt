// Copyright 2024 anyone-Hub
package com.anyonehub.barpos.ui

import com.anyonehub.barpos.R

/**
 * MASTER ICON MAPPING ENGINE
 * Maps database string IDs to high-quality 3D Drawable Resources.
 * Hardened for the high-volume MidTown POS environment.
 * All icons are exposed for runtime user configuration.
 */
object IconUtils {
    /**
     * Maps database string IDs to 3D Drawable Resources.
     * Normalizes input to lowercase for database safety.
     */
    fun getIconResource(iconName: String?): Int {
        return when (iconName?.lowercase() ?: "beer") {
            // --- CORE MENU CATEGORIES ---
            "beer"       -> R.drawable.ic_beer_3d
            "food"       -> R.drawable.ic_food_3d
            "shot"       -> R.drawable.ic_shot_3d
            "cocktail"   -> R.drawable.ic_cocktail_3d
            "wine"       -> R.drawable.ic_wine_3d
            "soda"       -> R.drawable.ic_soda_3d
            "soft_drink" -> R.drawable.ic_soda_3d
            
            // --- SYSTEM & NAVIGATION ---
            "customers"  -> R.drawable.ic_customers_3d
            "share"      -> R.drawable.ic_share_3d
            "settings"   -> R.drawable.ic_settings_3d
            "star"       -> R.drawable.ic_star_on
            "rotate"     -> R.drawable.ic_menu_rotate
            "compass"    -> R.drawable.ic_menu_compass
            "calendar"   -> R.drawable.ic_menu_my_calendar
            "camera"     -> R.drawable.ic_menu_camera
            "view"       -> R.drawable.ic_menu_view
            "admin"      -> R.drawable.ic_admin_3d
            "audit"      -> R.drawable.ic_audit_3d
            "history"    -> R.drawable.ic_history_3d
            "search"     -> R.drawable.ic_search_3d
            "tag"        -> R.drawable.ic_tag_3d
            "add"        -> R.drawable.ic_3d_add
            "lock"       -> R.drawable.ic_3d_lock
            "unlock"     -> R.drawable.ic_unlock_3d
            "save"       -> R.drawable.ic_save
            "delete"     -> R.drawable.ic_3d_delete
            "refill"     -> R.drawable.ic_3d_refill
            "midtown"    -> R.drawable.ic_midtown_3d
            "time"       -> R.drawable.ic_time_3d
            "light_mode" -> R.drawable.ic_light_mode_3d
            "dark_mode"  -> R.drawable.ic_dark_mode_3d
            "logout"     -> R.drawable.ic_logout_3d
            "tip_tracker" -> R.drawable.ic_tip_tracker_3d
            "zreport"    -> R.drawable.ic_zreport_3d
            "profile"    -> R.drawable.ic_user_profile_3d
            "hamburger"  -> R.drawable.ic_menu_3d
            "undo"       -> R.drawable.ic_undo
            "redo"       -> R.drawable.ic_redo
            "cloud"      -> R.drawable.ic_cloud_upload

            else -> R.drawable.ic_beer_3d // Safe production fallback
        }
    }

    /**
     * Options for the "Create/Edit Category" or "Edit Item" dialogs.
     * Every icon in the library is made available for runtime configuration.
     */
    val availableIcons = listOf(
        "beer"        to R.drawable.ic_beer_3d,
        "food"        to R.drawable.ic_food_3d,
        "shot"        to R.drawable.ic_shot_3d,
        "cocktail"    to R.drawable.ic_cocktail_3d,
        "wine"        to R.drawable.ic_wine_3d,
        "soda"        to R.drawable.ic_soda_3d,
        "customers"   to R.drawable.ic_customers_3d,
        "star"        to R.drawable.ic_star_on,
        "camera"      to R.drawable.ic_menu_camera,
        "calendar"    to R.drawable.ic_menu_my_calendar,
        "compass"     to R.drawable.ic_menu_compass,
        "rotate"      to R.drawable.ic_menu_rotate,
        "view"        to R.drawable.ic_menu_view,
        "admin"       to R.drawable.ic_admin_3d,
        "audit"       to R.drawable.ic_audit_3d,
        "history"     to R.drawable.ic_history_3d,
        "tag"         to R.drawable.ic_tag_3d,
        "refill"      to R.drawable.ic_3d_refill,
        "time"        to R.drawable.ic_time_3d,
        "tip_tracker" to R.drawable.ic_tip_tracker_3d,
        "zreport"     to R.drawable.ic_zreport_3d,
        "cloud"       to R.drawable.ic_cloud_upload,
        "search"      to R.drawable.ic_search_3d,
        "share"       to R.drawable.ic_share_3d,
        "lock"        to R.drawable.ic_3d_lock,
        "unlock"      to R.drawable.ic_unlock_3d,
        "save"        to R.drawable.ic_save,
        "delete"      to R.drawable.ic_3d_delete,
        "logout"      to R.drawable.ic_logout_3d,
        "profile"     to R.drawable.ic_user_profile_3d,
        "hamburger"   to R.drawable.ic_menu_3d,
        "undo"        to R.drawable.ic_undo,
        "redo"        to R.drawable.ic_redo
    )
}
