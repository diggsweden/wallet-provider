package se.digg.wallet.provider.application.config;

/**
 * Indicates that a client supplied an invalid parameter.
 */
public class InvalidWuaParameterException extends WalletRuntimeException {

  public InvalidWuaParameterException(Throwable cause) {
    super(cause);
  }

  public InvalidWuaParameterException(String message, Throwable cause) {
    super(message, cause);
  }
}
