package shared_code

@JvmInline
value class RegClass(val name: String) {
    val schema get() = name.substringBefore('.')
    val table get() = name.substringAfter('.')

    init {
        require(name.count { it == '.' } == 1) { "RegClass name must be of format schema.table" }
    }

    override fun toString(): String = name

    companion object {
        fun of(name: String) = if ('.' !in name) RegClass("public.$name") else RegClass(name)
    }
}
