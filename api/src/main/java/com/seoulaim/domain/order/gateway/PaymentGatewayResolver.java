package com.seoulaim.domain.order.gateway;

import com.seoulaim.global.exception.BusinessException;
import com.seoulaim.global.exception.ErrorCode;
import java.util.EnumMap;
import java.util.Map;

/**
 * 상점(MID)별 결제 게이트웨이. 원화 MID 는 항상 있고, 외화 MID 는 키가 설정됐을 때만 있다.
 *
 * <p>빈 등록은 {@link TossPaymentGatewayConfig}.
 */
public class PaymentGatewayResolver {

  private final Map<PaymentMerchant, PaymentGateway> gateways =
      new EnumMap<>(PaymentMerchant.class);

  /**
   * @param foreign 외화 MID 키가 없으면 null
   */
  public PaymentGatewayResolver(PaymentGateway krw, PaymentGateway foreign) {
    gateways.put(PaymentMerchant.KRW, krw);
    if (foreign != null) {
      gateways.put(PaymentMerchant.FOREIGN, foreign);
    }
  }

  public boolean isAvailable(PaymentMerchant merchant) {
    return gateways.containsKey(merchant);
  }

  /**
   * @throws BusinessException PAYMENT_METHOD_UNAVAILABLE — 키가 설정되지 않은 상점
   */
  public PaymentGateway forMerchant(PaymentMerchant merchant) {
    PaymentGateway gateway = gateways.get(merchant);
    if (gateway == null) {
      throw new BusinessException(ErrorCode.PAYMENT_METHOD_UNAVAILABLE);
    }
    return gateway;
  }
}
