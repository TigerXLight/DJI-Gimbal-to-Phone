package com.androiddjibridge.core;

import java.util.regex.Pattern;

/** Explicit, package-scoped Learn Camera state machine. */
public final class CameraProfileLearningSession {
    private static final Pattern PACKAGE_PATTERN =
            Pattern.compile("^[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+$");

    public enum State {
        IDLE,
        WAITING_FOR_CONTROL,
        CANDIDATE_CAPTURED,
        CONFIRMED
    }

    private State state = State.IDLE;
    private String selectedPackage;
    private CameraProfileCandidate candidate;

    public synchronized boolean start(String packageName) {
        if (packageName == null || !PACKAGE_PATTERN.matcher(packageName.trim()).matches()) {
            return false;
        }
        selectedPackage = packageName.trim();
        candidate = null;
        state = State.WAITING_FOR_CONTROL;
        return true;
    }

    public synchronized void cancel() {
        selectedPackage = null;
        candidate = null;
        state = State.IDLE;
    }

    public synchronized boolean captureCandidate(CameraProfileCandidate value) {
        if (state != State.WAITING_FOR_CONTROL || value == null
                || !selectedPackage.equals(value.getPackageName())
                || !value.isStructurallyUsable()) {
            return false;
        }
        candidate = value;
        state = State.CANDIDATE_CAPTURED;
        return true;
    }

    public synchronized boolean confirmCandidate() {
        if (state != State.CANDIDATE_CAPTURED || candidate == null) {
            return false;
        }
        state = State.CONFIRMED;
        return true;
    }

    public synchronized CameraProfile createConfirmedProfile(String displayName) {
        if (state != State.CONFIRMED || candidate == null) {
            throw new IllegalStateException("candidate confirmation is required");
        }
        return candidate.toLearnedProfile(displayName);
    }

    public synchronized boolean isActive() {
        return state != State.IDLE;
    }

    public synchronized boolean isConfirmed() {
        return state == State.CONFIRMED;
    }

    public synchronized State getState() { return state; }
    public synchronized String getSelectedPackage() { return selectedPackage; }
    public synchronized CameraProfileCandidate getCandidate() { return candidate; }

    public synchronized Snapshot snapshot() {
        return new Snapshot(state, selectedPackage, candidate);
    }

    public static final class Snapshot {
        private final State state;
        private final String selectedPackage;
        private final CameraProfileCandidate candidate;

        private Snapshot(State state, String selectedPackage, CameraProfileCandidate candidate) {
            this.state = state;
            this.selectedPackage = selectedPackage;
            this.candidate = candidate;
        }

        public State getState() { return state; }
        public String getSelectedPackage() { return selectedPackage; }
        public CameraProfileCandidate getCandidate() { return candidate; }
    }
}
