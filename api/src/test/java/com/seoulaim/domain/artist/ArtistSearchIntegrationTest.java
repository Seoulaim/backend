package com.seoulaim.domain.artist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.seoulaim.domain.artist.dto.ArtistWithGroupResponse;
import com.seoulaim.domain.artist.entity.Artist;
import com.seoulaim.domain.artist.entity.ArtistGroup;
import com.seoulaim.domain.artist.repository.ArtistGroupRepository;
import com.seoulaim.domain.artist.repository.ArtistRepository;
import com.seoulaim.domain.artist.service.ArtistService;
import com.seoulaim.global.common.CursorPageResponse;
import com.seoulaim.global.exception.BusinessException;
import com.seoulaim.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@Tag("integration")
@SpringBootTest
@Testcontainers
class ArtistSearchIntegrationTest {

  @Container @ServiceConnection
  static PostgreSQLContainer postgres =
      new PostgreSQLContainer(
          DockerImageName.parse("postgis/postgis:16-3.4").asCompatibleSubstituteFor("postgres"));

  @Autowired private ArtistGroupRepository artistGroupRepository;
  @Autowired private ArtistRepository artistRepository;
  @Autowired private ArtistService artistService;

  @BeforeEach
  void setUp() {
    artistRepository.deleteAll();
    artistGroupRepository.deleteAll();
    ArtistGroup ateez =
        artistGroupRepository.save(ArtistGroup.create("ATEEZ", "에이티즈", null, null, null, null, 1));
    ArtistGroup ive =
        artistGroupRepository.save(ArtistGroup.create("IVE", "아이브", null, null, null, null, 2));
    artistRepository.save(Artist.create(ateez, "SAN", "산", "サン", null, null, null, 1));
    artistRepository.save(Artist.create(ateez, "YUNHO", "윤호", null, null, null, null, 2));
    artistRepository.save(Artist.create(ive, "GAEUL", "가을", null, null, null, null, 1));
  }

  @Test
  @DisplayName("아티스트 영문 이름을 대소문자 무시하고 부분 일치로 검색한다")
  void searchArtists_영문부분일치_대소문자무시() {
    CursorPageResponse<ArtistWithGroupResponse> result =
        artistService.searchArtists("san", null, 20);

    assertThat(result.items()).extracting(ArtistWithGroupResponse::nameEn).containsExactly("SAN");
    assertThat(result.items().getFirst().artistGroup().nameEn()).isEqualTo("ATEEZ");
  }

  @Test
  @DisplayName("그룹 한글 이름으로 검색하면 그 그룹 멤버 전체를 반환한다")
  void searchArtists_그룹이름_멤버전체() {
    CursorPageResponse<ArtistWithGroupResponse> result =
        artistService.searchArtists("에이티", null, 20);

    assertThat(result.items())
        .extracting(ArtistWithGroupResponse::nameEn)
        .containsExactly("SAN", "YUNHO");
  }

  @Test
  @DisplayName("일본어 이름으로도 검색한다")
  void searchArtists_일본어이름() {
    assertThat(artistService.searchArtists("サン", null, 20).items())
        .extracting(ArtistWithGroupResponse::nameEn)
        .containsExactly("SAN");
  }

  @Test
  @DisplayName("이름과 검색어의 공백·하이픈을 무시하고 비교한다")
  void searchArtists_공백하이픈무시() {
    ArtistGroup straykids =
        artistGroupRepository.save(
            ArtistGroup.create("Stray Kids", "스트레이 키즈", null, null, null, null, 3));
    artistRepository.save(Artist.create(straykids, "I.N", "아이엔", null, null, null, null, 1));
    ArtistGroup bts =
        artistGroupRepository.save(ArtistGroup.create("BTS", "방탄소년단", null, null, null, null, 4));
    artistRepository.save(Artist.create(bts, "J-HOPE", "제이홉", null, null, null, null, 1));

    assertThat(artistService.searchArtists("straykids", null, 20).items())
        .extracting(ArtistWithGroupResponse::nameEn)
        .containsExactly("I.N");
    assertThat(artistService.searchArtists("스트레이키즈", null, 20).items())
        .extracting(ArtistWithGroupResponse::nameEn)
        .containsExactly("I.N");
    assertThat(artistService.searchArtists("j hope", null, 20).items())
        .extracting(ArtistWithGroupResponse::nameEn)
        .containsExactly("J-HOPE");
  }

  @Test
  @DisplayName("멤버 이름 일치가 그룹 이름 일치보다 먼저, 그다음 그룹·멤버 노출 순서로 정렬한다")
  void searchArtists_정렬_멤버일치우선() {
    // SAN·GAEUL 은 멤버 이름에 a, YUNHO 는 그룹 이름(ATEEZ)에만 a
    assertThat(artistService.searchArtists("a", null, 20).items())
        .extracting(ArtistWithGroupResponse::nameEn)
        .containsExactly("SAN", "GAEUL", "YUNHO");
  }

  @Test
  @DisplayName("공백·하이픈만 입력하면 빈 결과")
  void searchArtists_빈검색어() {
    assertThat(artistService.searchArtists(" - ", null, 20).items()).isEmpty();
  }

  @Test
  @DisplayName("결과가 size 보다 많으면 nextCursor 로 다음 페이지를 이어서 조회한다")
  void searchArtists_커서페이징_이어서조회() {
    CursorPageResponse<ArtistWithGroupResponse> first = artistService.searchArtists("a", null, 2);
    CursorPageResponse<ArtistWithGroupResponse> second =
        artistService.searchArtists("a", first.nextCursor(), 2);

    assertThat(first.items())
        .extracting(ArtistWithGroupResponse::nameEn)
        .containsExactly("SAN", "GAEUL");
    assertThat(first.nextCursor()).isEqualTo("2");
    assertThat(second.items()).extracting(ArtistWithGroupResponse::nameEn).containsExactly("YUNHO");
    assertThat(second.nextCursor()).isNull();
  }

  @Test
  @DisplayName("검색어의 % 는 와일드카드가 아닌 문자로 검색한다")
  void searchArtists_특수문자_문자그대로() {
    assertThat(artistService.searchArtists("%", null, 20).items()).isEmpty();
  }

  @Test
  @DisplayName("숫자가 아닌 cursor 면 ARTIST_INVALID_CURSOR 예외가 발생한다")
  void searchArtists_잘못된커서_예외() {
    assertThatThrownBy(() -> artistService.searchArtists("a", "not-number", 20))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.ARTIST_INVALID_CURSOR);
  }
}
