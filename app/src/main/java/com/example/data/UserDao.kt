package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User): Long

    @Update
    suspend fun updateUser(user: User)

    @Query("SELECT * FROM users WHERE LOWER(email) = LOWER(:email) LIMIT 1")
    suspend fun getUserByEmail(email: String): User?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: Int): User?

    // Session Management
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActiveSession(session: ActiveSession)

    @Query("DELETE FROM active_session WHERE id = 1")
    suspend fun deleteActiveSession()

    @Query("SELECT * FROM active_session WHERE id = 1 LIMIT 1")
    suspend fun getActiveSession(): ActiveSession?

    @Query("SELECT * FROM active_session WHERE id = 1")
    fun observeActiveSession(): Flow<ActiveSession?>

    @Query("SELECT * FROM users")
    fun observeAllUsers(): Flow<List<User>>

    @Query("SELECT * FROM users")
    suspend fun getAllUsers(): List<User>

    @Query("SELECT * FROM users WHERE role = :role")
    fun observeUsersByRole(role: String): Flow<List<User>>

    @Query("SELECT * FROM users WHERE role = :role")
    suspend fun getUsersByRole(role: String): List<User>
}
