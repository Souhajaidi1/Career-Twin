package com.souhajaidi.careertwin.career.repository;

import com.souhajaidi.careertwin.career.domain.Profile;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfileRepository extends JpaRepository<Profile, UUID> {}
