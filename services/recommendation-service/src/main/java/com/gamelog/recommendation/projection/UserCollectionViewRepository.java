package com.gamelog.recommendation.projection;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserCollectionViewRepository extends JpaRepository<UserCollectionView, Long> {

    Optional<UserCollectionView> findByUsernameAndGameId(String username, Long gameId);

    List<UserCollectionView> findByUsername(String username);
}
