package com.seoulaim.domain.goods.repository;

import com.seoulaim.domain.goods.dto.ProductThumbnail;
import com.seoulaim.domain.goods.entity.ProductImage;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductImageRepository extends JpaRepository<ProductImage, UUID> {

  List<ProductImage> findAllByProductIdOrderByDisplayOrderAscIdAsc(UUID productId);

  // 상품별 첫 이미지는 호출 측에서 고른다 (정렬 순서상 상품별 첫 행)
  @Query(
      """
      select new com.seoulaim.domain.goods.dto.ProductThumbnail(i.product.id, i.imageKey)
      from ProductImage i
      where i.product.id in :productIds
      order by i.displayOrder asc, i.id asc
      """)
  List<ProductThumbnail> findThumbnailCandidates(@Param("productIds") Collection<UUID> productIds);
}
