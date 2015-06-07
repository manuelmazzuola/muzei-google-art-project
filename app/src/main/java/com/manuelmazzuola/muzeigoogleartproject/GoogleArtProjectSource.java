package com.manuelmazzuola.muzeigoogleartproject;


import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.android.apps.muzei.api.Artwork;
import com.google.android.apps.muzei.api.RemoteMuzeiArtSource;

import org.apache.http.protocol.HTTP;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class GoogleArtProjectSource extends RemoteMuzeiArtSource {
    private static final String TAG = "MuzeiGoogleArtProject";
    private static final String SOURCE_NAME = "GoogleArtProjectSource";
    private static final String SOURCE_JSON = "imax.json";
    private static final String OLD_RANDOM_PREF = "muzeigap.oldrandom";
    private static final String DELAY_PREF = "muzeigap.delay";

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
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        super.onHandleIntent(intent);

        Object configFreq = intent.getExtras().get("configFreq");
        if(intent.getExtras().get("configFreq") != null) {
            unscheduleUpdate();
            int delayInMillis = Integer.parseInt(configFreq.toString());
            scheduleUpdate(System.currentTimeMillis() + delayInMillis);
        }
    }


    @Override
    protected void onTryUpdate(int reason) throws RetryException {
        String currentToken = (getCurrentArtwork() != null) ? getCurrentArtwork().getToken() : null;
        JSONArray arts = loadJSONFromAsset(SOURCE_JSON);
        if(arts == null) return;

        // Retrieve old images to be excluded
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = prefs.edit();
        Set<String> oldIndexes = prefs.getStringSet(OLD_RANDOM_PREF, new HashSet<String>());
        List<Integer> oldIndexesList = convertSetToList(oldIndexes);


        if(oldIndexesList.size() >= (arts.length() - 2)) {
            oldIndexesList = new ArrayList<>();
            Log.d(TAG, "Cleaned old images list");
        }

        Random r = new Random();
        Collections.sort(oldIndexesList); // ascending order
        int i1 = getRandomWithExclusion(r, arts.length(), oldIndexesList);

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

            // Save the json position of the selected image
            oldIndexesList.add(i1);
            editor.putStringSet(OLD_RANDOM_PREF,  convertListToSet(oldIndexesList));
            editor.apply();
        } catch (JSONException ex) {
            Log.e(TAG, ex.getMessage());
            throw new RetryException(ex.getCause());
        }

        String delayString = prefs.getString(DELAY_PREF, Integer.toString(R.string.default_delay));
        int delayInMillis = Integer.parseInt(delayString, 10);
        scheduleUpdate(System.currentTimeMillis() + delayInMillis);
    }

    private Set<String> convertListToSet(List<Integer> oldIndexesList) {
        Set<String> newSet = new HashSet<>();
        for(Integer index : oldIndexesList) {
            newSet.add(Integer.toString(index));
        }

        return newSet;
    }

    private List<Integer> convertSetToList(Set<String> oldIndexes) {
        List<Integer> newList = new ArrayList<>();
        for(String index : oldIndexes) {
            newList.add(new Integer(index));
        }

        return newList;
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

    private int getRandomWithExclusion(Random rnd, int max, List<Integer> exclude) {
        int random = rnd.nextInt(max - exclude.size());
        for (Integer ex : exclude) {
            if (random < ex) {
                break;
            }
            random++;
        }
        return random;
    }

}
