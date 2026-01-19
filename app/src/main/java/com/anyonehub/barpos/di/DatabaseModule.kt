// Copyright 2024 anyone-Hub
package com.anyonehub.barpos.di

import android.app.Application
import com.anyonehub.barpos.data.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        app: Application,
        scope: CoroutineScope
    ): AppDatabase = AppDatabase.getDatabase(app, scope)

    @Provides
    fun providePosDao(db: AppDatabase): PosDao = db.posDao()

    @Provides
    fun provideMenuDao(db: AppDatabase): MenuDao = db.menuDao()

    @Provides
    fun provideSalesDao(db: AppDatabase): SalesDao = db.salesDao()

    @Provides
    fun provideReportDao(db: AppDatabase): ReportDao = db.reportDao()

    @Provides
    fun provideTimeClockDao(db: AppDatabase): TimeClockDao = db.timeClockDao()

    @Provides
    fun provideCustomerDao(db: AppDatabase): CustomerDao = db.customerDao()
}
