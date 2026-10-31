package com.synfusion.workdailyadmin

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.functions.FirebaseFunctionsException
import com.synfusion.workdailyadmin.data.AdminErrorType
import com.synfusion.workdailyadmin.data.AdminRepository
import com.synfusion.workdailyadmin.data.models.DashboardStats
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException
import java.lang.reflect.Constructor
import java.net.UnknownHostException

class AdminRepositoryTest {

    private fun createFunctionsException(
        code: FirebaseFunctionsException.Code,
        message: String = code.name
    ): FirebaseFunctionsException {
        // FirebaseFunctionsException constructors are package-private in the Firebase SDK
        val constructor: Constructor<FirebaseFunctionsException> =
            FirebaseFunctionsException::class.java.getDeclaredConstructor(
                String::class.java,
                FirebaseFunctionsException.Code::class.java,
                Any::class.java
            )
        constructor.isAccessible = true
        return constructor.newInstance(message, code, null)
    }

    @Test
    fun testClassifyError_networkExceptions() {
        val netErr1 = UnknownHostException("Unable to resolve host")
        val result1 = AdminRepository.classifyError(netErr1)
        assertEquals(AdminErrorType.NETWORK_UNAVAILABLE, result1.errorType)
        assertTrue(result1.canRetry)

        val netErr2 = FirebaseNetworkException("Network disconnected")
        val result2 = AdminRepository.classifyError(netErr2)
        assertEquals(AdminErrorType.NETWORK_UNAVAILABLE, result2.errorType)
        assertTrue(result2.canRetry)

        val ioErr = IOException("Connection reset by peer")
        val result3 = AdminRepository.classifyError(ioErr)
        assertEquals(AdminErrorType.NETWORK_UNAVAILABLE, result3.errorType)
        assertTrue(result3.canRetry)
    }

    @Test
    fun testClassifyError_functionsNotFound() {
        val fnErr = createFunctionsException(FirebaseFunctionsException.Code.NOT_FOUND)
        val result = AdminRepository.classifyError(fnErr)
        assertEquals(AdminErrorType.BACKEND_FUNCTION_NOT_FOUND, result.errorType)
        assertFalse(result.message.contains("NOT_FOUND"))
        assertTrue(result.message.contains("Cloud Function"))
    }

    @Test
    fun testClassifyError_functionsPermissionDenied() {
        val fnErr = createFunctionsException(FirebaseFunctionsException.Code.PERMISSION_DENIED)
        val result = AdminRepository.classifyError(fnErr)
        assertEquals(AdminErrorType.PERMISSION_DENIED, result.errorType)
        assertFalse(result.canRetry)
    }

    @Test
    fun testClassifyError_functionsUnauthenticated() {
        val fnErr = createFunctionsException(FirebaseFunctionsException.Code.UNAUTHENTICATED)
        val result = AdminRepository.classifyError(fnErr)
        assertEquals(AdminErrorType.AUTHENTICATION_EXPIRED, result.errorType)
        assertFalse(result.canRetry)
    }

    @Test
    fun testClassifyError_firestorePermissionDenied() {
        val fsErr = FirebaseFirestoreException(
            "PERMISSION_DENIED",
            FirebaseFirestoreException.Code.PERMISSION_DENIED
        )
        val result = AdminRepository.classifyError(fsErr)
        assertEquals(AdminErrorType.PERMISSION_DENIED, result.errorType)
        assertFalse(result.canRetry)
    }

    @Test
    fun testDashboardStats_zeroCountsAreValid() {
        val emptyStats = DashboardStats(
            totalUsers = 0,
            suspendedUsers = 0,
            bannedUsers = 0,
            verifiedUsers = 0,
            activeLiveRooms = 0,
            pendingReports = 0,
            totalCoinsIssued = 0,
            totalCoinsSpent = 0,
            pendingRedemptionsCount = 0,
            systemStatus = "OPERATIONAL"
        )
        assertEquals(0, emptyStats.totalUsers)
        assertEquals(0, emptyStats.activeLiveRooms)
        assertEquals("OPERATIONAL", emptyStats.systemStatus)
    }
}
