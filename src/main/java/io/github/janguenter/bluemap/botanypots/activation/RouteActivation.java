/*
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package io.github.janguenter.bluemap.botanypots.activation;

import java.util.Locale;
import java.util.Objects;

/** Thread-safe fail-closed state for one exact rendering route. */
public final class RouteActivation {

    private final String routeId;
    private volatile Snapshot snapshot = new Snapshot(State.INACTIVE, "not-installed");

    public RouteActivation(String routeId) {
        this.routeId = requireWireValue(routeId, "routeId");
    }

    public String routeId() {
        return routeId;
    }

    public Snapshot snapshot() {
        return snapshot;
    }

    public boolean isActive() {
        return snapshot.state() == State.ACTIVE;
    }

    public synchronized void activate() {
        if (snapshot.state() != State.FAILED) {
            snapshot = new Snapshot(State.ACTIVE, "exact-profile");
        }
    }

    public synchronized void inactive(String detail) {
        if (snapshot.state() != State.FAILED) {
            snapshot = new Snapshot(State.INACTIVE, requireWireValue(detail, "detail"));
        }
    }

    public synchronized void fail(String detail) {
        snapshot = new Snapshot(State.FAILED, requireWireValue(detail, "detail"));
    }

    private static String requireWireValue(String value, String label) {
        Objects.requireNonNull(value, label);
        String normalized = value.trim().toLowerCase(Locale.ROOT).replace(' ', '-');
        if (!normalized.matches("[a-z0-9][a-z0-9._:-]*")) {
            throw new IllegalArgumentException(label + " must be a lowercase wire value");
        }
        return normalized;
    }

    public enum State {
        INACTIVE,
        ACTIVE,
        FAILED
    }

    public record Snapshot(State state, String detail) {

        public Snapshot {
            Objects.requireNonNull(state, "state");
            detail = requireWireValue(detail, "detail");
        }
    }
}
