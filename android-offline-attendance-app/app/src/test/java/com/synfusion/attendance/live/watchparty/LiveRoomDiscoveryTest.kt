package com.synfusion.attendance.live.watchparty

import com.synfusion.attendance.live.data.InMemoryLiveRoomBackend
import com.synfusion.attendance.live.data.LiveRoomCreateRequest
import com.synfusion.attendance.live.model.LiveOperationResult
import com.synfusion.attendance.live.model.LiveRoomType
import com.synfusion.attendance.live.model.LiveUserIdentity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests covering real active room creation, discovery streams, real-time addition/removal,
 * host identity mapping, private/ended room filtering, and demo room separation.
 */
class LiveRoomDiscoveryTest {

    private val hostUserA = LiveUserIdentity(
        uid = "user_a_uid",
        displayName = "Ramesh Site Engineer",
        username = "ramesh_eng",
        photoUrl = "https://cdn.example/ramesh.jpg",
        profession = "Civil Engineer"
    )

    private val hostUserB = LiveUserIdentity(
        uid = "user_b_uid",
        displayName = "Suresh Contractor",
        username = "suresh_contractor",
        photoUrl = "https://cdn.example/suresh.jpg",
        profession = "General Contractor"
    )

    private var now = 2_000_000L
    private fun backend() = InMemoryLiveRoomBackend(clock = { now })

    @Test
    fun `real room creation is discoverable through active room stream`() = runBlocking {
        val backend = backend()

        // Initially no active rooms
        val initialActive = backend.observeActiveRooms().first()
        assertTrue(initialActive.isEmpty())

        // User A starts a real live room
        val createResult = backend.createRoom(
            hostUserA,
            LiveRoomCreateRequest(
                title = "Concrete Foundation Safety Inspection",
                category = "Site Safety",
                type = LiveRoomType.VIDEO,
                tags = listOf("Safety", "Concrete"),
                isPublic = true
            )
        )
        assertTrue(createResult is LiveOperationResult.Success)
        val createdRoom = (createResult as LiveOperationResult.Success).data

        // User B observes active rooms stream
        val activeRooms = backend.observeActiveRooms().first()
        assertEquals(1, activeRooms.size)

        val discoveredRoom = activeRooms.first()
        assertEquals(createdRoom.id, discoveredRoom.id)
        assertEquals(hostUserA.uid, discoveredRoom.hostUserId)
        assertEquals(hostUserA.displayName, discoveredRoom.creatorName)
        assertEquals(hostUserA.photoUrl, discoveredRoom.creatorAvatar)
        assertEquals("ramesh_eng", discoveredRoom.hostUsername)
        assertEquals("Concrete Foundation Safety Inspection", discoveredRoom.title)
        assertTrue(discoveredRoom.isLive)
        assertFalse(discoveredRoom.isDemoRoom)
    }

    @Test
    fun `realtime addition and removal of active rooms`() = runBlocking {
        val backend = backend()

        // Room 1 created
        val room1Result = backend.createRoom(
            hostUserA,
            LiveRoomCreateRequest(title = "Room 1", category = "Engineering", type = LiveRoomType.VIDEO)
        ) as LiveOperationResult.Success
        assertEquals(1, backend.observeActiveRooms().first().size)

        // Room 2 created by another user
        val room2Result = backend.createRoom(
            hostUserB,
            LiveRoomCreateRequest(title = "Room 2", category = "Contracting", type = LiveRoomType.AUDIO)
        ) as LiveOperationResult.Success
        assertEquals(2, backend.observeActiveRooms().first().size)

        // Ending Room 1 removes it from active room stream
        backend.endRoom(room1Result.data.id, hostUserA.uid)

        val remainingActive = backend.observeActiveRooms().first()
        assertEquals(1, remainingActive.size)
        assertEquals(room2Result.data.id, remainingActive.first().id)
    }

    @Test
    fun `ended and private rooms are excluded from active room stream`() = runBlocking {
        val backend = backend()

        // Public room
        val publicRoom = (backend.createRoom(
            hostUserA,
            LiveRoomCreateRequest(title = "Public Live", category = "Safety", type = LiveRoomType.VIDEO, isPublic = true)
        ) as LiveOperationResult.Success).data

        // Private room
        backend.createRoom(
            hostUserB,
            LiveRoomCreateRequest(title = "Private Briefing", category = "Safety", type = LiveRoomType.VIDEO, isPublic = false)
        )

        val active = backend.observeActiveRooms().first()
        // Only public room appears in active stream
        assertEquals(1, active.size)
        assertEquals(publicRoom.id, active.first().id)

        // End public room
        backend.endRoom(publicRoom.id, hostUserA.uid)
        assertTrue(backend.observeActiveRooms().first().isEmpty())
    }

    @Test
    fun `room opening by another authenticated user loads exact host identity`() = runBlocking {
        val backend = backend()

        val room = (backend.createRoom(
            hostUserA,
            LiveRoomCreateRequest(title = "Real Stream", category = "Engineering", type = LiveRoomType.VIDEO)
        ) as LiveOperationResult.Success).data

        // User B loads the room by ID
        val loadedResult = backend.loadRoom(room.id)
        assertTrue(loadedResult is LiveOperationResult.Success)
        val loadedRoom = (loadedResult as LiveOperationResult.Success).data
        assertNotNull(loadedRoom)
        assertEquals(hostUserA.uid, loadedRoom!!.hostUserId)
        assertEquals("Ramesh Site Engineer", loadedRoom.creatorName)
        assertEquals("ramesh_eng", loadedRoom.hostUsername)
        assertFalse(loadedRoom.isDemoRoom)
    }

    @Test
    fun `listener and membership session cleanup on leave`() = runBlocking {
        val backend = backend()

        val room = (backend.createRoom(
            hostUserA,
            LiveRoomCreateRequest(title = "Test Session", category = "General", type = LiveRoomType.VIDEO)
        ) as LiveOperationResult.Success).data

        // Host is initial member
        assertEquals(1, backend.observeMembers(room.id).first().size)

        // User B joins
        backend.joinRoom(room.id, hostUserB)
        assertEquals(2, backend.observeMembers(room.id).first().size)

        // User B leaves
        backend.leaveRoom(room.id, hostUserB.uid)
        val membersAfterLeave = backend.observeMembers(room.id).first()
        assertEquals(1, membersAfterLeave.size)
        assertEquals(hostUserA.uid, membersAfterLeave.first().uid)
    }
}
