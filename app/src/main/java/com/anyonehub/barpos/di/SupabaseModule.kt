// Copyright 2024 anyone-Hub
@file:OptIn(SupabaseInternal::class, ExperimentalTime::class)
@file:Suppress("unused")

package com.anyonehub.barpos.di

import android.util.Log
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.annotations.SupabaseInternal
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.compose.auth.ComposeAuth
import io.github.jan.supabase.compose.auth.composeAuth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.PropertyConversionMethod
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.serializer.KotlinXSerializer
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.headers
import kotlinx.serialization.json.Json
import javax.inject.Named
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.time.ExperimentalTime

/**
 * Compatibility Object:
 * Exposes constants expected by ViewModels specifically for the high-volume POS.
 */
object SupabaseConstants {
    const val TABLE_MENU_GROUPS = "menu_groups"
    const val TABLE_CATEGORIES = "categories"
    const val TABLE_MENU_ITEMS = "menu_items"
    const val TABLE_USERS = "users"
    const val TABLE_ACTIVE_TABS = "active_tabs"
    const val TABLE_TAB_ITEMS = "tab_items"
    const val TABLE_APP_SETTINGS = "app_settings"
    const val TABLE_SALES_RECORDS = "sales_records"
    const val TABLE_SALE_ITEMS = "sale_items"
    const val TABLE_TIP_LOGS = "tip_logs"
    const val TABLE_TIME_CLOCK = "time_clock_entries"
    const val TABLE_CUSTOMERS = "customers"
    const val TABLE_Z_REPORTS = "z_reports"
    const val TABLE_AUDIT_LOGS = "audit_logs"

    // High-Volume Extensions
    const val TABLE_DRAWER_LOGS = "drawer_logs"
    const val TABLE_LOYALTY = "loyalty_ledger"
    const val TABLE_INVENTORY_AUDIT = "inventory_audit_log"

    const val BUCKET_STAFF_AVATARS = "staff_avatars"
    const val BUCKET_RECEIPT_BACKUPS = "receipt_backups"
    const val BUCKET_MENU_ASSETS = "menu_assets"

    // Edge Function Names
    const val FUNCTION_DAILY_SUMMARY = "daily-summary"
    const val FUNCTION_INVENTORY_ALERT = "inventory-alert"
    const val FUNCTION_SERVER_CHECKOUT = "server-checkout"
    const val FUNCTION_SECURE_AUDIT = "secure-audit"
}

@Module
@InstallIn(SingletonComponent::class)
object SupabaseModule {
    // --- CONFIGURATION ---
    private const val SUPABASE_URL = "https://lqiyrjfkweovyzdqszwu.supabase.co"

    // STANDARD CLIENT KEY (Publishable - Safe for public distribution)
    private const val SUPABASE_PUBLISHABLE_KEY = "sb_publishable_mVByaMmz920_kwKuHz5Rxg_po-hbXY_"

    // ADMIN CLIENT KEY (Secret - Bypasses RLS - FOR ADMIN/OWNER TERMINALS ONLY)
    private const val SUPABASE_SECRET_KEY = "sb_secret_egGO_oyU61IGR8aSdcVewQ_ckSpBe8A"

    // Shared JSON config to ensure both clients serialize data exactly the same way
    private val commonJson = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
        prettyPrint = false
    }

    // --- NORMAL CLIENT (Uses Publishable Key) ---
    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient {
        return createSupabaseClient(
            supabaseUrl = SUPABASE_URL,
            supabaseKey = SUPABASE_PUBLISHABLE_KEY
        ) {
            configureCommonClient(SUPABASE_PUBLISHABLE_KEY)
        }
    }

    // --- ADMIN CLIENT (Uses Secret Key) ---
    @Provides
    @Singleton
    @Named("admin")
    fun provideAdminSupabaseClient(): SupabaseClient {
        return createSupabaseClient(
            supabaseUrl = SUPABASE_URL,
            supabaseKey = SUPABASE_SECRET_KEY
        ) {
            configureCommonClient(SUPABASE_SECRET_KEY)
        }
    }

    // --- SHARED CONFIGURATION LOGIC ---
    private fun io.github.jan.supabase.SupabaseClientBuilder.configureCommonClient(
        apiKey: String
    ) {
        httpEngine = OkHttp.create {
            config {
                followRedirects(true)
                retryOnConnectionFailure(true)
            }
        }

        defaultSerializer = KotlinXSerializer(commonJson)

        httpConfig {
            headers {
                append("apikey", apiKey)
                append("Authorization", "Bearer $apiKey")
                val now: Instant = Clock.System.now()
                append("X-Client-Initialized-At", now.toString())
            }

            install(Logging) {
                level = LogLevel.HEADERS
                logger = object : Logger {
                    override fun log(message: String) {
                        Log.d("SupabaseNetwork", message)
                    }
                }
            }
        }

        install(Auth) {
            alwaysAutoRefresh = true
            autoLoadFromStorage = true
            host = "login-callback"
            scheme = "com.anyonehub.barpos"
        }

        install(Postgrest) {
            defaultSchema = "public"
            propertyConversionMethod = PropertyConversionMethod.CAMEL_CASE_TO_SNAKE_CASE
        }

        install(Storage) {
            transferTimeout = 90.seconds
        }

        install(Realtime) {
            reconnectDelay = 5.seconds
        }

        install(Functions)
        install(ComposeAuth) // Shared across both for consistency
    }

    // --- NORMAL PROVIDERS (Default injections) ---

    @Provides @Singleton
    fun provideSupabaseComposeAuth(client: SupabaseClient): ComposeAuth = client.composeAuth

    @Provides @Singleton
    fun provideSupabaseDatabase(client: SupabaseClient): Postgrest = client.postgrest

    @Provides @Singleton
    fun provideSupabaseAuth(client: SupabaseClient): Auth = client.auth

    @Provides @Singleton
    fun provideSupabaseStorage(client: SupabaseClient): Storage = client.storage

    @Provides @Singleton
    fun provideSupabaseRealtime(client: SupabaseClient): Realtime = client.realtime

    @Provides @Singleton
    fun provideSupabaseFunctions(client: SupabaseClient): Functions = client.functions

    // --- ADMIN PROVIDERS (Elevated injections) ---

    @Provides @Singleton @Named("admin")
    fun provideAdminSupabaseDatabase(@Named("admin") client: SupabaseClient): Postgrest = client.postgrest

    @Provides @Singleton @Named("admin")
    fun provideAdminSupabaseAuth(@Named("admin") client: SupabaseClient): Auth = client.auth

    @Provides @Singleton @Named("admin")
    fun provideAdminSupabaseStorage(@Named("admin") client: SupabaseClient): Storage = client.storage

    @Provides @Singleton @Named("admin")
    fun provideAdminSupabaseFunctions(@Named("admin") client: SupabaseClient): Functions = client.functions

    @Provides @Singleton @Named("admin")
    fun provideAdminSupabaseRealtime(@Named("admin") client: SupabaseClient): Realtime = client.realtime

    @Provides @Singleton @Named("admin")
    fun provideAdminSupabaseComposeAuth(@Named("admin") client: SupabaseClient): ComposeAuth = client.composeAuth

    // --- Shared Identifiers ---
    @Provides @Named("daily-summary")
    fun provideDailySummaryName(): String = SupabaseConstants.FUNCTION_DAILY_SUMMARY

    @Provides @Named("inventory-alert")
    fun provideInventoryAlertName(): String = SupabaseConstants.FUNCTION_INVENTORY_ALERT

    @Provides @Named("server-checkout")
    fun provideServerCheckoutName(): String = SupabaseConstants.FUNCTION_SERVER_CHECKOUT

    @Provides @Named("secure-audit")
    fun provideSecureAuditName(): String = SupabaseConstants.FUNCTION_SECURE_AUDIT

    @Provides @Named("staff_avatars")
    fun provideStaffAvatarsBucket(): String = SupabaseConstants.BUCKET_STAFF_AVATARS

    @Provides @Named("receipt_backups")
    fun provideReceiptBackupsBucket(): String = SupabaseConstants.BUCKET_RECEIPT_BACKUPS

    @Provides @Named("menu_assets")
    fun provideMenuAssetsBucket(): String = SupabaseConstants.BUCKET_MENU_ASSETS
}