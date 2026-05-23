package com.dailywork.admin.di

import com.dailywork.admin.data.repository.AdminFirestoreRepository
import com.dailywork.admin.data.repository.FCMRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides
    @Singleton
    fun provideAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideAdminFirestoreRepository(db: FirebaseFirestore, auth: FirebaseAuth): AdminFirestoreRepository {
        return AdminFirestoreRepository(db, auth)
    }

    @Provides
    @Singleton
    fun provideFCMRepository(): FCMRepository {
        return FCMRepository()
    }
}
