package com.kaassnuiver.commandslibrary.objects;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.lang.reflect.Method;
import java.util.List;

@Data
@AllArgsConstructor
public class BaseCommand {

    private final String name;
    private final String permission;
    private final List<String> aliases;
    private final String usage;
    private final Object classInstance;
    private final boolean async;
    private final Method invokeMethod;

}
