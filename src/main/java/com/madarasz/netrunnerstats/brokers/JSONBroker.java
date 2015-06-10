package com.madarasz.netrunnerstats.brokers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

/**
 * Created by madarasz on 05/06/15.
 */
public final class JSONBroker {

    private JSONBroker() {

    }

    public static String readJSONFromUrl(String urlString, boolean fix) {
        BufferedReader reader = null;
        try {
            URL url = new URL(urlString);
            reader = new BufferedReader(new InputStreamReader(url.openStream()));
            StringBuffer buffer = new StringBuffer();
            int read;
            char[] chars = new char[1024];
            while ((read = reader.read(chars)) != -1)
                buffer.append(chars, 0, read);

            if (fix) {
                return "{\"input\": " + buffer.toString() + "}";
            } else {
                return buffer.toString();
            }

        } catch (Exception ex) {
            return "";

        } finally {
            if (reader != null)
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }
    }
}
