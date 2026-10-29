package com.seoulaim.domain.diary.repository;

import com.seoulaim.domain.diary.entity.DiarySticker;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DiaryStickerRepository extends JpaRepository<DiarySticker, UUID> {

  List<DiarySticker> findAllByIsActiveTrueOrderByDisplayOrderAscIdAsc();

  List<DiarySticker> findAllByOrderByDisplayOrderAscIdAsc();
}
