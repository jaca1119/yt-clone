package com.example.ytclone.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


public interface VideoRepository extends JpaRepository<VideoEntity, UUID> {
    Optional<VideoEntity> findByIdAndCreatedBy(UUID id, String user);

    List<VideoEntity> findAllByFilenameIsNotNullOrderByUploadDateDesc();

    List<VideoEntity> findAllByCreatedBy(String user);

    boolean existsByCreatedBy(String user);

    Optional<VideoEntity> findByFilename(String filename);

    @Modifying
    @Query("UPDATE VideoEntity v SET v.viewsCount = v.viewsCount + 1 WHERE v.id = :videoId")
    void saveView(UUID videoId);

    @Modifying
    @Query("UPDATE VideoEntity v SET v.filename = :filename, v.length = :length WHERE v.id = :id AND v.createdBy = :creator")
    int updateUploadedFileMetadata(UUID id, String creator, String filename, Long length);

    List<VideoEntity> findAllByTitleIgnoreCaseContainingOrderByViewsCountDesc(String searchQuery);
}
