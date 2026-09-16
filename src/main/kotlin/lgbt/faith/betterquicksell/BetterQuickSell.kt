package lgbt.faith.betterquicksell

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.Element
import net.minecraft.client.gui.ParentElement
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.client.gui.widget.PressableWidget
import net.minecraft.client.input.MouseInput
import net.minecraft.client.option.KeyBinding
import net.minecraft.client.util.InputUtil
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import org.lwjgl.glfw.GLFW
import org.slf4j.LoggerFactory
import kotlin.concurrent.thread
import kotlin.random.Random

class BetterQuickSell : ClientModInitializer {

    private val logger = LoggerFactory.getLogger("screenlogger")

    private val sellConfirmText = "Are you sure you want to sell this?"

    private var enabled = true

    override fun onInitializeClient() {
        WhitelistConfig.load()

        val category = KeyBinding.Category.create(Identifier.of("betterquicksell", "main"))

        val whitelistKey = KeyBindingHelper.registerKeyBinding(
            KeyBinding("key.betterquicksell.whitelist", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_K, category)
        )

        val toggleKey = KeyBindingHelper.registerKeyBinding(
            KeyBinding("key.betterquicksell.toggle", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_J, category)
        )

        ClientTickEvents.END_CLIENT_TICK.register { client ->
            while (whitelistKey.wasPressed()) {
                client.setScreen(WhitelistScreen(client.currentScreen))
            }
            while (toggleKey.wasPressed()) {
                enabled = !enabled
                client.player?.sendMessage(
                    Text.translatable(if (enabled) "betterquicksell.toggle.on" else "betterquicksell.toggle.off"),
                    true
                )
            }
        }

        ScreenEvents.AFTER_INIT.register { _, screen, _, _ ->
            logger.info("screen opened ${screen::class.simpleName}")

            if (enabled && (screen::class.simpleName == "MultiActionDialogScreen" || screen::class.simpleName == "class_11478")) {
                val widgets = collectClickableWidgets(screen)
                widgets.forEach { logger.info("screen text: ${it.message.string}") }
                Thread.sleep(50)

                val hasSellConfirm = widgets.any { it.message.string.contains(sellConfirmText) }
                println(hasSellConfirm)
                val heldStack = MinecraftClient.getInstance().player?.mainHandStack
                if (hasSellConfirm && (heldStack == null || !WhitelistConfig.allows(heldStack))) {
                    logger.info("sell confirm detected but held item isn't whitelisted, not confirming")
                } else if (hasSellConfirm) {
                    val yesButton = widgets.filterIsInstance<PressableWidget>()
                        .firstOrNull { it.message.string.trim().equals("yes", ignoreCase = true) }

                    if (yesButton != null) {
                        thread(isDaemon = true) {
                            MinecraftClient.getInstance().execute {
                                yesButton.onPress(MouseInput(0, 0))
                            }
                        }
                    } else {
                        logger.warn("sell confirm detected but no 'Yes' button found")
                    }
                }
            }


            ScreenEvents.remove(screen).register { closed ->
                logger.info("screen closed ${closed::class.simpleName}")
            }
        }
    }

    private fun collectClickableWidgets(screen: Screen): List<ClickableWidget> {
        val widgets = mutableListOf<ClickableWidget>()

        fun visit(element: Element) {
            if (element is ClickableWidget) {
                widgets += element
            }
            if (element is ParentElement) {
                element.children().forEach(::visit)
            }
        }

        screen.children().forEach(::visit)
        return widgets
    }
}
