package com.example.videobrowsing.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.videobrowsing.entity.Subscription;
import com.example.videobrowsing.entity.User;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    boolean existsBySubscriberAndCreator(User subscriber, User creator);

    Optional<Subscription> findBySubscriberAndCreator(User subscriber, User creator);

    long countByCreator(User creator);

    long countBySubscriber(User subscriber);

    @Query("SELECT COUNT(s) FROM Subscription s WHERE s.creator.id = :creatorId")
    long countByCreatorId(@Param("creatorId") Long creatorId);

    @Query("SELECT COUNT(s) FROM Subscription s WHERE s.subscriber.id = :subscriberId")
    long countBySubscriberId(@Param("subscriberId") Long subscriberId);
}
