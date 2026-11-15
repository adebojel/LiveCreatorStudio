package id.livecreator.studio

/**
 * Small bounded FIFO queue for background speech requests.
 * When full, the oldest pending item is discarded so the newest request is retained.
 */
internal class BoundedSpeechQueue<T>(private val capacity: Int) {
    private val items = ArrayDeque<T>()

    init {
        require(capacity > 0) { "Queue capacity must be greater than zero." }
    }

    val size: Int
        get() = items.size

    val isEmpty: Boolean
        get() = items.isEmpty()

    fun enqueue(item: T) {
        if (items.size >= capacity) items.removeFirst()
        items.addLast(item)
    }

    fun dequeue(): T? = if (items.isEmpty()) null else items.removeFirst()

    fun clear() {
        items.clear()
    }
}
