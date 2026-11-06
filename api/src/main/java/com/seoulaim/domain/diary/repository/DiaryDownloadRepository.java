package com.seoulaim.domain.diary.repository;

import com.seoulaim.domain.diary.entity.DiaryDownload;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DiaryDownloadRepository extends JpaRepository<DiaryDownload, UUID> {

  List<DiaryDownload> findAllByDiaryIdInOrderByCreatedAtDescIdDesc(Collection<UUID> diaryIds);
}
