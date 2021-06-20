package xyz.starmun.justenoughkeys.common.gui;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Option;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.TooltipAccessor;
import net.minecraft.client.gui.screens.MouseSettingsScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.controls.ControlsScreen;
import net.minecraft.network.chat.*;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import xyz.starmun.justenoughkeys.common.contracts.IJEKControlScreenExtensions;
import xyz.starmun.justenoughkeys.common.contracts.IJEKKeyMappingExtensions;
import xyz.starmun.justenoughkeys.common.data.ModifierKey;
import xyz.starmun.justenoughkeys.common.data.ModifierKeyMap;

import java.util.Arrays;
import java.util.Locale;
import java.util.Random;

public class JEKControlScreen extends ControlsScreen {

    private Button resetButton;
    private EditBox search;
    private String searchQuery="";
    private BaseComponent toolTipComponent;
    public JEKControlScreen(Screen screen, Options options) {
        super(screen, options);
    }

    protected void init() {
        ((IJEKControlScreenExtensions) this).jek$setControlList(new JEKControlList(this, Minecraft.getInstance()));
        this.children.add(((IJEKControlScreenExtensions) this).jek$getControlList());
        this.setFocused(((IJEKControlScreenExtensions) this).jek$getControlList());
        initWidgets();
    }

    @Override
    public boolean keyPressed(int keyValue, int scanCode, int modifiers) {
        if (keyValue == GLFW.GLFW_KEY_ESCAPE && search.isFocused()) {
            search.setFocus(false);
            return true;
        }
        if (selectedKey == null) {
            search.keyPressed(keyValue, scanCode, modifiers);
            return super.keyPressed(keyValue, scanCode, modifiers);
        }
        if (keyValue == GLFW.GLFW_KEY_ESCAPE) {
            ((IJEKKeyMappingExtensions) selectedKey).jek$getModifierKeyMap().clear();

        } else if (selectedKey.isUnbound()) {
            options.setKey(selectedKey, InputConstants.getKey(keyValue, scanCode));
        } else {
            int currentModifierKeyValue = ((IJEKKeyMappingExtensions) selectedKey).jek$getKey().getValue();
            ModifierKey currentModifierKey = ModifierKey.modifierKeyFromValue(currentModifierKeyValue);
            ModifierKey currentKey = ModifierKey.modifierKeyFromValue(keyValue);
            if (currentModifierKey != ModifierKey.UNKNOWN && currentKey == ModifierKey.UNKNOWN) {
                ((IJEKKeyMappingExtensions) selectedKey).jek$getModifierKeyMap().set(currentModifierKey, true);
                options.setKey(selectedKey, InputConstants.getKey(keyValue, scanCode));
            } else {
                ((IJEKKeyMappingExtensions) selectedKey).jek$getModifierKeyMap().set(currentKey, true);
                ((IJEKKeyMappingExtensions) selectedKey).jek$getModifierKeyMap().clear(selectedKey);
            }
        }
        lastKeySelection = Util.getMillis();
        IJEKKeyMappingExtensions.resetMapping();
        return true;
    }

    @Override
    public boolean keyReleased(int keyValue, int scanCode, int modifiers) {
        this.selectedKey = null;
        this.lastKeySelection = Util.getMillis();
        return super.keyReleased(keyValue, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (search.isMouseOver(mouseX,mouseY) && button == 1) {
            search.setValue("");
        }
        else if(search.mouseClicked(mouseX,mouseY,button)){
            return true;
        }
        if (selectedKey == null) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        InputConstants.Key key = ((IJEKKeyMappingExtensions) selectedKey).jek$getKey();
        ModifierKeyMap modifierKeyMap = ((IJEKKeyMappingExtensions) selectedKey).jek$getModifierKeyMap();
        modifierKeyMap.set(key, true);
        boolean result = super.mouseClicked(mouseX, mouseY, button);
        IJEKKeyMappingExtensions.resetMapping();
        return result;
    }

    @Override
    public void render(PoseStack poseStack, int i, int j, float f) {
        this.renderBackground(poseStack);
        ((IJEKControlScreenExtensions) this).jek$getControlList().render(poseStack, i, j, f);
        drawCenteredString(poseStack, this.font, this.title.getString(), this.width / 2, 8, 16777215);
        for (AbstractWidget button : buttons) {
            button.render(poseStack, i, j, f);
        }
        this.resetButton.active = Arrays.stream(this.options.keyMappings).anyMatch(keyMapping -> !keyMapping.isDefault());
        this.search.render(poseStack, i, j, f);
        if(search.isMouseOver(i,j) && ! search.isFocused()){
            renderTooltip(poseStack,this.toolTipComponent,i,j);
        }
        font.draw(poseStack,new TranslatableComponent("jek.controls.search.label"),this.width / 2 - 153, this.height-45,  16777215);

    }
    //Mostly copied over from the parent class
    private void initWidgets() {
        int labelWidth = Minecraft.getInstance().font.width(new TranslatableComponent("jek.controls.search.label"));
        this.search = new EditBox(this.font, this.width / 2 - 153 + labelWidth + 5, this.height - 50, 300  - labelWidth, 18, new TextComponent(""));
        this.search.setMaxLength(200);
        this.addButton(new Button(this.width / 2 - 155, 18, 150, 20, new TranslatableComponent("options.mouse_settings"), (button) -> {
            assert this.minecraft != null;
            this.minecraft.setScreen(new MouseSettingsScreen(this, this.options));
        }));
        this.addButton(Option.AUTO_JUMP.createButton(this.options, this.width / 2 - 155 + 160, 18, 150));
        this.resetButton = this.addButton(new Button(this.width / 2 - 155, this.height - 29, 150, 20, new TranslatableComponent("controls.resetAll"), (button) -> {
            for (KeyMapping keyMapping : this.options.keyMappings) {
                keyMapping.setKey(keyMapping.getDefaultKey());
                ((IJEKKeyMappingExtensions) keyMapping).jek$getModifierKeyMap().clear();
            }
            IJEKKeyMappingExtensions.resetMapping();
        }));
        this.addButton(new Button(this.width / 2 - 155 + 160, this.height - 29, 150, 20, CommonComponents.GUI_DONE, (button) -> {
            assert this.minecraft != null;
            this.minecraft.setScreen(this.lastScreen);
        }));
        this.toolTipComponent = new TextComponent("");
        toolTipComponent.append(new TranslatableComponent("jek.controls.search.tooltip.title").withStyle(style -> style.withBold(true).withColor(ChatFormatting.DARK_AQUA)));
        toolTipComponent.append(System.lineSeparator());
        toolTipComponent.append(new TranslatableComponent("jek.controls.search.tooltip.description"));
        toolTipComponent.append(System.lineSeparator());
        toolTipComponent.append(new TranslatableComponent("jek.controls.search.tooltip.advanced.title").withStyle(ChatFormatting.GRAY));
        toolTipComponent.append(System.lineSeparator());
        toolTipComponent.append(new TranslatableComponent("jek.controls.search.tooltip.advanced.description"));
        toolTipComponent.append(System.lineSeparator());
        toolTipComponent.append(new TranslatableComponent("jek.controls.search.tooltip.advanced.filters.title").withStyle(ChatFormatting.DARK_GRAY));
        toolTipComponent.append(System.lineSeparator());
        toolTipComponent.append(new TranslatableComponent("jek.controls.search.tooltip.advanced.filters.description"));
        toolTipComponent.append(System.lineSeparator());
        toolTipComponent.append(new TranslatableComponent("jek.controls.search.tooltip.advanced.searchby.title").withStyle(ChatFormatting.DARK_GRAY));
        toolTipComponent.append(System.lineSeparator());
        toolTipComponent.append(new TranslatableComponent("jek.controls.search.tooltip.advanced.searchby.description"));
        toolTipComponent.append(System.lineSeparator());
        toolTipComponent.append(System.lineSeparator());
        toolTipComponent.append(new TranslatableComponent("jek.controls.search.tooltip.advanced.example.prefix").withStyle(ChatFormatting.DARK_GRAY));
        toolTipComponent.append(new TranslatableComponent("jek.controls.search.tooltip.advanced.example.query").withStyle(ChatFormatting.GOLD));
        toolTipComponent.append(new TranslatableComponent("jek.controls.search.tooltip.advanced.example.suffix"));

    }

    @Override
    public boolean charTyped(char var1, int var2) {
        return search.charTyped(var1, var2);
    }

    @Override
    public void tick() {
        this.search.tick();
        if(!this.searchQuery.equals(search.getValue())){
            this.searchQuery = search.getValue();
            ((JEKControlList)((IJEKControlScreenExtensions)this).jek$getControlList()).search(this.searchQuery.toLowerCase(Locale.ROOT));
        }
    }
}
