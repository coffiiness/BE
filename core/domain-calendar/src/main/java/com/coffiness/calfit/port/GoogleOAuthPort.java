package com.coffiness.calfit.port;

import com.coffiness.calfit.model.GoogleTokenModel;

public interface GoogleOAuthPort {
    GoogleTokenModel exchangeToken(String authCode);
}
