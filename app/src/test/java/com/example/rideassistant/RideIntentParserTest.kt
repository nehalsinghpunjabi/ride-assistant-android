package com.example.rideassistant

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RideIntentParserTest {
    private val parser = RideIntentParser()

    @Test fun parsesPickupDestinationAndAuto() {
        val request = parser.parse("Book me an auto from home to Phoenix Mall").getOrThrow()
        assertEquals("home", request.pickup)
        assertEquals("Phoenix Mall", request.destination)
        assertEquals(RideType.AUTO, request.rideType)
    }

    @Test fun parsesCurrentPickupAndCab() {
        val request = parser.parse("Get me a cab to Pune Airport").getOrThrow()
        assertEquals("current location", request.pickup)
        assertEquals("Pune Airport", request.destination)
        assertEquals(RideType.CAB, request.rideType)
    }

    @Test fun parsesTakeMeHome() {
        assertEquals("home", parser.parse("Take me home").getOrThrow().destination)
    }

    @Test fun unclearRequestFailsGracefully() {
        assertTrue(parser.parse("Book me a ride").isFailure)
    }
}
