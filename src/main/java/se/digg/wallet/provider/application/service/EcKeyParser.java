// SPDX-FileCopyrightText: 2026 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.provider.application.service;

import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.util.JSONObjectUtils;
import java.text.ParseException;
import java.util.Map;

public final class EcKeyParser {

  private EcKeyParser() {}

  /**
   * Parses a wallet public key JWK, raising {@link ParseException} for any malformed input.
   *
   * <p>
   * {@code ECKey.parse(String)} is {@code JSONObjectUtils.parse(s)} followed by
   * {@code ECKey.parse(Map)}, but {@code JSONObjectUtils.parse()} can return {@code null} for some
   * input (e.g. the JSON literal {@code "null"}) without throwing, and {@code ECKey.parse(Map)}
   * does not null-check it, causing an unchecked {@link NullPointerException} instead of a
   * {@link ParseException}. Do the null check ourselves so this is classified the same way as any
   * other malformed JWK.
   */
  public static ECKey parse(String jwk) throws ParseException {
    Map<String, Object> jsonObject = JSONObjectUtils.parse(jwk);
    if (jsonObject == null) {
      throw new ParseException("Invalid wallet public key JWK.", 0);
    }
    return ECKey.parse(jsonObject);
  }
}
