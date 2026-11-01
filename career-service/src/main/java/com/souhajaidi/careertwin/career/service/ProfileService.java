package com.souhajaidi.careertwin.career.service;

import com.souhajaidi.careertwin.career.api.dto.Dtos.ProfileBody;
import com.souhajaidi.careertwin.career.domain.Profile;
import com.souhajaidi.careertwin.career.repository.ProfileRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfileService {

    private final ProfileRepository profiles;

    public ProfileService(ProfileRepository profiles) {
        this.profiles = profiles;
    }

    @Transactional(readOnly = true)
    public ProfileBody get(UUID userId) {
        return profiles.findById(userId).map(ProfileBody::from).orElseGet(() -> new ProfileBody("", "", 0, ""));
    }

    @Transactional
    public ProfileBody put(UUID userId, ProfileBody body) {
        Profile profile = profiles.findById(userId).orElseGet(() -> new Profile(userId));
        profile.update(body.name().trim(), body.currentRole().trim(), body.experienceYears(), body.location().trim());
        return ProfileBody.from(profiles.save(profile));
    }
}
