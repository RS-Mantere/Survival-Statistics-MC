package dev.roope.survivalstats.display;

import net.minecraft.network.chat.Component;

public record RenderedTabList(Component header, Component footer, String cacheKey) {
}
