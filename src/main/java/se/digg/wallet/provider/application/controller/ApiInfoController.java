// SPDX-FileCopyrightText: 2026 Digg - Agency for Digital Government/wallet-backend-reference
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.provider.application.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import se.digg.wallet.provider.api.v0.ApiInfoApi;
import se.digg.wallet.provider.api.v0.model.ApiInfoDto;

@RestController
public class ApiInfoController implements ApiInfoApi {

  @Override
  public ResponseEntity<ApiInfoDto> getApiInfo() {
    ApiInfoDto apiInfo = new ApiInfoDto();
    apiInfo.setVersion("v0");
    apiInfo.setStatus("OK");
    return ResponseEntity.ok(apiInfo);
  }
}
