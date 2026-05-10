package com.inncontrol.repository;

import com.inncontrol.model.UserChatPreference;
import com.inncontrol.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserChatPreferenceRepository extends JpaRepository<UserChatPreference, Long> {
    Optional<UserChatPreference> findByUserAndContactIdAndIsGroup(User user, Long contactId, boolean isGroup);
    java.util.List<UserChatPreference> findByUserAndIsGroup(User user, boolean isGroup);
    boolean existsByUserAndContactIdAndIsGroup(User user, Long contactId, boolean isGroup);
}
