package io.github.ieswar23.greenbasket.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import io.github.ieswar23.greenbasket.data.local.dao.AddressDao
import io.github.ieswar23.greenbasket.data.local.dao.CartDao
import io.github.ieswar23.greenbasket.data.local.dao.CatalogDao
import io.github.ieswar23.greenbasket.data.local.dao.OrderDao
import io.github.ieswar23.greenbasket.data.local.dao.WishlistDao
import io.github.ieswar23.greenbasket.data.local.entity.AddressEntity
import io.github.ieswar23.greenbasket.data.local.entity.BannerEntity
import io.github.ieswar23.greenbasket.data.local.entity.CartItemEntity
import io.github.ieswar23.greenbasket.data.local.entity.CategoryEntity
import io.github.ieswar23.greenbasket.data.local.entity.OrderEntity
import io.github.ieswar23.greenbasket.data.local.entity.OrderItemEntity
import io.github.ieswar23.greenbasket.data.local.entity.ProductEntity
import io.github.ieswar23.greenbasket.data.local.entity.WishlistEntity

@Database(
    entities = [
        CategoryEntity::class,
        ProductEntity::class,
        BannerEntity::class,
        CartItemEntity::class,
        WishlistEntity::class,
        AddressEntity::class,
        OrderEntity::class,
        OrderItemEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class GreenBasketDatabase : RoomDatabase() {
    abstract fun catalogDao(): CatalogDao
    abstract fun cartDao(): CartDao
    abstract fun wishlistDao(): WishlistDao
    abstract fun orderDao(): OrderDao
    abstract fun addressDao(): AddressDao

    companion object {
        const val NAME = "greenbasket.db"
    }
}
