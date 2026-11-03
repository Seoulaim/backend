package com.seoulaim.domain.artist.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.seoulaim.domain.artist.admin.dto.AdminArtistGroupRequest;
import com.seoulaim.domain.artist.admin.dto.AdminArtistGroupRequest.AdminArtistMemberRequest;
import com.seoulaim.domain.artist.admin.dto.AdminArtistGroupResponse;
import com.seoulaim.domain.artist.admin.dto.AdminArtistGroupResponse.AdminArtistMemberResponse;
import com.seoulaim.domain.artist.repository.ArtistGroupRepository;
import com.seoulaim.domain.artist.repository.ArtistRepository;
import com.seoulaim.domain.artist.service.AdminArtistGroupService;
import com.seoulaim.domain.file.entity.File;
import com.seoulaim.domain.file.entity.FileDomain;
import com.seoulaim.domain.file.repository.FileRepository;
import com.seoulaim.global.exception.BusinessException;
import com.seoulaim.global.exception.ErrorCode;
import java.time.LocalDate;
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
class AdminArtistGroupIntegrationTest {

  @Container @ServiceConnection
  static PostgreSQLContainer postgres =
      new PostgreSQLContainer(
          DockerImageName.parse("postgis/postgis:16-3.4").asCompatibleSubstituteFor("postgres"));

  private static final String GROUP_IMAGE = "artist/2026/11/group.png";
  private static final String SAN_IMAGE = "artist/2026/11/san.png";

  @Autowired private AdminArtistGroupService adminArtistGroupService;
  @Autowired private ArtistRepository artistRepository;
  @Autowired private ArtistGroupRepository artistGroupRepository;
  @Autowired private FileRepository fileRepository;

  @BeforeEach
  void setUp() {
    artistRepository.deleteAllInBatch();
    artistGroupRepository.deleteAllInBatch();
    fileRepository.deleteAllInBatch();

    fileRepository.save(File.createPending(null, FileDomain.ARTIST, GROUP_IMAGE, "image/png", 10L));
    fileRepository.save(File.createPending(null, FileDomain.ARTIST, SAN_IMAGE, "image/png", 10L));
  }

  @Test
  @DisplayName("그룹을 멤버와 함께 등록하면 배열 순서대로 멤버 순서가 저장되고 사진 파일이 연결된다")
  void createGroup_멤버포함등록() {
    AdminArtistGroupResponse created =
        adminArtistGroupService.createGroup(
            groupRequest(
                GROUP_IMAGE,
                List.of(member(null, "산", "SAN", SAN_IMAGE), member(null, "윤호", "YUNHO", null))));

    assertThat(created.nameEn()).isEqualTo("ATEEZ");
    assertThat(created.updatedAt()).isNotNull();
    assertThat(created.members())
        .extracting(AdminArtistMemberResponse::nameEn)
        .containsExactly("SAN", "YUNHO");
    assertThat(created.members())
        .extracting(AdminArtistMemberResponse::displayOrder)
        .containsExactly(0, 1);
    assertThat(fileRepository.findAll()).allMatch(File::isLinked);

    List<AdminArtistGroupResponse> groups = adminArtistGroupService.getGroups().items();
    assertThat(groups).hasSize(1);
    assertThat(groups.getFirst().members()).hasSize(2);
  }

  @Test
  @DisplayName("그룹을 수정하면 기존 멤버는 수정, id 없는 멤버는 추가, 빠진 멤버는 유지한다")
  void updateGroup_멤버수정추가유지() {
    AdminArtistGroupResponse created =
        adminArtistGroupService.createGroup(
            groupRequest(
                GROUP_IMAGE,
                List.of(member(null, "산", "SAN", SAN_IMAGE), member(null, "윤호", "YUNHO", null))));
    AdminArtistMemberResponse san = created.members().get(0);

    AdminArtistGroupResponse updated =
        adminArtistGroupService.updateGroup(
            created.id(),
            groupRequest(
                GROUP_IMAGE,
                List.of(
                    member(san.id(), "산", "SAN (CHOI)", SAN_IMAGE),
                    member(null, "여상", "YEOSANG", null))));

    assertThat(updated.members())
        .extracting(AdminArtistMemberResponse::nameEn)
        .contains("SAN (CHOI)", "YEOSANG", "YUNHO");
    assertThat(artistRepository.count()).isEqualTo(3);
  }

  @Test
  @DisplayName("다른 그룹의 멤버 id 로 수정하면 ARTIST_NOT_IN_GROUP 예외가 발생한다")
  void updateGroup_다른그룹멤버_예외() {
    AdminArtistGroupResponse ateez =
        adminArtistGroupService.createGroup(
            groupRequest(null, List.of(member(null, "산", "SAN", null))));
    AdminArtistGroupResponse other =
        adminArtistGroupService.createGroup(
            groupRequest(null, List.of(member(null, "정한", "JEONGHAN", null))));
    AdminArtistMemberResponse jeonghan = other.members().getFirst();

    assertThatThrownBy(
            () ->
                adminArtistGroupService.updateGroup(
                    ateez.id(),
                    groupRequest(null, List.of(member(jeonghan.id(), "정한", "JEONGHAN", null)))))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.ARTIST_NOT_IN_GROUP);
  }

  @Test
  @DisplayName("업로드되지 않은 사진 key 면 FILE_NOT_FOUND 예외가 발생한다")
  void createGroup_없는사진_예외() {
    assertThatThrownBy(
            () ->
                adminArtistGroupService.createGroup(
                    groupRequest("artist/2026/11/unknown.png", List.of())))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.FILE_NOT_FOUND);
    assertThat(artistGroupRepository.count()).isZero();
  }

  @Test
  @DisplayName("다른 도메인으로 올린 사진이면 FILE_INVALID_DOMAIN 예외가 발생한다")
  void createGroup_다른도메인사진_예외() {
    fileRepository.save(
        File.createPending(null, FileDomain.PRODUCT, "product/2026/11/p.png", "image/png", 10L));

    assertThatThrownBy(
            () ->
                adminArtistGroupService.createGroup(
                    groupRequest("product/2026/11/p.png", List.of())))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.FILE_INVALID_DOMAIN);
  }

  @Test
  @DisplayName("없는 그룹을 수정하면 ARTIST_GROUP_NOT_FOUND 예외가 발생한다")
  void updateGroup_없는그룹_예외() {
    assertThatThrownBy(
            () ->
                adminArtistGroupService.updateGroup(
                    UUID.randomUUID(), groupRequest(null, List.of())))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.ARTIST_GROUP_NOT_FOUND);
  }

  private AdminArtistGroupRequest groupRequest(
      String imageKey, List<AdminArtistMemberRequest> members) {
    return new AdminArtistGroupRequest(
        "에이티즈", "ATEEZ", "エイティーズ", null, imageKey, LocalDate.of(2018, 10, 24), 0, members);
  }

  private AdminArtistMemberRequest member(UUID id, String nameKo, String nameEn, String imageKey) {
    return new AdminArtistMemberRequest(
        id, nameKo, nameEn, null, null, imageKey, LocalDate.of(1999, 7, 10));
  }
}
