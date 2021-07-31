package com.kaassnuiver.commandslibrary;

import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;
import java.util.function.Supplier;
import java.util.logging.Level;

public final class CommandsLibrary extends JavaPlugin {

    @Getter private static CommandsLibrary instance;

    private static RegisterCommand registerCommand;

    @Override
    public void onEnable() {
        // Plugin startup logic
        instance = this;
        registerCommand = new RegisterCommand(this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        registerCommand = null;
        instance = null;
    }

    @SuppressWarnings("unused")
    public static void registerCommand(Supplier<Object> supplier) {
        Objects.requireNonNull(supplier, "Supplier cannot be null.");
        Objects.requireNonNull(supplier.get(), "Object cannot be null.");
        getInstance().getLogger().log(Level.INFO, "Attempting to register commands of class " + supplier.get().getClass().getSimpleName());
        registerCommand.register(supplier.get());
    }
}
