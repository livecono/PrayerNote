package com.prayernote.app.data.local.dao

import androidx.paging.PagingSource
import androidx.room.*
import com.prayernote.app.data.local.entity.Person
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonDao {
    @Query("SELECT * FROM persons ORDER BY createdAt DESC")
    fun getAllPersons(): Flow<List<Person>>

    @Query("SELECT * FROM persons ORDER BY createdAt DESC")
    fun getAllPersonsPaged(): PagingSource<Int, Person>

    @Query("SELECT * FROM persons WHERE id = :personId")
    fun getPersonById(personId: Long): Flow<Person?>

    @Query("SELECT * FROM persons WHERE name LIKE '%' || :query || '%'")
    fun searchPersons(query: String): Flow<List<Person>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPerson(person: Person): Long

    @Update
    suspend fun updatePerson(person: Person)

    @Delete
    suspend fun deletePerson(person: Person)

    @Query("DELETE FROM persons WHERE id = :personId")
    suspend fun deletePersonById(personId: Long)

    @Query("SELECT COUNT(*) FROM persons")
    suspend fun getPersonCount(): Int
}
