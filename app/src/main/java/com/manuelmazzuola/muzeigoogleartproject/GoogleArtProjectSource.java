package com.manuelmazzuola.muzeigoogleartproject;


import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.google.android.apps.muzei.api.Artwork;
import com.google.android.apps.muzei.api.RemoteMuzeiArtSource;

import org.apache.http.protocol.HTTP;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

public class GoogleArtProjectSource extends RemoteMuzeiArtSource {
    private static final String TAG = "MuzeiGoogleArtProject";
    private static final String SOURCE_NAME = "GoogleArtProjectSource";
    private static final String SOURCE_JSON = "imax.json";

    public GoogleArtProjectSource() {
        super(SOURCE_NAME);
    }

    public GoogleArtProjectSource(String name) {
        super(name);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        setUserCommands(BUILTIN_COMMAND_ID_NEXT_ARTWORK);
    }

    @Override
    protected void onTryUpdate(int reason) throws RetryException {
        String currentToken = (getCurrentArtwork() != null) ? getCurrentArtwork().getToken() : null;
        JSONArray arts = loadJSONFromAsset(SOURCE_JSON);
        if(arts == null) return;

        Random r = new Random();
        int i1 = r.nextInt(arts.length());

        JSONObject randomArt;
        try {
            // Pick a random object
            randomArt = arts.getJSONObject(i1);
            // Debug object
            Log.d(TAG, randomArt.toString());
            // Appending =s1200 to the path the servers returns a picture no larger than 1200px
            Uri imageURI = Uri.parse(randomArt.getString("image") + "=s1200");
            // URI path as an unique id
            String token = imageURI.getPath();
            if(token.equals(currentToken)) throw new RetryException();
                publishArtwork(new Artwork.Builder()
                    .title(randomArt.getString("title"))
                    .byline(randomArt.getString("creator"))
                    .imageUri(imageURI)
                    .token(token)
                    .viewIntent(new Intent(Intent.ACTION_VIEW,
                            Uri.parse("https://www.google.com/culturalinstitute/u/0/" + randomArt.getString("link"))))
                    .build());
        } catch (JSONException ex) {
            Log.e(TAG, ex.getMessage());
            throw new RetryException(ex.getCause());
        }
    }

    // http://stackoverflow.com/questions/13814503/reading-a-json-file-in-android/13814551#13814551
    private JSONArray loadJSONFromAsset(String fileName) {
        String json;
        try {
            InputStream is = getAssets().open(fileName);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, HTTP.UTF_8);
            return new JSONArray(json);
        } catch (JSONException ex) {
            Log.e(TAG, ex.getMessage());
            return null;
        } catch (IOException ex) {
            Log.e(TAG, ex.getMessage());
            return null;
        }

    }

}
