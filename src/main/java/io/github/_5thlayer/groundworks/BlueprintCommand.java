// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * {@code /groundworks blueprint}, an operator's (permission level 2) recovery of Cuts whose sheets
 * were lost (ADR 0014): alone, it lists every Cut not yet pasted, with its owner, its size and how
 * long since it was last held; {@code give <id>} gives the operator a new sheet naming one. The
 * Cut is pasted once, by whichever of its sheets is pasted first.
 */
final class BlueprintCommand {

    private static final String GIVE = "/groundworks blueprint give ";

    private BlueprintCommand() {
    }

    static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal(Groundworks.MOD_ID)
                .then(Commands.literal("blueprint")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .executes(context -> list(context.getSource()))
                        .then(Commands.literal("give")
                                .then(Commands.argument("id", UuidArgument.uuid())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                                cuts(context.getSource().getServer()).stream()
                                                        .map(cut -> cut.getKey().toString()), builder))
                                        .executes(context -> give(context.getSource(),
                                                UuidArgument.getUuid(context, "id")))))));
    }

    /** The world's Cuts not yet pasted, the longest unused first. */
    private static List<Map.Entry<UUID, Blueprint>> cuts(MinecraftServer server) {
        return BlueprintStore.of(server).all().entrySet().stream()
                .filter(entry -> entry.getValue().isCut())
                .sorted(Comparator.comparingLong(entry -> entry.getValue().lastUsed()))
                .toList();
    }

    private static int list(CommandSourceStack source) {
        MinecraftServer server = source.getServer();
        List<Map.Entry<UUID, Blueprint>> cuts = cuts(server);
        if (cuts.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("command.groundworks.blueprint.none"), false);
            return 0;
        }
        long now = BlueprintPrune.now();
        source.sendSuccess(() -> Component.translatable("command.groundworks.blueprint.cuts", cuts.size()), false);
        for (Map.Entry<UUID, Blueprint> cut : cuts) {
            Blueprint blueprint = cut.getValue();
            Vec3i size = blueprint.blocks().size();
            Component id = Component.literal(cut.getKey().toString()).withStyle(style -> style
                    .withUnderlined(true)
                    .withClickEvent(new ClickEvent.SuggestCommand(GIVE + cut.getKey()))
                    .withHoverEvent(new HoverEvent.ShowText(
                            Component.translatable("command.groundworks.blueprint.give_hint"))));
            source.sendSuccess(() -> Component.translatable("command.groundworks.blueprint.cut", id,
                    ownerOf(server, blueprint.owner()),
                    size.getX() + "×" + size.getY() + "×" + size.getZ(),
                    age(Duration.ofMillis(Math.max(0, now - blueprint.lastUsed())))), false);
        }
        return cuts.size();
    }

    private static int give(CommandSourceStack source, UUID id) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        Blueprint blueprint = BlueprintStore.of(source.getServer()).get(id);
        if (blueprint == null || !blueprint.isCut()) {
            source.sendFailure(Component.translatable("command.groundworks.blueprint.no_such_cut", id.toString()));
            return 0;
        }
        ItemStack sheet = Blueprints.recovered(player.level(), id);
        if (sheet == null) {
            source.sendFailure(Component.translatable("command.groundworks.blueprint.no_sheet"));
            return 0;
        }
        player.getInventory().placeItemBackInInventory(sheet);
        source.sendSuccess(() -> Component.translatable("command.groundworks.blueprint.given", id.toString(),
                player.getDisplayName()), true);
        return 1;
    }

    /** The owner's name, where the server knows it, else their id. */
    private static String ownerOf(MinecraftServer server, UUID owner) {
        ServerPlayer online = server.getPlayerList().getPlayer(owner);
        if (online != null) {
            return online.getGameProfile().name();
        }
        return server.services() == null ? owner.toString()
                : server.services().nameToIdCache().get(owner).map(NameAndId::name).orElse(owner.toString());
    }

    /** How long, in the largest whole unit of days, hours and minutes. */
    private static Component age(Duration age) {
        if (age.toDays() > 0) {
            return Component.translatable("command.groundworks.blueprint.days", age.toDays());
        }
        if (age.toHours() > 0) {
            return Component.translatable("command.groundworks.blueprint.hours", age.toHours());
        }
        return Component.translatable("command.groundworks.blueprint.minutes", age.toMinutes());
    }
}
