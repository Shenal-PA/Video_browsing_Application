package com.example.videobrowsing.service;

import java.util.Objects;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.videobrowsing.entity.Subscription;
import com.example.videobrowsing.entity.User;
import com.example.videobrowsing.repository.SubscriptionRepository;
import com.example.videobrowsing.repository.UserRepository;

@Service
@Transactional
public class SubscriptionService {

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private UserRepository userRepository;

    public boolean isSubscribed(User subscriber, User creator) {
        if (subscriber == null || creator == null) {
            return false;
        }
        return subscriptionRepository.existsBySubscriberAndCreator(subscriber, creator);
    }

    public boolean subscribe(User subscriber, User creator) {
        validateUsers(subscriber, creator);

        if (subscriptionRepository.existsBySubscriberAndCreator(subscriber, creator)) {
            return false;
        }

        Subscription subscription = new Subscription(subscriber, creator);
        subscriptionRepository.save(subscription);
        return true;
    }

    public boolean unsubscribe(User subscriber, User creator) {
        validateUsers(subscriber, creator);

        Optional<Subscription> existing = subscriptionRepository.findBySubscriberAndCreator(subscriber, creator);
        if (existing.isEmpty()) {
            return false;
        }

        subscriptionRepository.delete(existing.get());
        return true;
    }

    public long countSubscribers(User creator) {
        if (creator == null || creator.getId() == null) {
            return 0L;
        }
        return subscriptionRepository.countByCreatorId(creator.getId());
    }

    public long countSubscriptions(User subscriber) {
        if (subscriber == null || subscriber.getId() == null) {
            return 0L;
        }
        return subscriptionRepository.countBySubscriberId(subscriber.getId());
    }

    public Optional<User> findUserById(Long userId) {
        if (userId == null) {
            return Optional.empty();
        }
        return userRepository.findById(userId);
    }

    private void validateUsers(User subscriber, User creator) {
        if (subscriber == null || creator == null) {
            throw new IllegalArgumentException("Subscriber and creator must be provided");
        }
        if (subscriber.getId() == null || creator.getId() == null) {
            throw new IllegalArgumentException("Users must be persistent entities");
        }
        if (Objects.equals(subscriber.getId(), creator.getId())) {
            throw new IllegalArgumentException("You cannot subscribe to yourself");
        }
    }
}
