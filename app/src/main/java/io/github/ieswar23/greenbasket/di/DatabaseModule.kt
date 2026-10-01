package io.github.ieswar23.greenbasket.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.ieswar23.greenbasket.data.local.GreenBasketDatabase
import io.github.ieswar23.greenbasket.data.local.dao.AddressDao
import io.github.ieswar23.greenbasket.data.local.dao.CartDao
import io.github.ieswar23.greenbasket.data.local.dao.CatalogDao
import io.github.ieswar23.greenbasket.data.local.dao.OrderDao
import io.github.ieswar23.greenbasket.data.local.dao.WishlistDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): GreenBasketDatabase =
        Room.databaseBuilder(context, GreenBasketDatabase::class.java, GreenBasketDatabase.NAME)
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideCatalogDao(db: GreenBasketDatabase): CatalogDao = db.catalogDao()
    @Provides fun provideCartDao(db: GreenBasketDatabase): CartDao = db.cartDao()
    @Provides fun provideWishlistDao(db: GreenBasketDatabase): WishlistDao = db.wishlistDao()
    @Provides fun provideOrderDao(db: GreenBasketDatabase): OrderDao = db.orderDao()
    @Provides fun provideAddressDao(db: GreenBasketDatabase): AddressDao = db.addressDao()
}
