package com.seoulaim.domain.artist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.seoulaim.domain.artist.dto.ArtistWithGroupResponse;
import com.seoulaim.domain.artist.entity.Artist;
import com.seoulaim.domain.artist.entity.ArtistGroup;
import com.seoulaim.domain.artist.repository.ArtistGroupRepository;
import com.seoulaim.domain.artist.repository.ArtistRepository;
import com.seoulaim.domain.artist.repository.UserBiasRepository;
import com.seoulaim.domain.artist.service.UserBiasService;
import com.seoulaim.domain.user.entity.User;
import com.seoulaim.domain.user.repository.UserRepository;
import com.seoulaim.global.exception.BusinessException;
import com.seoulaim.global.exception.ErrorCode;
import java.util.List;
import java.util.UUID;
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
class UserBiasIntegrationTest {

  @Container @ServiceConnection
  static PostgreSQLContainer postgres =
      new PostgreSQLContainer(
          DockerImageName.parse("postgis/postgis:16-3.4").asCompatibleSubstituteFor("postgres"));

  @Autowired private UserBiasService userBiasService;
  @Autowired private UserBiasRepository userBiasRepository;
  @Autowired private ArtistRepository artistRepository;
  @Autowired private ArtistGroupRepository artistGroupRepository;
  @Autowired private UserRepository userRepository;

  private UUID userId;
  private Artist san;
  private Artist hongjoong;
  private Artist yunho;

  @BeforeEach
  void setUp() {
    userBiasRepository.deleteAll();
    artistRepository.deleteAll();
    artistGroupRepository.deleteAll();
    userRepository.deleteAll();

    userId = userRepository.save(User.createWithEmail("luna@example.com", "hash", "Luna")).getId();
    ArtistGroup ateez =
        artistGroupRepository.save(ArtistGroup.create("ATEEZ", "에이티즈", null, null, null, null, 1));
    san = artistRepository.save(Artist.create(ateez, "SAN", "산", null, null, null, null, 1));
    hongjoong =
        artistRepository.save(Artist.create(ateez, "HONGJOONG", "홍중", null, null, null, null, 2));
    yunho = artistRepository.save(Artist.create(ateez, "YUNHO", "윤호", null, null, null, null, 3));
  }

  @Test
  @DisplayName("bias 를 저장하면 요청 순서대로 그룹 정보와 함께 반환한다")
  void replaceMyBiases_저장_요청순서반환() {
    List<ArtistWithGroupResponse> result =
        userBiasService.replaceMyBiases(userId, List.of(hongjoong.getId(), san.getId()));

    assertThat(result)
        .extracting(ArtistWithGroupResponse::nameEn)
        .containsExactly("HONGJOONG", "SAN");
    assertThat(result.getFirst().artistGroup().nameEn()).isEqualTo("ATEEZ");
  }

  @Test
  @DisplayName("기존 bias 와 겹치는 아티스트로 다시 저장해도 unique 충돌 없이 교체된다")
  void replaceMyBiases_겹치는아티스트_교체() {
    userBiasService.replaceMyBiases(userId, List.of(san.getId(), hongjoong.getId()));

    userBiasService.replaceMyBiases(userId, List.of(hongjoong.getId(), yunho.getId()));

    assertThat(userBiasService.findMyBiases(userId))
        .extracting(ArtistWithGroupResponse::nameEn)
        .containsExactlyInAnyOrder("HONGJOONG", "YUNHO");
  }

  @Test
  @DisplayName("빈 배열로 저장하면 bias 가 전체 해제된다")
  void replaceMyBiases_빈배열_전체해제() {
    userBiasService.replaceMyBiases(userId, List.of(san.getId()));

    userBiasService.replaceMyBiases(userId, List.of());

    assertThat(userBiasService.findMyBiases(userId)).isEmpty();
  }

  @Test
  @DisplayName("존재하지 않는 아티스트가 섞이면 ARTIST_NOT_FOUND 예외가 발생하고 기존 bias 는 유지된다")
  void replaceMyBiases_없는아티스트_롤백() {
    userBiasService.replaceMyBiases(userId, List.of(san.getId()));

    assertThatThrownBy(
            () ->
                userBiasService.replaceMyBiases(userId, List.of(yunho.getId(), UUID.randomUUID())))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.ARTIST_NOT_FOUND);
    assertThat(userBiasService.findMyBiases(userId))
        .extracting(ArtistWithGroupResponse::nameEn)
        .containsExactly("SAN");
  }

  @Test
  @DisplayName("같은 아티스트를 중복으로 보내면 COMMON_INVALID_REQUEST 예외가 발생한다")
  void replaceMyBiases_중복_예외() {
    assertThatThrownBy(
            () -> userBiasService.replaceMyBiases(userId, List.of(san.getId(), san.getId())))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.COMMON_INVALID_REQUEST);
  }
}
