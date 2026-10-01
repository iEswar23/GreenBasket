package io.github.ieswar23.greenbasket.data.local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "cart_items")
data class CartItemEntity(
    @PrimaryKey val productId: String,
    val quantity: Int,
    val addedAt: Long,
)

/** Cart row joined with its product. */
data class CartLine(
    @Embedded val product: ProductEntity,
    val quantity: Int,
    val addedAt: Long,
)

@Entity(tableName = "wishlist")
data class WishlistEntity(
    @PrimaryKey val productId: String,
    val addedAt: Long,
)

@Entity(tableName = "addresses")
data class AddressEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
    val receiverName: String,
    val phone: String,
    val houseDetails: String,
    val area: String,
    val city: String,
    val pincode: String,
)

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey val id: String,
    val placedAt: Long,
    val status: String,
    val slotLabel: String,
    val isExpress: Boolean,
    val slotStartMillis: Long,
    val addressLabel: String,
    val addressLine: String,
    val paymentMethod: String,
    val itemTotal: Int,
    val mrpTotal: Int,
    val deliveryFee: Int,
    val handlingFee: Int,
    val couponCode: String?,
    val couponDiscount: Int,
    val grandTotal: Int,
)

@Entity(
    tableName = "order_items",
    primaryKeys = ["orderId", "productId"],
    foreignKeys = [
        ForeignKey(
            entity = OrderEntity::class,
            parentColumns = ["id"],
            childColumns = ["orderId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("orderId"), Index("productId")],
)
data class OrderItemEntity(
    val orderId: String,
    val productId: String,
    val name: String,
    val packSize: String,
    val emoji: String,
    val tint: String,
    val price: Int,
    val mrp: Int,
    val quantity: Int,
)

data class OrderWithItems(
    @Embedded val order: OrderEntity,
    @Relation(parentColumn = "id", entityColumn = "orderId")
    val items: List<OrderItemEntity>,
)
