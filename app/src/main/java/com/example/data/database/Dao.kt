package com.example.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SchoolDao {
    // --- Students ---
    @Query("SELECT * FROM students")
    fun getAllStudents(): Flow<List<Student>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudents(students: List<Student>)

    @Update
    suspend fun updateStudent(student: Student)

    // --- DECE Cases ---
    @Query("SELECT * FROM dece_cases ORDER BY priority DESC, id DESC")
    fun getAllDeceCases(): Flow<List<DeceCase>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeceCase(deceCase: DeceCase)

    @Update
    suspend fun updateDeceCase(deceCase: DeceCase)

    // --- Chats ---
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getChatHistory(): Flow<List<ChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatMessage(message: ChatMessage)

    // --- Notifications ---
    @Query("SELECT * FROM school_notifications ORDER BY timestamp DESC")
    fun getAllNotifications(): Flow<List<SchoolNotification>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: SchoolNotification)

    @Query("UPDATE school_notifications SET isRead = 1 WHERE id = :id")
    suspend fun markNotificationAsRead(id: Int)

    // --- Activities ---
    @Query("SELECT * FROM school_activities ORDER BY dueDate ASC")
    fun getAllActivities(): Flow<List<SchoolActivity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivities(activities: List<SchoolActivity>)

    @Update
    suspend fun updateActivity(activity: SchoolActivity)

    // --- Schedules ---
    @Query("SELECT * FROM schedules")
    fun getAllSchedules(): Flow<List<SchoolSchedule>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedules(schedules: List<SchoolSchedule>)

    // --- User Accounts ---
    @Query("SELECT * FROM user_accounts")
    fun getAllUserAccounts(): Flow<List<UserAccount>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserAccount(user: UserAccount)

    @Query("DELETE FROM user_accounts WHERE username = :username")
    suspend fun deleteUserAccount(username: String)

    // --- Role Permissions ---
    @Query("SELECT * FROM role_permissions")
    fun getAllRolePermissions(): Flow<List<RolePermission>>

    @Query("SELECT * FROM role_permissions WHERE role = :role LIMIT 1")
    suspend fun getRolePermissionByRole(role: String): RolePermission?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRolePermissions(permissions: List<RolePermission>)

    // --- Institution Profiles ---
    @Query("SELECT * FROM institution_profiles")
    fun getAllInstitutions(): Flow<List<InstitutionProfile>>

    @Query("SELECT * FROM institution_profiles WHERE isActive = 1 LIMIT 1")
    fun getActiveInstitution(): Flow<InstitutionProfile?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInstitution(profile: InstitutionProfile)

    @Update
    suspend fun updateInstitution(profile: InstitutionProfile)

    @Query("UPDATE institution_profiles SET isActive = 0")
    suspend fun deactivateAllInstitutions()

    @Transaction
    suspend fun setActiveInstitution(id: Int) {
        deactivateAllInstitutions()
        // Wait, since we can't query and update directly easily in Room without separate SQL, let's write a Query:
        _setActiveInstitutionFlag(id)
    }

    @Query("UPDATE institution_profiles SET isActive = 1 WHERE id = :id")
    suspend fun _setActiveInstitutionFlag(id: Int)

    @Query("DELETE FROM institution_profiles WHERE id = :id")
    suspend fun deleteInstitution(id: Int)

    // --- Custom Templates ---
    @Query("SELECT * FROM custom_templates ORDER BY lastUpdated DESC")
    fun getAllCustomTemplates(): Flow<List<CustomTemplate>>

    @Query("SELECT * FROM custom_templates WHERE institutionId = :institutionId ORDER BY lastUpdated DESC")
    fun getTemplatesForInstitution(institutionId: Int): Flow<List<CustomTemplate>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomTemplate(template: CustomTemplate)

    @Delete
    suspend fun deleteCustomTemplate(template: CustomTemplate)
}
