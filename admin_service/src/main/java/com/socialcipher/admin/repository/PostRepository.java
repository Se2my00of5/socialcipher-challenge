package com.socialcipher.admin.repository;

import com.socialcipher.admin.model.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {
    List<Post> findByUserId(Long userId);

    Optional<Post> findByTitle(String title);
}