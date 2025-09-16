package net.AnimalPlus;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;



@Mod(AnimalPlusMain.MOD_ID)
public class AnimalPlusMain {
    public static final String MOD_ID = "animalplus";

    private final ConfigManager configManager;
    private final MobManager mobManager;
    private final ModCommand modCommand;

    public AnimalPlusMain(ModContainer container, IEventBus bus) {
        this.configManager = new ConfigManager();
        this.configManager.loadConfig();

        this.mobManager = new MobManager(configManager);
        this.modCommand = new ModCommand(configManager, mobManager);

        NeoForge.EVENT_BUS.register(this.mobManager);
        NeoForge.EVENT_BUS.register(this.modCommand);
    }

}
