package com.niixlabs.inputtweaks.storage;

import com.niixlabs.inputtweaks.core.model.ProfileData;

import java.util.List;

public interface ProfileRepository {
    List<ProfileData> loadGlobalProfiles();

    void saveGlobalProfile(ProfileData profile);

    void deleteGlobalProfile(String profileName);

    ProfileData loadLocalOverrides();

    void saveLocalOverrides(ProfileData overrides);

    String loadActiveProfileName();

    void saveActiveProfileName(String profileName);
}
