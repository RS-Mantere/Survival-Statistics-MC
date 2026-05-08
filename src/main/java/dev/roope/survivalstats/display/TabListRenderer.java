package dev.roope.survivalstats.display;

import dev.roope.survivalstats.Config;
import dev.roope.survivalstats.StatDef;
import dev.roope.survivalstats.StatSnapshot;
import net.minecraft.network.chat.Component;

public final class TabListRenderer {
    public RenderedTabList render(Config config, StatSnapshot snapshot) {
        Component header = Component.literal("§6§lSurvival Stats");
        StringBuilder footerText = new StringBuilder();
        for (StatDef def : StatDef.USER_STATS) {
            if (!footerText.isEmpty()) {
                footerText.append('\n');
            }
            footerText.append(StatFormatter.formatRow(def, snapshot, config.distanceUnit()));
        }
        String footerString = footerText.toString();
        Component footer = Component.literal(footerString);
        String key = header.getString() + "\n" + footerString;
        return new RenderedTabList(header, footer, key);
    }
}
