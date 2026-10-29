package com.synfusion.attendance.live.media

import com.synfusion.attendance.live.data.InMemoryLiveRoomBackend
import com.synfusion.attendance.live.data.LiveRoomCreateRequest
import com.synfusion.attendance.live.model.LiveOperationResult
import com.synfusion.attendance.live.model.LiveRoomType
import com.synfusion.attendance.live.viewmodel.HostStartupStep
import com.synfusion.attendance.live.viewmodel.HostStartupStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HostStartupStateMachineTest {

    private val backend = InMemoryLiveRoomBackend()

    @Test
    fun testReserveAndActivateMediaRoom() = runBlocking {
        val request = LiveRoomCreateRequest(
            title = "Test Stream",
            category = "Site Safety",
            type = LiveRoomType.VIDEO
        )
        val reserveRes = backend.reserveMediaRoom(request)
        assertTrue(reserveRes is LiveOperationResult.Success)
        val roomId = (reserveRes as LiveOperationResult.Success).data
        assertTrue(roomId.isNotBlank())

        val initialRoom = backend.loadRoom(roomId)
        assertTrue(initialRoom is LiveOperationResult.Success)
        val roomDoc = (initialRoom as LiveOperationResult.Success).data
        assertEquals("PREPARING", roomDoc?.statusText)
        assertFalse(roomDoc?.isLive == true)

        val activateRes = backend.activateMediaRoom(roomId)
        assertTrue(activateRes is LiveOperationResult.Success)

        val activeRoom = backend.loadRoom(roomId)
        val activeDoc = (activeRoom as LiveOperationResult.Success).data
        assertEquals("LIVE", activeDoc?.statusText)
        assertTrue(activeDoc?.isLive == true)
    }

    @Test
    fun testEndRoomAfterActivation() = runBlocking {
        val request = LiveRoomCreateRequest(
            title = "Voice Session",
            category = "Site Discussion",
            type = LiveRoomType.AUDIO
        )
        val roomId = (backend.reserveMediaRoom(request) as LiveOperationResult.Success).data
        backend.activateMediaRoom(roomId)

        val endRes = backend.endRoom(roomId, "test_host")
        assertTrue(endRes is LiveOperationResult.Success)

        val endedRoom = (backend.loadRoom(roomId) as LiveOperationResult.Success).data
        assertEquals("ENDED", endedRoom?.statusText)
        assertFalse(endedRoom?.isLive == true)
    }

    @Test
    fun testHostStartupStatusTransitions() {
        var status = HostStartupStatus()
        assertEquals(HostStartupStep.IDLE, status.step)

        status = HostStartupStatus(HostStartupStep.RESERVING)
        assertEquals(HostStartupStep.RESERVING, status.step)

        status = HostStartupStatus(HostStartupStep.CONNECTING, roomId = "room_123")
        assertEquals(HostStartupStep.CONNECTING, status.step)
        assertEquals("room_123", status.roomId)

        status = HostStartupStatus(HostStartupStep.PUBLISHING, roomId = "room_123")
        assertEquals(HostStartupStep.PUBLISHING, status.step)

        status = HostStartupStatus(HostStartupStep.ACTIVATING, roomId = "room_123")
        assertEquals(HostStartupStep.ACTIVATING, status.step)

        status = HostStartupStatus(HostStartupStep.LIVE, roomId = "room_123")
        assertEquals(HostStartupStep.LIVE, status.step)
    }
}
