package net.AnimalPlus;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static net.AnimalPlus.AnimalPlusMain.MOD_ID;


public class ModCommand {
    private static Set<String> AGEABLE_MOBS = new HashSet<>();
    private ConfigManager configManager;
    private MobManager mobManager;

    public ModCommand(ConfigManager configManager, MobManager mobManager) {
        this.configManager = configManager;
        this.mobManager = mobManager;
    }


    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        // mob argument
        RequiredArgumentBuilder<CommandSourceStack, String> mobArg =
                Commands.argument(CommandList.SubArgs.ANIMAL.toString(), StringArgumentType.word())
                        .suggests(this::mobSuggestions);

        // attach all commands
        mobArg.then(registerReset());
        mobArg.then(registerChange());
        mobArg.then(registerCheck());

        // register root
        event.getDispatcher().register(
                Commands.literal(MOD_ID).then(mobArg)
        );
    }


    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        ServerLevel level = event.getServer().overworld();
        getAllAgeableMobNamesString(level);
        Set<String> updated = AGEABLE_MOBS.stream()
                .map(mob -> {
                    int idx = mob.lastIndexOf(':');
                    return (idx != -1) ? mob.substring(idx + 1) : mob;
                })
                .collect(Collectors.toSet());
        AGEABLE_MOBS.clear();
        AGEABLE_MOBS.addAll(updated);

    }

    /**
     * Helper method to check which entity is AgeableMob
     *
     * @param level server level
     */
    private void getAllAgeableMobNamesString(Level level) {
        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
            if (id == null) continue;
            try {
                Entity entity = type.create(level); // Pass a valid Level, can use a dummy/server level
                if (entity instanceof AgeableMob) {
                    AGEABLE_MOBS.add(id.toString());
                }
            } catch (Exception e) {
                // Entity could not be instantiated (likely abstract, etc.)
            }
        }
    }

    private  LiteralArgumentBuilder<CommandSourceStack> registerCheck() {
        return Commands.literal(CommandList.check.toString())
                .executes(this::handleCommand);
    }

    private  LiteralArgumentBuilder<CommandSourceStack> registerReset() {
        return Commands.literal(CommandList.reset.toString())
                .then(registerAge(CommandList.reset))
                .then(registerBreed(CommandList.reset));
    }

    private  LiteralArgumentBuilder<CommandSourceStack> registerChange() {
        return Commands.literal(CommandList.change.toString())
                .then(registerAge(CommandList.change))
                .then(registerBreed(CommandList.change))
                .executes(ctx -> {
                    ctx.getSource().sendFailure(
                            Component.literal("Usage: /mobagetweak <mob> change <age|breed> <seconds>")
                    );
                    return 0;
                });
    }

    private LiteralArgumentBuilder<CommandSourceStack> registerAge(CommandList commandList) {
        if (commandList.equals(CommandList.reset)) {
            return Commands.literal(CommandList.SubArgs.AGE.toString())
                    .executes(this::handleCommand);
        } else {
            return Commands.literal(CommandList.SubArgs.AGE.toString())
                    .then(Commands.argument(CommandList.SubArgs.SECONDS.toString(), IntegerArgumentType.integer(1, 1200))
                            .executes(this::handleCommand))
                    .executes(ctx -> {
                        ctx.getSource().sendFailure(
                                Component.literal("You must provide seconds for age. Usage: /mobagetweak <mob> change age <1-1200>")
                        );
                        return 0;
                    });
        }
    }

    private  LiteralArgumentBuilder<CommandSourceStack> registerBreed(CommandList commandList) {
        if (commandList.equals(CommandList.reset)) {
            return Commands.literal(CommandList.SubArgs.BREED.toString())
                    .executes(this::handleCommand);
        } else {
            return Commands.literal(CommandList.SubArgs.BREED.toString())
                    .then(Commands.argument(CommandList.SubArgs.SECONDS.toString(), IntegerArgumentType.integer(1, 600))
                            .executes(this::handleCommand))
                    .executes(ctx -> {
                        ctx.getSource().sendFailure(
                                Component.literal("You must provide seconds for breed. Usage: /mobagetweak <mob> change breed <1-600>")
                        );
                        return 0;
                    });
        }
    }


    private CompletableFuture<Suggestions> mobSuggestions(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        Level level = context.getSource().getLevel();
        for (String mob : AGEABLE_MOBS) {
            int idx = mob.indexOf(":");
            builder.suggest(mob);
        }
        return builder.buildFuture();
    }


    private int handleCommand(CommandContext<CommandSourceStack> context) {
        // Check if the game is single or multiplayer
        MinecraftServer server = context.getSource().getServer();
        boolean isSingleplayer = server.isSingleplayer();
        if (!isSingleplayer) {
            CommandSourceStack source = context.getSource();
            if (!source.hasPermission(2)) {
                source.sendFailure(Component.literal("You do not have permission to execute this command."));
                return Command.SINGLE_SUCCESS;
            }
        }

        // Get mob argument
        String mobName = StringArgumentType.getString(context, CommandList.SubArgs.ANIMAL.toString());

        if (!AGEABLE_MOBS.contains(mobName)) {
            String msg = String.format("Error: %s not found", mobName);
            context.getSource().sendFailure(Component.literal(msg));
            return Command.SINGLE_SUCCESS;
        }

        MobSettings mobSettings = new MobSettings(mobName, MobSettings.AGE_DEFAULT_CD, MobSettings.BREED_DEFAULT_CD); // By default, make it 1200,6000
        return mobManager.handleCommands(context, mobSettings);
    }

}