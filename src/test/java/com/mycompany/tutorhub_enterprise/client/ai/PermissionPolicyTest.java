package com.mycompany.tutorhub_enterprise.client.ai;

import com.mycompany.tutorhub_enterprise.client.ai.permission.PermissionDecision;
import com.mycompany.tutorhub_enterprise.client.ai.permission.PermissionPolicy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PermissionPolicyTest {

    @Test
    void phase7PolicyAllowsReadAndProposeButAsksForApply() {
        PermissionPolicy policy = PermissionPolicy.phase7Defaults();

        assertEquals(PermissionDecision.ALLOW, policy.decide("read_file", false));
        assertEquals(PermissionDecision.ALLOW, policy.decide("propose_patch", false));
        assertEquals(PermissionDecision.ASK, policy.decide("apply_patch", false));
        assertEquals(PermissionDecision.ALLOW, policy.decide("apply_patch", true));
        assertEquals(PermissionDecision.DENY, policy.decide("run_command", true));
    }

    @Test
    void phase8PolicyAllowsCommandProposalAndAsksForRun() {
        PermissionPolicy policy = PermissionPolicy.phase8Defaults();

        assertEquals(PermissionDecision.ALLOW, policy.decide("propose_command", false));
        assertEquals(PermissionDecision.ALLOW, policy.decide("git_status", false));
        assertEquals(PermissionDecision.ASK, policy.decide("run_command", false));
        assertEquals(PermissionDecision.ALLOW, policy.decide("run_command", true));
    }

    @Test
    void phase9PolicyAllowsFilteredMemoryNotes() {
        PermissionPolicy policy = PermissionPolicy.phase9Defaults();

        assertEquals(PermissionDecision.ALLOW, policy.decide("remember_note", false));
        assertEquals(PermissionDecision.ALLOW, policy.decide("propose_command", false));
        assertEquals(PermissionDecision.ASK, policy.decide("run_command", false));
    }
}
