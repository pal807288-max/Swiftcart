package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

data class CartItemWithItem(
    @Embedded val cartItem: CartItem,
    @Relation(
        parentColumn = "itemId",
        entityColumn = "id"
    )
    val item: Item?
)

@Dao
interface SwiftCartDao {

    // Store operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStores(stores: List<Store>)

    @Query("SELECT * FROM stores")
    fun getAllStoresFlow(): Flow<List<Store>>

    @Query("SELECT * FROM stores WHERE type = :type")
    fun getStoresByTypeFlow(type: String): Flow<List<Store>>

    @Query("SELECT * FROM stores WHERE name LIKE '%' || :query || '%'")
    fun searchStores(query: String): Flow<List<Store>>

    @Query("SELECT * FROM stores WHERE id = :storeId LIMIT 1")
    suspend fun getStoreById(storeId: Int): Store?

    @Query("DELETE FROM stores")
    suspend fun clearStores()


    // Item operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<Item>)

    @Query("SELECT * FROM items")
    fun getAllItemsFlow(): Flow<List<Item>>

    @Query("SELECT * FROM items WHERE storeId = :storeId")
    fun getItemsByStoreFlow(storeId: Int): Flow<List<Item>>

    @Query("SELECT * FROM items WHERE rating >= 4.7 LIMIT 6")
    fun getRecommendedItemsFlow(): Flow<List<Item>>

    @Query("SELECT * FROM items WHERE name LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%'")
    fun searchItems(query: String): Flow<List<Item>>

    @Query("DELETE FROM items")
    suspend fun clearItems()


    // Cart operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCartItem(cartItem: CartItem)

    @Update
    suspend fun updateCartItem(cartItem: CartItem)

    @Delete
    suspend fun deleteCartItem(cartItem: CartItem)

    @Transaction
    @Query("SELECT * FROM cart_items WHERE userId = :userId")
    fun getCartItemsFlow(userId: Int): Flow<List<CartItemWithItem>>

    @Query("SELECT * FROM cart_items WHERE userId = :userId AND itemId = :itemId LIMIT 1")
    suspend fun getCartItem(userId: Int, itemId: Int): CartItem?

    @Query("DELETE FROM cart_items WHERE userId = :userId")
    suspend fun clearCart(userId: Int)


    // Order operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: Order)

    @Query("SELECT * FROM orders WHERE userId = :userId ORDER BY timestamp DESC")
    fun getOrdersFlow(userId: Int): Flow<List<Order>>

    // Address operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAddress(address: UserAddress)

    @Update
    suspend fun updateAddress(address: UserAddress)

    @Delete
    suspend fun deleteAddress(address: UserAddress)

    @Query("SELECT * FROM user_addresses WHERE userId = :userId ORDER BY id ASC")
    fun getAddressesFlow(userId: Int): Flow<List<UserAddress>>

    @Query("SELECT * FROM user_addresses WHERE id = :id LIMIT 1")
    suspend fun getAddressById(id: Int): UserAddress?

    @Query("UPDATE user_addresses SET isDefault = 0 WHERE userId = :userId")
    suspend fun clearDefaultAddresses(userId: Int)

    @Query("UPDATE user_addresses SET isDefault = 1 WHERE id = :addressId")
    suspend fun setDefaultAddress(addressId: Int)


    // --- Store Owner specific operations ---
    @Query("SELECT * FROM stores WHERE ownerId = :ownerId LIMIT 1")
    suspend fun getStoreByOwner(ownerId: Int): Store?

    @Query("SELECT * FROM stores WHERE ownerId = :ownerId")
    fun getStoreByOwnerFlow(ownerId: Int): Flow<Store?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStore(store: Store): Long

    @Update
    suspend fun updateStore(store: Store)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: Item): Long

    @Update
    suspend fun updateItem(item: Item)

    @Delete
    suspend fun deleteItem(item: Item)

    @Query("SELECT * FROM items WHERE id = :itemId LIMIT 1")
    suspend fun getItemById(itemId: Int): Item?

    @Query("SELECT * FROM orders WHERE storeId = :storeId ORDER BY timestamp DESC")
    fun getOrdersByStoreFlow(storeId: Int): Flow<List<Order>>

    @Update
    suspend fun updateOrder(order: Order)

    @Query("SELECT * FROM orders WHERE id = :orderId LIMIT 1")
    suspend fun getOrderById(orderId: Int): Order?

    // --- Category administrative operations ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: Category): Long

    @Update
    suspend fun updateCategory(category: Category)

    @Delete
    suspend fun deleteCategory(category: Category)

    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAllCategoriesFlow(): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE id = :id LIMIT 1")
    suspend fun getCategoryById(id: Int): Category?

    @Query("SELECT COUNT(*) FROM items WHERE category = :categoryName")
    suspend fun getItemCountByCategory(categoryName: String): Int

    // --- Order administrative operations ---
    @Query("SELECT * FROM orders ORDER BY timestamp DESC")
    fun getAllOrdersFlow(): Flow<List<Order>>

    @Query("SELECT * FROM orders")
    suspend fun getAllOrders(): List<Order>

    @Query("SELECT * FROM stores")
    suspend fun getAllStores(): List<Store>
}
