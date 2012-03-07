/*
Android Asynchronous Http Client
Copyright (c) 2011 James Smith <james@loopj.com>
http://loopj.com

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package com.feigdev.webcom;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.http.client.CookieStore;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.cookie.BasicClientCookie;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.net.http.AndroidHttpClient;
import android.text.TextUtils;
import android.util.Log;

/**
* A persistent cookie store which implements the Apache HttpClient
* {@link CookieStore} interface. Cookies are stored and will persist on the
* user's device between application sessions since they are serialized and
* stored in {@link SharedPreferences}.
* <p>
* Instances of this class are designed to be used with
* {@link AsyncHttpClient#setCookieStore}, but can also be used with a 
* regular old apache HttpClient/HttpContext if you prefer.
*/
public class PersistentCookieStore implements CookieStore {
private static final String COOKIE_PREFS = "CookiePrefsFile";
private static final String COOKIE_NAME_STORE = "names";
private static final String COOKIE_NAME_PREFIX = "cookie_";
private static final String PATH = "path";
private static final String LOGTAG = "webkit";
private static final String DOMAIN = "domain";
private static final String EXPIRES = "expires";
private static final String SECURE = "secure";
private static final String MAX_AGE = "max-age";
private static final String HTTP_ONLY = "httponly";
private static final String HTTPS = "https";
private static final char PERIOD = '.';
private static final char COMMA = ',';
private static final char SEMICOLON = ';';
private static final char EQUAL = '=';
private static final char PATH_DELIM = '/';
private static final char QUESTION_MARK = '?';
private static final char WHITE_SPACE = ' ';
private static final char QUOTATION = '\"';
private static final int SECURE_LENGTH = SECURE.length();
private static final int HTTP_ONLY_LENGTH = HTTP_ONLY.length();
// RFC2109 defines 4k as maximum size of a cookie
private static final int MAX_COOKIE_LENGTH = 4 * 1024;
// RFC2109 defines 20 as max cookie count per domain. As we track with base
// domain, we allow 50 per base domain
private static final int MAX_COOKIE_COUNT_PER_BASE_DOMAIN = 50;
// RFC2109 defines 300 as max count of domains. As we track with base
// domain, we set 200 as max base domain count
private static final int MAX_DOMAIN_COUNT = 200;
// max cookie count to limit RAM cookie takes less than 100k, it is based on
// average cookie entry size is less than 100 bytes
private static final int MAX_RAM_COOKIES_COUNT = 1000;
//  max domain count to limit RAM cookie takes less than 100k,
private static final int MAX_RAM_DOMAIN_COUNT = 15;
private final ConcurrentHashMap<String, Cookie> cookies;
private final SharedPreferences cookiePrefs;

/**
 * Construct a persistent cookie store.
 */
public PersistentCookieStore(Context context) {
    cookiePrefs = context.getSharedPreferences(COOKIE_PREFS, 0);
    cookies = new ConcurrentHashMap<String, Cookie>();

    // Load any previously stored cookies into the store
    String storedCookieNames = cookiePrefs.getString(COOKIE_NAME_STORE, null);
    if(storedCookieNames != null) {
        String[] cookieNames = TextUtils.split(storedCookieNames, ",");
        for(String name : cookieNames) {
            String encodedCookie = cookiePrefs.getString(COOKIE_NAME_PREFIX + name, null);
            if(encodedCookie != null) {
                Cookie decodedCookie = decodeCookie(encodedCookie);
                if(decodedCookie != null) {
                    cookies.put(name, decodedCookie);
                }
            }
        }

        // Clear out expired cookies
        clearExpired(new Date());
    }
}

public void addCookie(Cookie cookie) {
    String name = cookie.getName();

    // Save cookie into local store, or remove if expired
    if(!cookie.isExpired(new Date())) {
        cookies.put(name, cookie);
    } else {
        cookies.remove(name);
    }

    // Save cookie into persistent store
    SharedPreferences.Editor prefsWriter = cookiePrefs.edit();
    prefsWriter.putString(COOKIE_NAME_STORE, TextUtils.join(",", cookies.keySet()));
    prefsWriter.putString(COOKIE_NAME_PREFIX + name, encodeCookie(new SerializableCookie(cookie)));
    prefsWriter.commit();
}

public void addCookie(String rawCookie, String url) throws Exception{
	ArrayList<BasicClientCookie> cookies = parseRawCookie(rawCookie, url);
	for(BasicClientCookie cookie : cookies){
		if (Constants.VERBOSE){Log.d("PersistentCookieStore","adding cookie: " + cookie.getName() + "=" + cookie.getValue());}
		addCookie(cookie);
	}
}

private ArrayList<BasicClientCookie> parseRawCookie(String rawCookie, String url) throws Exception {
    String[] rawCookieParams = rawCookie.split(";");
    ArrayList<BasicClientCookie> cookies = new ArrayList<BasicClientCookie>();
    BasicClientCookie cookie ;
    for (int i = 0; i < rawCookieParams.length; i++) {
    	String rawCookieParamNameAndValue[] = rawCookieParams[i].trim().split("=");

    	String cookieName = rawCookieParamNameAndValue[0].trim();
        String cookieValue = rawCookieParamNameAndValue[1].trim();

        for (int j = 2; j<rawCookieParamNameAndValue.length; j++){
        	cookieValue += "="+ rawCookieParamNameAndValue[j].trim();
        }
        
    	cookie = new BasicClientCookie(cookieName, cookieValue);
    	cookie.setDomain(url);
    	cookie.setExpiryDate(new Date(System.currentTimeMillis() + 1000000000));
    	cookies.add(cookie);
    }

    return cookies;
}





public void clear() {
    // Clear cookies from local store
    cookies.clear();

    // Clear cookies from persistent store
    SharedPreferences.Editor prefsWriter = cookiePrefs.edit();
    for(String name : cookies.keySet()) {
        prefsWriter.remove(COOKIE_NAME_PREFIX + name);
    }
    prefsWriter.remove(COOKIE_NAME_STORE);
    prefsWriter.commit();
}

public boolean clearExpired(Date date) {
    boolean clearedAny = false;
    SharedPreferences.Editor prefsWriter = cookiePrefs.edit();

    for(ConcurrentHashMap.Entry<String, Cookie> entry : cookies.entrySet()) {
        String name = entry.getKey();
        Cookie cookie = entry.getValue();
        if(cookie.isExpired(date)) {
            // Clear cookies from local store
            cookies.remove(name);

            // Clear cookies from persistent store
            prefsWriter.remove(COOKIE_NAME_PREFIX + name);

            // We've cleared at least one
            clearedAny = true;
        }
    }

    // Update names in persistent store
    if(clearedAny) {
        prefsWriter.putString(COOKIE_NAME_STORE, TextUtils.join(",", cookies.keySet()));
    }
    prefsWriter.commit();

    return clearedAny;
}

public List<Cookie> getCookies() {
	return new ArrayList<Cookie>(cookies.values());
}

public BasicCookieStore getCookieStore(){
	BasicCookieStore cs = new BasicCookieStore();
	for (Cookie cookie: getCookies()){
		cs.addCookie(cookie);
	}
	return cs;
}

//
// Cookie serialization/deserialization
//

protected String encodeCookie(SerializableCookie cookie) {
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    try {
        ObjectOutputStream outputStream = new ObjectOutputStream(os);
        outputStream.writeObject(cookie);
    } catch (Exception e) {
        return null;
    }

    return byteArrayToHexString(os.toByteArray());
}

protected Cookie decodeCookie(String cookieStr) {
    byte[] bytes = hexStringToByteArray(cookieStr);
    ByteArrayInputStream is = new ByteArrayInputStream(bytes);
    Cookie cookie = null;
    try {
       ObjectInputStream ois = new ObjectInputStream(is);
       cookie = ((SerializableCookie)ois.readObject()).getCookie();
    } catch (Exception e) {
       e.printStackTrace();
    }

    return cookie;
}

// Using some super basic byte array <-> hex conversions so we don't have
// to rely on any large Base64 libraries. Can be overridden if you like!
protected String byteArrayToHexString(byte[] b) {
    StringBuffer sb = new StringBuffer(b.length * 2);
    for (byte element : b) {
        int v = element & 0xff;
        if(v < 16) {
            sb.append('0');
        }
        sb.append(Integer.toHexString(v));
    }
    return sb.toString().toUpperCase();
}

protected byte[] hexStringToByteArray(String s) {
    int len = s.length();
    byte[] data = new byte[len / 2];
    for(int i=0; i<len; i+=2) {
        data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i+1), 16));
    }
    return data;
}

    /**
     * Get the base domain for a give host. E.g. mail.google.com will return
     * google.com
     * @param host The give host
     * @return the base domain
     */
    private String getBaseDomain(String host) {
        int startIndex = 0;
        int nextIndex = host.indexOf(PERIOD);
        int lastIndex = host.lastIndexOf(PERIOD);
        while (nextIndex < lastIndex) {
            startIndex = nextIndex + 1;
            nextIndex = host.indexOf(PERIOD, startIndex);
        }
        if (startIndex > 0) {
            return host.substring(startIndex);
        } else {
            return host;
        }
    }
    private final static String[] BAD_COUNTRY_2LDS =
        { "ac", "co", "com", "ed", "edu", "go", "gouv", "gov", "info",
          "lg", "ne", "net", "or", "org" };

    /**
     * parseCookie() parses the cookieString which is a comma-separated list of
     * one or more cookies in the format of "NAME=VALUE; expires=DATE;
     * path=PATH; domain=DOMAIN_NAME; secure httponly" to a list of Cookies.
     * Here is a sample: IGDND=1, IGPC=ET=UB8TSNwtDmQ:AF=0; expires=Sun,
     * 17-Jan-2038 19:14:07 GMT; path=/ig; domain=.google.com, =,
     * PREF=ID=408909b1b304593d:TM=1156459854:LM=1156459854:S=V-vCAU6Sh-gobCfO;
     * expires=Sun, 17-Jan-2038 19:14:07 GMT; path=/; domain=.google.com which
     * contains 3 cookies IGDND, IGPC, PREF and an empty cookie
     * @param host The default host
     * @param path The default path
     * @param cookieString The string coming from "Set-Cookie:"
     * @return A list of Cookies
     */
    private ArrayList<BasicClientCookie> parseCookie(String host, String path,
            String cookieString) {
        ArrayList<BasicClientCookie> ret = new ArrayList<BasicClientCookie>();

        int index = 0;
        int length = cookieString.length();
        while (true) {
        	BasicClientCookie cookie = null;

            // done
            if (index < 0 || index >= length) {
                break;
            }

            // skip white space
            if (cookieString.charAt(index) == WHITE_SPACE) {
                index++;
                continue;
            }

            /*
             * get NAME=VALUE; pair. detecting the end of a pair is tricky, it
             * can be the end of a string, like "foo=bluh", it can be semicolon
             * like "foo=bluh;path=/"; or it can be enclosed by \", like
             * "foo=\"bluh bluh\";path=/"
             *
             * Note: in the case of "foo=bluh, bar=bluh;path=/", we interpret
             * it as one cookie instead of two cookies.
             */
            int semicolonIndex = cookieString.indexOf(SEMICOLON, index);
            int equalIndex = cookieString.indexOf(EQUAL, index);
            
            // Cookies like "testcookie; path=/;" are valid and used
            // (lovefilm.se).
            // Look for 2 cases:
            // 1. "foo" or "foo;" where equalIndex is -1
            // 2. "foo; path=..." where the first semicolon is before an equal
            //    and a semicolon exists.
            if ((semicolonIndex != -1 && (semicolonIndex < equalIndex)) ||
                    equalIndex == -1) {
                // Fix up the index in case we have a string like "testcookie"
                if (semicolonIndex == -1) {
                    semicolonIndex = length;
                }            
                cookie = new BasicClientCookie(cookieString.substring(index, semicolonIndex), null);
                cookie.setDomain(host);
                cookie.setPath(path);
            } else {
            	cookie = new BasicClientCookie(cookieString.substring(index, equalIndex), null);
                cookie.setDomain(host);
                cookie.setPath(path);
                // Make sure we do not throw an exception if the cookie is like
                // "foo="
                if ((equalIndex < length - 1) &&
                        (cookieString.charAt(equalIndex + 1) == QUOTATION)) {
                    index = cookieString.indexOf(QUOTATION, equalIndex + 2);
                    if (index == -1) {
                        // bad format, force return
                        break;
                    }
                }
                // Get the semicolon index again in case it was contained within
                // the quotations.
                semicolonIndex = cookieString.indexOf(SEMICOLON, index);
                if (semicolonIndex == -1) {
                    semicolonIndex = length;
                }
                if (semicolonIndex - equalIndex > MAX_COOKIE_LENGTH) {
                    // cookie is too big, trim it
                    cookie.setValue(cookieString.substring(equalIndex + 1,
                            equalIndex + 1 + MAX_COOKIE_LENGTH));
                } else if (equalIndex + 1 == semicolonIndex
                        || semicolonIndex < equalIndex) {
                    // this is an unusual case like "foo=;" or "foo="
                    cookie.setValue("");
                } else {
                    cookie.setValue(cookieString.substring(equalIndex + 1,
                            semicolonIndex));
                }
            }
            // get attributes
            index = semicolonIndex;
            while (true) {
                // done
                if (index < 0 || index >= length) {
                    break;
                }

                // skip white space and semicolon
                if (cookieString.charAt(index) == WHITE_SPACE
                        || cookieString.charAt(index) == SEMICOLON) {
                    index++;
                    continue;
                }

                // comma means next cookie
                if (cookieString.charAt(index) == COMMA) {
                    index++;
                    break;
                }

                // "secure" is a known attribute doesn't use "=";
                // while sites like live.com uses "secure="
                if (length - index >= SECURE_LENGTH
                        && cookieString.substring(index, index + SECURE_LENGTH).
                        equalsIgnoreCase(SECURE)) {
                    index += SECURE_LENGTH;
                    cookie.setSecure(true);
                    if (index == length) break;
                    if (cookieString.charAt(index) == EQUAL) index++;
                    continue;
                }

                // "httponly" is a known attribute doesn't use "=";
                // while sites like live.com uses "httponly="
                if (length - index >= HTTP_ONLY_LENGTH
                        && cookieString.substring(index,
                            index + HTTP_ONLY_LENGTH).
                        equalsIgnoreCase(HTTP_ONLY)) {
                    index += HTTP_ONLY_LENGTH;
                    if (index == length) break;
                    if (cookieString.charAt(index) == EQUAL) index++;
                    // FIXME: currently only parse the attribute
                    continue;
                }
                equalIndex = cookieString.indexOf(EQUAL, index);
                if (equalIndex > 0) {
                    String name = cookieString.substring(index, equalIndex)
                            .toLowerCase();
                    if (name.equals(EXPIRES)) {
                        int comaIndex = cookieString.indexOf(COMMA, equalIndex);

                        // skip ',' in (Wdy, DD-Mon-YYYY HH:MM:SS GMT) or
                        // (Weekday, DD-Mon-YY HH:MM:SS GMT) if it applies.
                        // "Wednesday" is the longest Weekday which has length 9
                        if ((comaIndex != -1) &&
                                (comaIndex - equalIndex <= 10)) {
                            index = comaIndex + 1;
                        }
                    }
                    semicolonIndex = cookieString.indexOf(SEMICOLON, index);
                    int commaIndex = cookieString.indexOf(COMMA, index);
                    if (semicolonIndex == -1 && commaIndex == -1) {
                        index = length;
                    } else if (semicolonIndex == -1) {
                        index = commaIndex;
                    } else if (commaIndex == -1) {
                        index = semicolonIndex;
                    } else {
                        index = Math.min(semicolonIndex, commaIndex);
                    }
                    String value =
                            cookieString.substring(equalIndex + 1, index);
                    
                    // Strip quotes if they exist
                    if (value.length() > 2 && value.charAt(0) == QUOTATION) {
                        int endQuote = value.indexOf(QUOTATION, 1);
                        if (endQuote > 0) {
                            value = value.substring(1, endQuote);
                        }
                    }
                    if (name.equals(EXPIRES)) {
                        try {
                        	cookie.setExpiryDate(new Date(AndroidHttpClient.parseDate(value)));
                        } catch (IllegalArgumentException ex) {
                            Log.e(LOGTAG,
                                    "illegal format for expires: " + value);
                        }
                    } else if (name.equals(MAX_AGE)) {
                        try {
                            cookie.setExpiryDate(new Date( System.currentTimeMillis() + 1000  * Long.parseLong(value)));
                        } catch (NumberFormatException ex) {
                            Log.e(LOGTAG,
                                    "illegal format for max-age: " + value);
                        }
                    } else if (name.equals(PATH)) {
                        // only allow non-empty path value
                        if (value.length() > 0) {
                            cookie.setPath(value);
                        }
                    } else if (name.equals(DOMAIN)) {
                        int lastPeriod = value.lastIndexOf(PERIOD);
                        if (lastPeriod == 0) {
                            // disallow cookies set for TLDs like [.com]
                            cookie.setDomain(null);
                            continue;
                        }
                        try {
                            Integer.parseInt(value.substring(lastPeriod + 1));
                            // no wildcard for ip address match
                            if (!value.equals(host)) {
                                // no cross-site cookie
                            	cookie.setDomain(null);
                            }
                            continue;
                        } catch (NumberFormatException ex) {
                            // ignore the exception, value is a host name
                        }
                        value = value.toLowerCase();
                        if (value.charAt(0) != PERIOD) {
                            // pre-pended dot to make it as a domain cookie
                            value = PERIOD + value;
                            lastPeriod++;
                        }
                        if (host.endsWith(value.substring(1))) {
                            int len = value.length();
                            int hostLen = host.length();
                            if (hostLen > (len - 1)
                                    && host.charAt(hostLen - len) != PERIOD) {
                                // make sure the bar.com doesn't match .ar.com
                            	cookie.setDomain(null);
                                continue;
                            }
                            // disallow cookies set on ccTLDs like [.co.uk]
                            if ((len == lastPeriod + 3)
                                    && (len >= 6 && len <= 8)) {
                                String s = value.substring(1, lastPeriod);
                                if (Arrays.binarySearch(BAD_COUNTRY_2LDS, s) >= 0) {
                                	cookie.setDomain(null);
                                    continue;
                                }
                            }
                            cookie.setDomain(value);
                        } else {
                            // no cross-site or more specific sub-domain cookie
                            cookie.setDomain(null);
                        }
                    }
                } else {
                    // bad format, force return
                    index = length;
                }
            }
            if (cookie != null && cookie.getDomain() != null) {
                ret.add(cookie);
            }
        }
        return ret;
    }
}
