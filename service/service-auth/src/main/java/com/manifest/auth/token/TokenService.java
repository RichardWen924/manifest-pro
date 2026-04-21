package com.manifest.auth.token;

public interface TokenService {

    IssuedToken issue(TokenPrincipal principal);

    TokenPrincipal parse(String accessToken);

    IssuedToken refresh(String refreshToken);

    void revoke(String jti);

    boolean validate(String accessToken);
}
