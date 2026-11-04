package com.seoulaim.domain.survey.repository;

import com.seoulaim.domain.survey.entity.SurveyQuestion;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SurveyQuestionRepository extends JpaRepository<SurveyQuestion, UUID> {

  List<SurveyQuestion> findAllByOrderByDisplayOrderAscIdAsc();

  List<SurveyQuestion> findAllByArchivedFalseOrderByDisplayOrderAscIdAsc();
}
