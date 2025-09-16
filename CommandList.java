package net.AnimalPlus;

import java.util.HashSet;
import java.util.Set;

public enum CommandList {
    change,
    check,
    reset;

    CommandList() {

    }

    public String toString(){
        return this.name().toLowerCase();
    }

    public static Set<String> getAllCommands(){
        Set<String> commands = new HashSet<>();
        for(CommandList c : CommandList.values()){
            commands.add(c.toString());
        }
        return commands;
    }

    public static CommandList fromString(String command) {
        for (CommandList c : CommandList.values()) {
            if (c.toString().equalsIgnoreCase(command)) {
                return c;
            }
        }
        return null;
    }

    public enum SubArgs {
        AGE,
        BREED,
        ANIMAL,
        SECONDS;

        SubArgs() {

        }

        public static SubArgs fromString(String command) {
            for (SubArgs c : SubArgs.values()) {
                if (c.toString().equalsIgnoreCase(command)) {
                    return c;
                }
            }
            return null;
        }

        @Override
        public String toString() {
            return this.name().toLowerCase();
        }
    }

}
