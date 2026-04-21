package com.manifest.auth.security;

import java.security.PrivateKey;
import java.security.PublicKey;

public interface KeyProvider {

    PublicKey getPublicKey();

    PrivateKey getPrivateKey();

    String getPublicKeyPem();
}
