package net.AnimalPlus;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.BabyEntitySpawnEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.*;


public class MobManager {
    private  final Map<Mob, Integer> DELAYED_MOBS = new HashMap<>();
    private static int TICKLEFT = 20; // For custom change later ? I don't know
    private final int TICKTOMIN = 1200;
    private  ConfigManager configManager;

    public MobManager(ConfigManager configManager) {
        this.configManager = configManager;
    }

    private int getAgeCooldown(String mobName) {
        return configManager.getAgeCooldownOrAdd(mobName);
    }

    private int getBreedCooldown(String mobName) {
        return configManager.getBreedCooldownOrAdd(mobName);
    }

    /**
     * Fired when a baby animal is created through breeding.
     * Registers both parents and the child for delayed cooldown handling.
     *
     * @param event the baby entity spawn event
     */
    @SubscribeEvent
    public void babyMobSpawn(BabyEntitySpawnEvent event) {
        Mob parentA = event.getParentA();
        Mob parentB = event.getParentB();
        AgeableMob child = event.getChild();
        DELAYED_MOBS.put(child, TICKLEFT);
        DELAYED_MOBS.put(parentA, TICKLEFT);
        DELAYED_MOBS.put(parentB, TICKLEFT);

    }

    /**
     * Brutally overrides Minecraft's dumb magic-number system.
     * <p>
     * Mojang hardcoded breeding and age cooldowns (seriously?!),
     * so we hijack the tick loop and patch them after a short delay.
     *
     * @param event server tick event (where we clean up Mojang's mess) not really
     */
    @SubscribeEvent
    public void handleMobDelays(ServerTickEvent.Post event) {
        if (DELAYED_MOBS.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<Mob, Integer>> it = DELAYED_MOBS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Mob, Integer> entry = it.next();
            Mob mob = entry.getKey();
            int ticksLeft = entry.getValue() - 1;
            if (ticksLeft <= 0) {
                if (mob instanceof Animal animal) {
                    String mobName = animal.getName().getString().toLowerCase();
                    if(animal.isBaby()){
                        int ageCD = getAgeCooldown(mobName);
                        animal.setAge(-ageCD * 20);
                    }else{
                        int breedCD = getBreedCooldown(mobName);
                        animal.setAge(breedCD);
                    }

                }
                it.remove();
            } else {
                entry.setValue(ticksLeft);
            }

        }
    }



    /**
     *
     *
     * @param context     command context with caller and arguments
     * @param mobSettings mob settings target
     * @return command success status
     */

    public int handleCommands(CommandContext<CommandSourceStack> context,MobSettings mobSettings) {
        String command = context.getNodes().get(2).getNode().getName();
        CommandList cmd = CommandList.fromString(command);
        String subCommand;
        CommandList.SubArgs subCmd = null;
        int seconds = 0;
        if(!command.equals(CommandList.check.toString())){
            try{
                subCommand = context.getNodes().get(3).getNode().getName(); // <age|breed>
                subCmd = CommandList.SubArgs.fromString(subCommand);
                seconds = IntegerArgumentType.getInteger(context, CommandList.SubArgs.SECONDS.toString());
            }catch(Exception ignored){
                // Reset called no second there or error then tried to read node
            }
        }
        if(cmd != null){
            switch (cmd) {
                case check -> {
                    context.getSource().sendSystemMessage(
                            Component.literal(
                                    String.format(
                                            "Current cooldowns for %s: Age = %d %s, Breed = %d %s",
                                            mobSettings.getName(),
                                            getAgeCooldown(mobSettings.getName()),
                                            CommandList.SubArgs.SECONDS,
                                            getBreedCooldown(mobSettings.getName()),
                                            CommandList.SubArgs.SECONDS
                                    )
                            ).withStyle(ChatFormatting.DARK_GREEN)
                    );
                    return Command.SINGLE_SUCCESS;
                }
                case change -> {
                    configManager.setCoolDown(mobSettings.getName(),subCmd,seconds);
                    if(subCmd.equals(CommandList.SubArgs.AGE)) {
                        context.getSource().sendSystemMessage(Component.literal(
                                String.format("Age cooldown for %s changed to %d seconds", mobSettings.getName(), seconds))
                                .withStyle(ChatFormatting.GREEN));
                        return Command.SINGLE_SUCCESS;
                    }else if (subCmd.equals(CommandList.SubArgs.BREED)) {
                        context.getSource().sendSystemMessage(Component.literal(
                                String.format("Breed cooldown for %s changed to %d seconds", mobSettings.getName(), seconds))
                                .withStyle(ChatFormatting.GREEN));
                        return Command.SINGLE_SUCCESS;
                    }
                    return Command.SINGLE_SUCCESS;
                }
                case reset -> {
                    configManager.setReset(mobSettings.getName(),subCmd);
                    if(subCmd.equals(CommandList.SubArgs.AGE)) {
                        context.getSource().sendSystemMessage(
                                Component.literal(
                                        String.format("Age cooldown for %s has been reset to default (%d mins)",
                                                mobSettings.getName(),
                                                MobSettings.AGE_DEFAULT_CD/TICKTOMIN)
                                ).withStyle(ChatFormatting.DARK_RED)
                        );

                    }
                    if(subCmd.equals(CommandList.SubArgs.BREED)) {
                        context.getSource().sendSystemMessage(
                                Component.literal(
                                        String.format("Breed cooldown for %s has been reset to default (%d mins)",
                                                mobSettings.getName(),
                                                MobSettings.BREED_DEFAULT_CD/TICKTOMIN)
                                ).withStyle(ChatFormatting.DARK_RED)
                        );
                    }
                    return Command.SINGLE_SUCCESS;
                }
                default -> {
                    context.getSource().sendFailure(Component.literal(String.format("Unknown command %s", command)));
                    return Command.SINGLE_SUCCESS;
                }
            }

        }
        return Command.SINGLE_SUCCESS;
    }

    private void printDebugInfo(Mob mob, boolean toggleDebugInfo) {
        if (!toggleDebugInfo) {
            return;
        }
        Animal mobAnimal = (Animal) mob;

        double x = mobAnimal.getX();
        double y = mobAnimal.getY();
        double z = mobAnimal.getZ();

        for (Player player : mobAnimal.level().players()) {
            if (player instanceof ServerPlayer serverPlayer && serverPlayer.hasPermissions(2)) {
                String tpCommand = String.format("/tp %s %.2f %.2f %.2f", serverPlayer.getName().getString(), x, y, z);
                Component tpMsg = Component.literal("[Teleport]")
                        .withStyle(ChatFormatting.AQUA)
                        .withStyle(style -> style
                                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, tpCommand))
                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Click to teleport to this mob!")))
                        );

                Component fullMsg = Component.literal("Debug Info: ")
                        .withStyle(ChatFormatting.YELLOW)
                        .append(Component.literal("Baby " + mobAnimal.getType().toShortString()).withStyle(ChatFormatting.AQUA))
                        .append(Component.literal(" | Age: " + mobAnimal.getAge()).withStyle(ChatFormatting.GREEN))
                        .append(Component.literal(String.format(" | (x %.2f, y %.2f, z %.2f)", x, y, z)).withStyle(ChatFormatting.GRAY))
                        .append(Component.literal(" ")) // Space
                        .append(tpMsg);

                serverPlayer.sendSystemMessage(fullMsg);
            }
        }
    }
}