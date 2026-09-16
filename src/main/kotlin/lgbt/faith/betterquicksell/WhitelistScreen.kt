package lgbt.faith.betterquicksell

import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.Element
import net.minecraft.client.gui.Selectable
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.tooltip.Tooltip
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.client.gui.widget.DirectionalLayoutWidget
import net.minecraft.client.gui.widget.ElementListWidget
import net.minecraft.client.gui.widget.TextFieldWidget
import net.minecraft.client.gui.widget.ThreePartsLayoutWidget
import net.minecraft.item.ItemStack
import net.minecraft.registry.Registries
import net.minecraft.screen.ScreenTexts
import net.minecraft.text.Text
import net.minecraft.util.Colors

class WhitelistScreen(private val parent: Screen?) : Screen(Text.translatable("betterquicksell.whitelist.title")) {

    private val layout = ThreePartsLayoutWidget(this, ThreePartsLayoutWidget.DEFAULT_HEADER_FOOTER_HEIGHT, 60)
    private lateinit var list: WhitelistListWidget
    private lateinit var itemField: TextFieldWidget
    private lateinit var countField: TextFieldWidget
    private lateinit var addButton: ButtonWidget

    override fun init() {
        layout.addHeader(title, textRenderer)
        list = layout.addBody(WhitelistListWidget(client!!))

        val footer = layout.addFooter(DirectionalLayoutWidget.vertical().spacing(4))

        val inputRow = footer.add(DirectionalLayoutWidget.horizontal().spacing(4))
        itemField = inputRow.add(TextFieldWidget(textRenderer, 200, 20, Text.translatable("betterquicksell.whitelist.item")))
        itemField.setMaxLength(128)
        itemField.setPlaceholder(Text.translatable("betterquicksell.whitelist.item.placeholder"))
        itemField.setChangedListener { updateInputs() }

        countField = inputRow.add(TextFieldWidget(textRenderer, 40, 20, Text.translatable("betterquicksell.whitelist.count")))
        countField.setMaxLength(2)
        countField.setTextPredicate { it.all(Char::isDigit) }
        countField.setPlaceholder(Text.literal("64"))
        countField.setChangedListener { updateInputs() }

        addButton = inputRow.add(ButtonWidget.builder(Text.translatable("betterquicksell.whitelist.add")) { addEntry() }.width(60).build())

        val buttonRow = footer.add(DirectionalLayoutWidget.horizontal().spacing(8))
        buttonRow.add(ButtonWidget.builder(Text.translatable("betterquicksell.whitelist.held")) { fillFromHeld() }
            .tooltip(Tooltip.of(Text.translatable("betterquicksell.whitelist.held.tooltip")))
            .build())
        buttonRow.add(ButtonWidget.builder(ScreenTexts.DONE) { close() }.build())

        layout.forEachChild { addDrawableChild(it) }
        refreshWidgetPositions()
        updateInputs()
    }

    override fun refreshWidgetPositions() {
        layout.refreshPositions()
        list.position(width, layout)
    }

    override fun setInitialFocus() {
        setInitialFocus(itemField)
    }

    override fun close() {
        client!!.setScreen(parent)
    }

    private fun parsedCount(): Int? = countField.text.ifEmpty { "64" }.toIntOrNull()?.takeIf { it in 1..99 }

    private fun updateInputs() {
        val validItem = WhitelistEntry.parseItem(itemField.text).let { it != null && it != net.minecraft.item.Items.AIR }
        itemField.setEditableColor(if (validItem || itemField.text.isEmpty()) TextFieldWidget.DEFAULT_EDITABLE_COLOR else Colors.RED)
        countField.setEditableColor(if (parsedCount() != null || countField.text.isEmpty()) TextFieldWidget.DEFAULT_EDITABLE_COLOR else Colors.RED)
        addButton.active = validItem && parsedCount() != null
    }

    private fun fillFromHeld() {
        val stack = client!!.player?.mainHandStack ?: return
        if (stack.isEmpty) return
        itemField.text = Registries.ITEM.getId(stack.item).toString()
        countField.text = stack.count.toString()
    }

    private fun addEntry() {
        val item = WhitelistEntry.parseItem(itemField.text) ?: return
        val count = parsedCount() ?: return
        val entry = WhitelistEntry(Registries.ITEM.getId(item).toString(), count)
        if (entry !in WhitelistConfig.entries) {
            WhitelistConfig.entries += entry
            WhitelistConfig.save()
            list.refresh()
        }
        itemField.text = ""
        countField.text = ""
        setFocused(itemField)
    }

    private inner class WhitelistListWidget(client: MinecraftClient) :
        ElementListWidget<WhitelistListWidget.Entry>(client, width, layout.contentHeight, layout.headerHeight, 24) {

        init {
            refresh()
        }

        fun refresh() {
            clearEntries()
            WhitelistConfig.entries.forEach { addEntry(Entry(it)) }
        }

        override fun getRowWidth(): Int = 500

        inner class Entry(private val entry: WhitelistEntry) : ElementListWidget.Entry<Entry>() {
            private val stack = ItemStack(entry.resolveItem() ?: net.minecraft.item.Items.BARRIER, entry.count)

            private val removeButton = ButtonWidget.builder(Text.translatable("betterquicksell.whitelist.remove")) {
                WhitelistConfig.entries.remove(entry)
                WhitelistConfig.save()
                refresh()
            }.width(60).build()

            override fun render(context: DrawContext, mouseX: Int, mouseY: Int, hovered: Boolean, deltaTicks: Float) {
                val x = contentX
                val y = contentMiddleY
                context.drawItem(stack, x, y - 8)
                context.drawStackOverlay(textRenderer, stack, x, y - 8, entry.count.toString())
                context.drawTextWithShadow(textRenderer, stack.name, x + 24, y - 9, Colors.WHITE)
                context.drawTextWithShadow(textRenderer, entry.item, x + 24, y + 1, Colors.GRAY)

                removeButton.setPosition(contentRightEnd - removeButton.width, y - removeButton.height / 2)
                removeButton.render(context, mouseX, mouseY, deltaTicks)
            }

            override fun children(): List<Element> = listOf(removeButton)

            override fun selectableChildren(): List<Selectable> = listOf(removeButton)
        }
    }
}
