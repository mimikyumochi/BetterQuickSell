package lgbt.faith.betterquicksell

import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier
import org.slf4j.LoggerFactory
import java.nio.file.Files

data class WhitelistEntry(val item: String, val count: Int) {
    fun resolveItem(): Item? = parseItem(item)

    companion object {
        fun parseItem(id: String): Item? {
            val identifier = Identifier.tryParse(id.trim()) ?: return null
            if (!Registries.ITEM.containsId(identifier)) return null
            return Registries.ITEM.getOptionalValue(identifier).orElse(null)
        }
    }
}

object WhitelistConfig {
    private val logger = LoggerFactory.getLogger("betterquicksell")
    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val path = FabricLoader.getInstance().configDir.resolve("betterquicksell.json")

    val entries = mutableListOf<WhitelistEntry>()

    fun load() {
        entries.clear()
        if (!Files.exists(path)) return
        try {
            val type = object : TypeToken<List<WhitelistEntry>>() {}.type
            val loaded: List<WhitelistEntry>? = Files.newBufferedReader(path).use { gson.fromJson(it, type) }
            loaded?.filter { it.resolveItem() != null && it.count > 0 }?.let(entries::addAll)
        } catch (e: Exception) {
            logger.error("failed to load whitelist", e)
        }
    }

    fun save() {
        try {
            Files.createDirectories(path.parent)
            Files.newBufferedWriter(path).use { gson.toJson(entries, it) }
        } catch (e: Exception) {
            logger.error("failed to save whitelist", e)
        }
    }

    /** The held stack is allowed if its item is whitelisted and its count doesn't exceed the whitelisted stack size. */
    fun allows(stack: ItemStack): Boolean {
        if (stack.isEmpty) return false
        val id = Registries.ITEM.getId(stack.item).toString()
        return entries.any { it.item == id && stack.count <= it.count }
    }
}
