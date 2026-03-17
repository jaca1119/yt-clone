package com.example.ytclone.application;

import com.example.ytclone.infrastructure.persistence.VideoEntity;
import com.example.ytclone.infrastructure.persistence.VideoRepository;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.FluentQuery;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class InMemoryVideoRepository implements VideoRepository {
    ConcurrentHashMap<UUID, VideoEntity> videos = new ConcurrentHashMap<>();

    @Override
    public void flush() {

    }

    @Override
    public <S extends VideoEntity> S saveAndFlush(S entity) {
        return null;
    }

    @Override
    public <S extends VideoEntity> List<S> saveAllAndFlush(Iterable<S> entities) {
        return List.of();
    }

    @Override
    public void deleteAllInBatch(Iterable<VideoEntity> entities) {

    }

    @Override
    public void deleteAllByIdInBatch(Iterable<UUID> uuids) {

    }

    @Override
    public void deleteAllInBatch() {

    }

    @Override
    public VideoEntity getOne(UUID uuid) {
        return null;
    }

    @Override
    public VideoEntity getById(UUID uuid) {
        return null;
    }

    @Override
    public VideoEntity getReferenceById(UUID uuid) {
        return null;
    }

    @Override
    public <S extends VideoEntity> Optional<S> findOne(Example<S> example) {
        return Optional.empty();
    }

    @Override
    public <S extends VideoEntity> List<S> findAll(Example<S> example) {
        return List.of();
    }

    @Override
    public <S extends VideoEntity> List<S> findAll(Example<S> example, Sort sort) {
        return List.of();
    }

    @Override
    public <S extends VideoEntity> Page<S> findAll(Example<S> example, Pageable pageable) {
        return null;
    }

    @Override
    public <S extends VideoEntity> long count(Example<S> example) {
        return 0;
    }

    @Override
    public <S extends VideoEntity> boolean exists(Example<S> example) {
        return false;
    }

    @Override
    public <S extends VideoEntity, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) {
        return null;
    }

    @Override
    public <S extends VideoEntity> S save(S entity) {
        videos.merge(entity.getId(), entity, (videoEntity, videoEntity2) -> {
            videoEntity.setLength(videoEntity2.getLength());
            videoEntity.setFilename(videoEntity2.getFilename());
            return videoEntity;
        });
        return entity;
    }

    @Override
    public <S extends VideoEntity> List<S> saveAll(Iterable<S> entities) {
        return List.of();
    }

    @Override
    public Optional<VideoEntity> findById(UUID uuid) {
        return Optional.ofNullable(videos.get(uuid));
    }

    @Override
    public boolean existsById(UUID uuid) {
        return false;
    }

    @Override
    public List<VideoEntity> findAll() {
        return List.of();
    }

    @Override
    public List<VideoEntity> findAllById(Iterable<UUID> uuids) {
        return List.of();
    }

    @Override
    public long count() {
        return 0;
    }

    @Override
    public void deleteById(UUID uuid) {

    }

    @Override
    public void delete(VideoEntity entity) {

    }

    @Override
    public void deleteAllById(Iterable<? extends UUID> uuids) {

    }

    @Override
    public void deleteAll(Iterable<? extends VideoEntity> entities) {

    }

    @Override
    public void deleteAll() {

    }

    @Override
    public List<VideoEntity> findAll(Sort sort) {
        return List.of();
    }

    @Override
    public Page<VideoEntity> findAll(Pageable pageable) {
        return null;
    }

    @Override
    public Optional<VideoEntity> findByIdAndCreatedBy(UUID id, String user) {
        VideoEntity videoEntity = videos.get(id);
        if (videoEntity != null && videoEntity.getCreatedBy().equals(user)) {
            return Optional.of(videoEntity);
        }

        return Optional.empty();
    }
}
