package io.github.mattidragon.extendeddrawers.client.config;

import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.utils.Dimension;
import dev.isxander.yacl3.gui.AbstractWidget;
import dev.isxander.yacl3.gui.YACLScreen;
import dev.isxander.yacl3.gui.controllers.string.IStringController;
import dev.isxander.yacl3.gui.controllers.string.StringControllerElement;
import net.minecraft.util.Identifier;

public record IdentifierController(Option<Identifier> option) implements IStringController<Identifier> {
    @Override
    public String getString() {
        return option.pendingValue().toString();
    }

    @Override
    public void setFromString(String value) {
        option.requestSet(Identifier.tryParse(value));
    }

    @Override
    public boolean isInputValid(String input) {
        return Identifier.isValid(input);
    }

    @Override
    public AbstractWidget provideWidget(YACLScreen screen, Dimension<Integer> widgetDimension) {
        return new StringControllerElement(this, screen, widgetDimension, false);
    }
}
