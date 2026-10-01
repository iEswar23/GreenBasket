package io.github.ieswar23.greenbasket.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.ieswar23.greenbasket.data.preferences.DataStorePreferencesRepository
import io.github.ieswar23.greenbasket.data.preferences.PreferencesRepository
import io.github.ieswar23.greenbasket.data.repository.AddressRepository
import io.github.ieswar23.greenbasket.data.repository.CartRepository
import io.github.ieswar23.greenbasket.data.repository.CatalogRepository
import io.github.ieswar23.greenbasket.data.repository.DefaultOrderRepository
import io.github.ieswar23.greenbasket.data.repository.OfflineFirstCatalogRepository
import io.github.ieswar23.greenbasket.data.repository.OrderRepository
import io.github.ieswar23.greenbasket.data.repository.RoomAddressRepository
import io.github.ieswar23.greenbasket.data.repository.RoomCartRepository
import io.github.ieswar23.greenbasket.data.repository.RoomWishlistRepository
import io.github.ieswar23.greenbasket.data.repository.WishlistRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton
    abstract fun bindCatalogRepository(impl: OfflineFirstCatalogRepository): CatalogRepository

    @Binds @Singleton
    abstract fun bindCartRepository(impl: RoomCartRepository): CartRepository

    @Binds @Singleton
    abstract fun bindWishlistRepository(impl: RoomWishlistRepository): WishlistRepository

    @Binds @Singleton
    abstract fun bindAddressRepository(impl: RoomAddressRepository): AddressRepository

    @Binds @Singleton
    abstract fun bindOrderRepository(impl: DefaultOrderRepository): OrderRepository

    @Binds @Singleton
    abstract fun bindPreferencesRepository(impl: DataStorePreferencesRepository): PreferencesRepository
}
