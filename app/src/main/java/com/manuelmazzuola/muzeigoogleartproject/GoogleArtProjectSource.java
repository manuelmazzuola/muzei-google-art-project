package com.manuelmazzuola.muzeigoogleartproject;


import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.android.apps.muzei.api.Artwork;
import com.google.android.apps.muzei.api.RemoteMuzeiArtSource;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;

//http://tools.wmflabs.org/erwin85/randomarticle.php?lang=&family=commons&categories=Google_Art_Project&namespaces=6&subcats=1&d=10

public class GoogleArtProjectSource extends RemoteMuzeiArtSource {
    private static final String TAG = "MuzeiGoogleArtProject";
    private static final String SOURCE_NAME = "GoogleArtProjectSource";
    private static final String DELAY_PREF = "muzeigap.delay";
    private static final String WIKIMEDIA_RANDOM_URL =
            "http://tools.wmflabs.org/erwin85/randomarticle.php?lang=&family=commons&categories=Google_Art_Project&namespaces=6&subcats=1&d=10";

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
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        try {
            Art art = getRandomArtFromWikimedia();

            // Debug object
            Log.d(TAG, art.toString());
            // URI path as an unique id
            String token = art.getUrl();
            if(token.equals(currentToken)) throw new RetryException();

            StringBuilder bylineBuilder = new StringBuilder();

            if(art.getAuthor() != null) bylineBuilder.append(art.getAuthor());
            if(art.getLocation() != null) bylineBuilder.append(", ").append(art.getLocation());
            if(art.getDate() != null) bylineBuilder.append(", ").append(art.getDate());

            publishArtwork(new Artwork.Builder()
                    .title(art.getTitle())
                    .byline(bylineBuilder.toString())
                    .imageUri(Uri.parse(art.getUrl()))
                    .token(token)
                    .viewIntent(new Intent(Intent.ACTION_VIEW, Uri.parse(art.getDetailsUrl())))
                    .build());
        } catch (Exception ex) {
            Log.d(TAG, null != ex.getMessage() ? ex.getMessage() : "");
            throw new RetryException(ex.getCause());
        }

        String delayString = prefs.getString(DELAY_PREF, Integer.toString(R.string.default_delay));
        int delayInMillis = Integer.parseInt(delayString, 10);
        scheduleUpdate(System.currentTimeMillis() + delayInMillis);
    }

    private Art getRandomArtFromWikimedia() throws RetryException {
        Document page;
        try {
            page = Jsoup.connect(WIKIMEDIA_RANDOM_URL).get();
        } catch (IOException e) {
            throw new RetryException(e.getCause());
        }

        Element body = page.body();

        // URL
        String artUrl;
        Elements otherResElements = body.getElementsByClass("mw-filepage-other-resolutions");
        if(otherResElements.size() > 0 && otherResElements.get(0).getElementsByTag("a").size() > 0) {
            int lastResolution = otherResElements.get(0).getElementsByTag("a").size() - 1;
            artUrl = otherResElements.get(0).getElementsByTag("a").get(lastResolution).attr("href");
        }
        else artUrl = body.getElementById("file").getElementsByTag("a").get(0).attr("href"); // Original resolution

        // AUTHOR
        String artAuthor;
        Element authorElement = body.getElementById("fileinfotpl_aut").nextElementSibling();
        if(authorElement.getElementsByClass("fn").size() > 0)
            artAuthor = authorElement.getElementsByClass("fn").text();
        else
            artAuthor = authorElement.text();
        if(artAuthor.contains("(")) artAuthor = artAuthor.substring(0, artAuthor.indexOf("("));

        // TITLE
        String artTitle;
        Element titleElement = body.getElementById("fileinfotpl_art_title").nextElementSibling();
        if(titleElement.getElementsByTag("i").size() > 0)
            artTitle = titleElement.getElementsByTag("i").get(0).html();
        else
            artTitle = titleElement.getElementsByClass("fn").text();
        artTitle = artTitle.replace("&quot;", "\"");

        // DATE
        Element dateLabelElement = body.getElementById("fileinfotpl_date");
        String artDate;
        if(dateLabelElement.nextElementSibling().getElementsByTag("time").size() > 0)
            artDate = dateLabelElement.nextElementSibling().getElementsByTag("time").get(0).html();
        else artDate = dateLabelElement.nextElementSibling().text();

        // LOCATION
        String artLocation;
        Element locationElement = body.getElementById("fileinfotpl_art_gallery").nextElementSibling();
        if(locationElement.getElementsByTag("a").size() > 0)
            artLocation = locationElement.getElementsByTag("a").get(0).text();
        else
            artLocation = locationElement.getElementsByClass("description").text();

        // DETAILS
        String artDetailsUrl = body.getElementById("fileinfotpl_src").parent().getElementsByTag("a").get(0).attr("href");

        // Build art object
        Art art = new Art();
        art.setUrl(artUrl);
        art.setAuthor(artAuthor.trim());
        art.setTitle(artTitle.trim());
        art.setDate(artDate);
        art.setLocation(artLocation.trim());
        art.setDetailsUrl("https:" + artDetailsUrl);

        Log.d(TAG, art.getDetailsUrl());

        return art;
    }
}
