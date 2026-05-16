package dev.beryl.lattice.paper.text;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;

public final class PaperAudiences {
    private PaperAudiences() {
    }

    public static Audience audience(CommandSender sender) {
        return sender;
    }

    public static void send(Audience audience, Component component) {
        audience.sendMessage(component);
    }
}

