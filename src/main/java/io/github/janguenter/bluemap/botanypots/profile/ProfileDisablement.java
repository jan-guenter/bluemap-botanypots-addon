/*
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package io.github.janguenter.bluemap.botanypots.profile;

import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

/** Operator-controlled, restart-scoped exact-profile disablement. */
public final class ProfileDisablement {

    public static final String SYSTEM_PROPERTY = "bluemap.botanypots.disabledProfiles";
    public static final String ENVIRONMENT_VARIABLE = "BLUEMAP_BOTANYPOTS_DISABLED_PROFILES";
    private static final Pattern PROFILE_ID = Pattern.compile("[a-z0-9_.-]+");

    private final Set<String> disabledProfiles;

    private ProfileDisablement(Set<String> disabledProfiles) {
        this.disabledProfiles = Set.copyOf(disabledProfiles);
    }

    public static ProfileDisablement current() {
        return from(System.getProperty(SYSTEM_PROPERTY), System.getenv(ENVIRONMENT_VARIABLE));
    }

    public static ProfileDisablement from(String propertyValue, String environmentValue) {
        TreeSet<String> result = new TreeSet<>();
        addCsv(result, propertyValue);
        addCsv(result, environmentValue);
        return new ProfileDisablement(result);
    }

    public boolean isDisabled(String profileId) {
        return disabledProfiles.contains(profileId.toLowerCase(Locale.ROOT));
    }

    public Set<String> disabledProfiles() {
        return disabledProfiles;
    }

    private static void addCsv(Set<String> output, String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return;
        }
        for (String token : rawValue.split(",", -1)) {
            String normalized = token.trim().toLowerCase(Locale.ROOT);
            if (PROFILE_ID.matcher(normalized).matches()) {
                output.add(normalized);
            }
        }
    }
}
