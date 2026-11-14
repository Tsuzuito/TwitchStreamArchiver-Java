package model;

import com.google.gson.annotations.SerializedName;

public class ClientSecretYouTube {

    private Installed installed;

    public String getClientId() {
        return installed.getClientId();
    }

    public String getClientSecret() {
        return installed.getClientSecret();
    }

    public static class Installed {

        @SerializedName("client_id")
        private String clientId;

        @SerializedName("client_secret")
        private String clientSecret;

        public String getClientId() {
            return clientId;
        }

        public String getClientSecret() {
            return clientSecret;
        }
    }
}