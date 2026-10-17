package com.magazine.repository;

import com.magazine.model.Magazine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MagazineRepository extends JpaRepository<Magazine, Long> {

    List<Magazine> findAllByOrderByUploadedAtDesc();

    List<Magazine> findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCaseOrderByUploadedAtDesc(
            String title, String description
    );

    List<Magazine> findByCategoryIgnoreCaseOrderByUploadedAtDesc(String category);
}
