package rj.dfmnext.danmaku.model

interface IDanmakus : Iterable<BaseDanmaku> {
    fun addItem(item: BaseDanmaku): Boolean
    fun removeItem(item: BaseDanmaku): Boolean
    fun subnew(startTime: Long, endTime: Long): IDanmakus
    fun sub(startTime: Long, endTime: Long): IDanmakus
    fun size(): Int
    fun clear()
    fun first(): BaseDanmaku?
    fun last(): BaseDanmaku?
    override fun iterator(): MutableIterator<BaseDanmaku>
    fun contains(item: BaseDanmaku): Boolean
    fun isEmpty(): Boolean
    fun setSubItemsDuplicateMergingEnabled(enable: Boolean)

    // Operator overloads with default implementations
    operator fun plusAssign(item: BaseDanmaku) { addItem(item) }
    operator fun minusAssign(item: BaseDanmaku) { removeItem(item) }
}
