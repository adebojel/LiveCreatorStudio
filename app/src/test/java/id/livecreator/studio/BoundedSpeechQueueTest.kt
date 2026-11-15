package id.livecreator.studio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BoundedSpeechQueueTest {
    @Test
    fun dequeuePreservesFifoOrder() {
        val queue = BoundedSpeechQueue<String>(3)
        queue.enqueue("first")
        queue.enqueue("second")
        queue.enqueue("third")

        assertEquals("first", queue.dequeue())
        assertEquals("second", queue.dequeue())
        assertEquals("third", queue.dequeue())
        assertNull(queue.dequeue())
    }

    @Test
    fun enqueueAtCapacityDropsOldestPendingItem() {
        val queue = BoundedSpeechQueue<Int>(2)
        queue.enqueue(1)
        queue.enqueue(2)
        queue.enqueue(3)

        assertEquals(2, queue.size)
        assertEquals(2, queue.dequeue())
        assertEquals(3, queue.dequeue())
    }

    @Test
    fun clearDiscardsAllPendingItems() {
        val queue = BoundedSpeechQueue<String>(2)
        queue.enqueue("pending")
        queue.clear()

        assertEquals(0, queue.size)
        assertNull(queue.dequeue())
    }

    @Test(expected = IllegalArgumentException::class)
    fun capacityMustBePositive() {
        BoundedSpeechQueue<String>(0)
    }
}
