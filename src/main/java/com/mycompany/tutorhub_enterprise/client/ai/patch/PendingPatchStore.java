package com.mycompany.tutorhub_enterprise.client.ai.patch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class PendingPatchStore {

    private static final int MAX_PATCHES = 50;

    private final Map<String, PatchProposal> pending = new LinkedHashMap<>();

    public synchronized PatchProposal add(PatchProposal proposal) {
        if (proposal == null || proposal.getId().isBlank()) {
            throw new IllegalArgumentException("Patch proposal is required");
        }
        pending.put(proposal.getId(), proposal);
        trim();
        return proposal;
    }

    public synchronized Optional<PatchProposal> find(String patchId) {
        if (patchId == null || patchId.trim().isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(pending.get(patchId.trim()));
    }

    public synchronized Optional<PatchProposal> remove(String patchId) {
        if (patchId == null || patchId.trim().isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(pending.remove(patchId.trim()));
    }

    public synchronized List<PatchProposal> snapshot() {
        return Collections.unmodifiableList(new ArrayList<>(pending.values()));
    }

    private void trim() {
        while (pending.size() > MAX_PATCHES) {
            String firstKey = pending.keySet().iterator().next();
            pending.remove(firstKey);
        }
    }
}
