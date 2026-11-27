package ai.bondhu.assistant.di

import android.content.Context
import androidx.room.Room
import ai.bondhu.assistant.data.db.BondhuDatabase
import ai.bondhu.assistant.data.prefs.SettingsStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun db(@ApplicationContext context: Context): BondhuDatabase =
        Room.databaseBuilder(context, BondhuDatabase::class.java, "bondhu.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    @Singleton
    fun dao(db: BondhuDatabase) = db.dao()

    @Provides
    @Singleton
    fun settings(@ApplicationContext context: Context) = SettingsStore(context)
}
