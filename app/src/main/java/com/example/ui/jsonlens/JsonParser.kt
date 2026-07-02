package com.example.ui.jsonlens

import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener

sealed class JsonTreeNode {
    abstract val label: String // key name or array index
    abstract val path: String
    abstract val depth: Int

    data class ObjectNode(
        override val label: String,
        override val path: String,
        override val depth: Int,
        val children: List<JsonTreeNode>,
        val size: Int
    ) : JsonTreeNode()

    data class ArrayNode(
        override val label: String,
        override val path: String,
        override val depth: Int,
        val children: List<JsonTreeNode>,
        val size: Int
    ) : JsonTreeNode()

    data class LeafNode(
        override val label: String,
        override val path: String,
        override val depth: Int,
        val type: ValueType,
        val valueString: String
    ) : JsonTreeNode()
}

enum class ValueType {
    STRING, NUMBER, BOOLEAN, NULL
}

object JsonParser {

    fun parse(jsonStr: String): JsonTreeNode {
        val trimmed = jsonStr.trim()
        val tokener = JSONTokener(trimmed)
        val rootVal = tokener.nextValue()

        return when (rootVal) {
            is JSONObject -> parseObject("root", "", 0, rootVal)
            is JSONArray -> parseArray("root", "", 0, rootVal)
            else -> JsonTreeNode.LeafNode("root", "root", 0, getLeafType(rootVal), rootVal.toString())
        }
    }

    private fun parseObject(label: String, parentPath: String, depth: Int, obj: JSONObject): JsonTreeNode.ObjectNode {
        val currentPath = if (parentPath.isEmpty()) label else "$parentPath.$label"
        val keys = obj.keys().asSequence().sorted().toList()
        val children = keys.map { key ->
            val value = obj.get(key)
            val cleanParentPath = if (parentPath.isEmpty()) "" else parentPath
            val newParentPath = if (cleanParentPath.isEmpty()) label else "$cleanParentPath.$label"

            when (value) {
                is JSONObject -> parseObject(key, newParentPath, depth + 1, value)
                is JSONArray -> parseArray(key, newParentPath, depth + 1, value)
                else -> JsonTreeNode.LeafNode(key, if (newParentPath.isEmpty()) key else "$newParentPath.$key", depth + 1, getLeafType(value), value.toString())
            }
        }
        return JsonTreeNode.ObjectNode(label, currentPath, depth, children, children.size)
    }

    private fun parseArray(label: String, parentPath: String, depth: Int, arr: JSONArray): JsonTreeNode.ArrayNode {
        val currentPath = if (parentPath.isEmpty()) label else "$parentPath.$label"
        val children = (0 until arr.length()).map { idx ->
            val value = arr.get(idx)
            val cleanParentPath = if (parentPath.isEmpty()) "" else parentPath
            val newParentPath = if (cleanParentPath.isEmpty()) label else "$cleanParentPath.$label"
            val keyString = "[$idx]"

            when (value) {
                is JSONObject -> parseObject(keyString, newParentPath, depth + 1, value)
                is JSONArray -> parseArray(keyString, newParentPath, depth + 1, value)
                else -> JsonTreeNode.LeafNode(keyString, "$newParentPath$keyString", depth + 1, getLeafType(value), value.toString())
            }
        }
        return JsonTreeNode.ArrayNode(label, currentPath, depth, children, children.size)
    }

    private fun getLeafType(value: Any?): ValueType {
        if (value == null || value == JSONObject.NULL) return ValueType.NULL
        return when (value) {
            is Number -> ValueType.NUMBER
            is Boolean -> ValueType.BOOLEAN
            else -> ValueType.STRING
        }
    }

    // Helper to calculate statistics
    fun calculateStats(node: JsonTreeNode, sourceSize: Int): JsonStats {
        var keysCount = 0
        var maxDepth = 0

        fun traverse(n: JsonTreeNode) {
            keysCount++
            if (n.depth > maxDepth) maxDepth = n.depth
            when (n) {
                is JsonTreeNode.ObjectNode -> n.children.forEach { traverse(it) }
                is JsonTreeNode.ArrayNode -> n.children.forEach { traverse(it) }
                else -> {}
            }
        }

        traverse(node)
        return JsonStats(
            keysCount = keysCount,
            maxDepth = maxDepth,
            sizeKb = sourceSize.toDouble() / 1024.0
        )
    }
}

data class JsonStats(
    val keysCount: Int,
    val maxDepth: Int,
    val sizeKb: Double
)
