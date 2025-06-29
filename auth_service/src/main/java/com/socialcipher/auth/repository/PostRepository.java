package com.socialcipher.auth.repository;

import com.socialcipher.auth.model.Post;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {
    List<Post> findByUserId(Long userId);

    Optional<Post> findByTitle(String title);
}