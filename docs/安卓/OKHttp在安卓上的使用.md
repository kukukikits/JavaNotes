# OKHttp在安卓上的使用

以下示例配置让OKHttp支持Cookie保存，Cookie的序列化和反序列化。

1. 首先定义ICookieStore接口，用来读取和持久化Cookie
```java
import java.util.List;
import okhttp3.Cookie;
import okhttp3.HttpUrl;

public interface ICookieStore {
    List<Cookie> get(HttpUrl url);
    void put(HttpUrl url, List<Cookie> cookies);
    boolean removeAll();
    boolean remove(HttpUrl url, Cookie cookie);
    List<Cookie> getCookies();
}

```

2. 实现ICookieStore接口, 将Cookie持久化到安卓SharedPreferences
```java

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import okhttp3.Cookie;
import okhttp3.HttpUrl;

public class PersistentCookieStore implements ICookieStore{
    private static final String LOG_TAG = "PersistentCookieStore";
    private static final String COOKIE_PREFS = "Cookies_Prefs";
    private final Map<String, ConcurrentHashMap<String, Cookie>> cookies;
    private final SharedPreferences cookiePrefs;

    public PersistentCookieStore(Context context) {
        cookiePrefs = context.getSharedPreferences(COOKIE_PREFS, 0);
        cookies = new HashMap<>();

        //将持久化的cookies缓存到内存中 即map cookies
        Map<String, ?> prefsMap = cookiePrefs.getAll();
        for (Map.Entry<String, ?> entry : prefsMap.entrySet()) {
            String[] cookieNames = TextUtils.split((String) entry.getValue(), ",");
            for (String name : cookieNames) {
                String encodedCookie = cookiePrefs.getString(name, null);
                if (encodedCookie != null) {
                    Cookie decodedCookie = decodeCookie(encodedCookie);
                    if (decodedCookie != null) {
                        if (!cookies.containsKey(entry.getKey())) {
                            cookies.put(entry.getKey(), new ConcurrentHashMap<String, Cookie>());
                        }
                        cookies.get(entry.getKey()).put(name, decodedCookie);
                    }
                }
            }
        }
    }

    protected String getCookieToken(Cookie cookie) {
        return cookie.name() + "@" + cookie.domain();
    }

    public void add(HttpUrl url, Cookie cookie) {
        String name = getCookieToken(cookie);

        //将cookies缓存到内存中 如果缓存过期 就重置此cookie
//        if (cookie.persistent()) {
        if (!cookies.containsKey(url.host())) {
            cookies.put(url.host(), new ConcurrentHashMap<String, Cookie>());
        }
        cookies.get(url.host()).put(name, cookie);
//        } else {
//            if (cookies.containsKey(url.host())) {
//                cookies.get(url.host()).remove(name);
//            }
//        }

        //TODO: 这个持久化的逻辑应该改一下，以应对大量add操作
        //讲cookies持久化到本地
        SharedPreferences.Editor prefsWriter = cookiePrefs.edit();
        ConcurrentHashMap<String, Cookie> stringCookieConcurrentHashMap = cookies.get(url.host());
        if (null != stringCookieConcurrentHashMap) {
            for (Map.Entry<String, Cookie> cookieEntry : stringCookieConcurrentHashMap.entrySet()) {
                Cookie value = cookieEntry.getValue();
                if (needsPersist(value)) {
                    prefsWriter.putString(getCookieToken(value), encodeCookie(new SerializableOkHttpCookies(value)));
                } else {
                    prefsWriter.remove(name);
                }
            }

            prefsWriter.putString(url.host(), TextUtils.join(",", getKeySet(stringCookieConcurrentHashMap)));
            prefsWriter.apply();
        }

    }

    @Override
    public List<Cookie> get(HttpUrl url) {
        ArrayList<Cookie> ret = new ArrayList<>();
        if (cookies.containsKey(url.host()))
            ret.addAll(cookies.get(url.host()).values());
        return ret;
    }

    @Override
    public void put(HttpUrl url, List<Cookie> cookies) {
        try {
            if (cookies != null && cookies.size() > 0) {
                for (Cookie item : cookies) {
                    this.add(url, item);
                }
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }
    @Override
    public boolean removeAll() {
        SharedPreferences.Editor prefsWriter = cookiePrefs.edit();
        prefsWriter.clear();
        prefsWriter.apply();
        cookies.clear();
        return true;
    }
    @Override
    public boolean remove(HttpUrl url, Cookie cookie) {
        String name = getCookieToken(cookie);

        if (cookies.containsKey(url.host()) && cookies.get(url.host()).containsKey(name)) {
            cookies.get(url.host()).remove(name);

            SharedPreferences.Editor prefsWriter = cookiePrefs.edit();
            if (cookiePrefs.contains(name)) {
                prefsWriter.remove(name);
            }
            prefsWriter.putString(url.host(), TextUtils.join(",", getKeySet(cookies.get(url.host()))));
            prefsWriter.apply();

            return true;
        } else {
            return false;
        }
    }
    @Override
    public List<Cookie> getCookies() {
        ArrayList<Cookie> ret = new ArrayList<>();
        for (String key : cookies.keySet())
            ret.addAll(cookies.get(key).values());

        return ret;
    }

    /**
     * cookies 序列化成 string
     *
     * @param cookie 要序列化的cookie
     * @return 序列化之后的string
     */
    protected String encodeCookie(SerializableOkHttpCookies cookie) {
        if (cookie == null)
            return null;
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            ObjectOutputStream outputStream = new ObjectOutputStream(os);
            outputStream.writeObject(cookie);
        } catch (IOException e) {
            Log.d(LOG_TAG, "IOException in encodeCookie", e);
            return null;
        }

        return byteArrayToHexString(os.toByteArray());
    }

    /**
     * 将字符串反序列化成cookies
     *
     * @param cookieString cookies string
     * @return cookie object
     */
    protected Cookie decodeCookie(String cookieString) {
        byte[] bytes = hexStringToByteArray(cookieString);
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
        Cookie cookie = null;
        try {
            ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);
            cookie = ((SerializableOkHttpCookies) objectInputStream.readObject()).getCookies();
        } catch (IOException e) {
            Log.d(LOG_TAG, "IOException in decodeCookie", e);
        } catch (ClassNotFoundException e) {
            Log.d(LOG_TAG, "ClassNotFoundException in decodeCookie", e);
        }

        return cookie;
    }

    /**
     * 二进制数组转十六进制字符串
     *
     * @param bytes byte array to be converted
     * @return string containing hex values
     */
    protected String byteArrayToHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte element : bytes) {
            int v = element & 0xff;
            if (v < 16) {
                sb.append('0');
            }
            sb.append(Integer.toHexString(v));
        }
        return sb.toString().toUpperCase(Locale.US);
    }

    /**
     * 十六进制字符串转二进制数组
     *
     * @param hexString string of hex-encoded values
     * @return decoded byte array
     */
    protected byte[] hexStringToByteArray(String hexString) {
        int len = hexString.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hexString.charAt(i), 16) << 4) + Character.digit(hexString.charAt(i + 1), 16));
        }
        return data;
    }

    /**
     * 为解决以下问题，使用getKeySet来保持兼容性
     * Android 8 报错：java.lang.NoSuchMethodError: No virtual method keySet()Ljava/util/concurrent/ConcurrentHashMap$KeySetView; in class Ljava/util/concurrent/ConcurrentHashMap; or its super classes (declaration of 'java.util.concurrent.ConcurrentHashMap' appears in /system/framework/core-oj.jar)
     * @param concurrentHashMap
     * @return
     */
    private Set<String> getKeySet(ConcurrentHashMap<String, Cookie> concurrentHashMap){
        Set<String> keySet = new HashSet<>();
        for (Map.Entry<String, Cookie> entry : concurrentHashMap.entrySet()) {
            keySet.add(entry.getKey());
        }
        return keySet;
    }

    /**
     * Okhttp3 {@link Cookie#parse(long, HttpUrl, String)}方法中，如果服务器返回expires为过去的时间，
     * max-age为0的时候，{@link Cookie#persistent}属性为true，可能是一个bug，所以这里额外添加判断
     */
    private boolean needsPersist(Cookie cookie){
        return System.currentTimeMillis() < cookie.expiresAt() && cookie.persistent();
    }
}

```

3. 定义Cookie的序列化和反序列化工具
```java
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import okhttp3.Cookie;

public class SerializableOkHttpCookies implements Serializable {
    private transient final Cookie cookies;
    private transient Cookie clientCookies;

    public SerializableOkHttpCookies(Cookie cookies) {
        this.cookies = cookies;
    }

    public Cookie getCookies() {
        Cookie bestCookies = cookies;
        if (clientCookies != null) {
            bestCookies = clientCookies;
        }
        return bestCookies;
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeObject(cookies.name());
        out.writeObject(cookies.value());
        out.writeLong(cookies.expiresAt());
        out.writeObject(cookies.domain());
        out.writeObject(cookies.path());
        out.writeBoolean(cookies.secure());
        out.writeBoolean(cookies.httpOnly());
        out.writeBoolean(cookies.hostOnly());
        out.writeBoolean(cookies.persistent());
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        String name = (String) in.readObject();
        String value = (String) in.readObject();
        long expiresAt = in.readLong();
        String domain = (String) in.readObject();
        String path = (String) in.readObject();
        boolean secure = in.readBoolean();
        boolean httpOnly = in.readBoolean();
        boolean hostOnly = in.readBoolean();
        boolean persistent = in.readBoolean();
        Cookie.Builder builder = new Cookie.Builder();
        builder = builder.name(name);
        builder = builder.value(value);
        builder = builder.expiresAt(expiresAt);
        builder = hostOnly ? builder.hostOnlyDomain(domain) : builder.domain(domain);
        builder = builder.path(path);
        builder = secure ? builder.secure() : builder;
        builder = httpOnly ? builder.httpOnly() : builder;
        clientCookies =builder.build();
    }
}

```
4. 定义OkHttpUtils工具类
```java
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import okhttp3.Callback;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class OkHttpUtils {
    private static ICookieStore cookieStore = new DefaultMemoryCookieStore();
    private static OkHttpClient client = new OkHttpClient.Builder()
            .cookieJar(new CookieJar() {
                @Override
                public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
                    cookieStore.put(url, cookies);
                }

                @Override
                public List<Cookie> loadForRequest(HttpUrl url) {
                    List<Cookie> cookies = cookieStore.get(url);
                    return cookies != null ? cookies : new ArrayList<Cookie>();
                }
            })
//            .addInterceptor(new CommonInterceptor())
            .addNetworkInterceptor(new LoginInterceptor())
// 可以给OkHttp设置代理服务器            
//            .proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress("61.164.57.186", 34416)))
//            .proxyAuthenticator(new Authenticator() {
//                @Override
//                public Request authenticate(Route route, Response response) throws IOException {
//                    //设置代理服务器账号密码
//                    String credential = Credentials.basic("账户", "密码");
//                    return response.request().newBuilder()
//                            .header("Proxy-Authorization", credential)
//                            .build();
//                }
//            })
            .build();

    public static void setCookieStore(ICookieStore cookieStore){
        OkHttpUtil.cookieStore = cookieStore;
    }
    public static OkHttpClient getOkHttpClientInstance(){
        return client;
    }
    // 某些请求没有使用OkHttpUtils工具，可以使用这个方法先获取必要的请求头部
    public static Map<String, String> loadCookiesForRequest(HttpUrl url){
        List<Cookie> cookies = cookieStore.get(url);
        Map<String, String> headers = new HashMap<>(cookies.size());
        if (null != cookies && cookies.size() != 0){
            StringBuilder cookiesStr = new StringBuilder();
            for (Cookie cookie : cookies) {
                cookiesStr.append(cookie.name()).append("=").append(cookie.value()).append("; ");
            }
            String s = cookiesStr.toString();
            headers.put("Cookie", s.substring(0, s.length() - 2));
        }
        return headers;
    }
    // 获取 记住登录状态 的Cookie
    public static Cookie getPersistentLoginToken(HttpUrl url){
        List<Cookie> cookies = cookieStore.get(url);
        for (Cookie cookie : cookies) {
            if (cookie.name().equalsIgnoreCase("remember-me") && cookie.persistent()
                    && System.currentTimeMillis() < cookie.expiresAt()){
                return cookie;
            }else {
                cookieStore.remove(url, cookie);
            }
        }
        return null;
    }

    public static String login(String url, String username, String password) throws IOException {
        FormBody formBody = new FormBody.Builder()
                .add("username", username)
                .add("password", password)
                .add("remember-me", "on")
                .build();

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .post(formBody)
                .build();

        Response response = client.newCall(request).execute();
        if (response.isSuccessful()) {
            return response.body().string();
        } else {
            throw new IOException("Unexpected code " + response);
        }
    }

    public static void logout(Callback callback) throws NullPointerException{
        HttpUrl homeUrl = AppConfigUtil.getLogoutUrl();
        Request request = new Request.Builder()
                .get()
                .url(Objects.requireNonNull(homeUrl))
                .build();
        client.newCall(request).enqueue(callback);
    }

    private static class DefaultMemoryCookieStore implements ICookieStore {
        private static ConcurrentHashMap<String, List<Cookie>> cookieStore = new ConcurrentHashMap<>();
        @Override
        public List<Cookie> get(HttpUrl url) {
            List<Cookie> cookies = cookieStore.get(url.host());
            return cookies != null ? cookies : new ArrayList<Cookie>();
        }

        @Override
        public void put(HttpUrl url, List<Cookie> cookies) {
            cookieStore.put(url.host(), cookies);
        }

        @Override
        public boolean removeAll() {
            cookieStore.clear();
            return true;
        }

        @Override
        public boolean remove(HttpUrl url, Cookie cookie) {
            cookieStore.remove(url.host());
            return true;
        }

        @Override
        public List<Cookie> getCookies() {
            List<Cookie> cookies = new LinkedList<>();
            for (Map.Entry<String, List<Cookie>> entry : cookieStore.entrySet()) {
                cookies.addAll(entry.getValue());
            }
            return cookies;
        }
    }
}

```

4. 使用示例
```java
// 判断客户端是否被服务器记住登录状态
private boolean isRememberedByServer() {
    // 从配置中获取主页网址
    HttpUrl homeUrl = AppConfigUtil.getHomeUrl(this); 
    if (null == homeUrl) {
        throw new IllegalArgumentException("服务器地址不能为null");
    }
    // Do a GET request in order to make sure that user has been remembered by server
    Request getRequest = new Request.Builder()
            .get()
            .url(homeUrl)
            .build();
    OkHttpUtil.getOkHttpClientInstance().newCall(getRequest).enqueue(new Callback() {
        @Override
        public void onFailure(Call call, IOException e) {
            //doNothing
        }

        @Override
        public void onResponse(Call call, Response response) throws IOException {
            //doNothing
        }
    });
    Cookie persistentLoginToken = OkHttpUtil.getPersistentLoginToken(homeUrl);
    return null != persistentLoginToken;
}
```