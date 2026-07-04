package com.mycompany.tutorhub_enterprise.client.ai.command;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CommandParserPolicyTest {

    @Test
    void parsesQuotedCommandWithoutShell() {
        List<String> tokens = CommandParser.parse("\"C:/Program Files/Java/bin/java.exe\" -version");

        assertEquals("C:/Program Files/Java/bin/java.exe", tokens.get(0));
        assertEquals("-version", tokens.get(1));
    }

    @Test
    void rejectsShellOperators() {
        assertThrows(IllegalArgumentException.class, () -> CommandParser.parse("git status && del file.txt"));
    }

    @Test
    void policyAllowsGitStatusAndDeniesMutatingGit() {
        CommandPolicy policy = new CommandPolicy();

        policy.validate(new CommandSpec(List.of("git", "status", "--short"), ".", 30, "status"));
        assertThrows(SecurityException.class,
                () -> policy.validate(new CommandSpec(List.of("git", "reset", "--hard"), ".", 30, "reset")));
    }
}
