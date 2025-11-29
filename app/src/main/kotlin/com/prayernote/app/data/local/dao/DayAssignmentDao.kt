package com.prayernote.app.data.local.dao

import androidx.room.*
import com.prayernote.app.data.local.entity.DayAssignment
import com.prayernote.app.data.local.entity.Person
import kotlinx.coroutines.flow.Flow

data class PersonWithDay(
    @Embedded val person: Person,
    val dayOfWeek: Int
)

@Dao
interface DayAssignmentDao {
    @Query("SELECT * FROM day_assignments WHERE dayOfWeek = :dayOfWeek")
    fun getAssignmentsByDay(dayOfWeek: Int): Flow<List<DayAssignment>>

    @Query("""
        SELECT persons.*, day_assignments.dayOfWeek 
        FROM persons 
        INNER JOIN day_assignments ON persons.id = day_assignments.personId 
        WHERE day_assignments.dayOfWeek = :dayOfWeek
        ORDER BY persons.name ASC
    """)
    fun getPersonsByDay(dayOfWeek: Int): Flow<List<PersonWithDay>>

    @Query("SELECT * FROM day_assignments WHERE personId = :personId")
    fun getAssignmentsByPerson(personId: Long): Flow<List<DayAssignment>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAssignment(assignment: DayAssignment): Long

    @Delete
    suspend fun deleteAssignment(assignment: DayAssignment)

    @Query("DELETE FROM day_assignments WHERE personId = :personId AND dayOfWeek = :dayOfWeek")
    suspend fun deleteAssignment(personId: Long, dayOfWeek: Int)

    @Query("DELETE FROM day_assignments WHERE personId = :personId")
    suspend fun deleteAssignmentsByPerson(personId: Long)

    @Query("SELECT EXISTS(SELECT 1 FROM day_assignments WHERE personId = :personId AND dayOfWeek = :dayOfWeek)")
    suspend fun isAssigned(personId: Long, dayOfWeek: Int): Boolean
}
