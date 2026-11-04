package com.seoulaim.domain.survey.repository;

import com.seoulaim.domain.survey.entity.SurveyOption;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SurveyOptionRepository extends JpaRepository<SurveyOption, UUID> {

  List<SurveyOption> findAllByQuestionIdInOrderByDisplayOrderAscIdAsc(Collection<UUID> questionIds);
}
