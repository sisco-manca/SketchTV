package dev.sketchtv.home;
import android.content.*;
import android.os.*;
import org.json.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.zip.GZIPInputStream;
import javax.net.ssl.HttpsURLConnection;

/** Owns background work, transport and private cache. No network work in onResume or Canvas drawing. */
final class WidgetRepository {
    interface Listener {void changed(Map<String,WidgetEngine.State> states);}
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final Handler ui=new Handler(Looper.getMainLooper());
    private final WidgetEngine engine;
    private final Listener listener;
    private volatile boolean active=false,closed=false;
    private boolean busy=true;
    private Map<String,WidgetEngine.State> published=Collections.emptyMap();
    WidgetRepository(Context context,List<WidgetProvider> providers,Listener listener){
        this.listener=listener;SharedPreferences prefs=context.getSharedPreferences("widget-cache-v2",0);
        engine=new WidgetEngine(providers,WidgetRepository::read,new WidgetEngine.Cache(){
            public WidgetEngine.Cached load(String key)throws Exception{String raw=prefs.getString(key,null);if(raw==null)return null;JSONObject j=new JSONObject(raw);JSONArray a=j.getJSONArray("raw");List<String> r=new ArrayList<>();for(int i=0;i<a.length();i++)r.add(a.getString(i));return new WidgetEngine.Cached(r,j.getLong("fetched"));}
            public void save(String key,WidgetEngine.Cached c)throws Exception{JSONObject j=new JSONObject();j.put("raw",new JSONArray(c.raw));j.put("fetched",c.fetched);prefs.edit().putString(key,j.toString()).apply();}
        });
        worker.execute(()->{engine.restore();Map<String,WidgetEngine.State> state=engine.snapshot();ui.post(()->{busy=false;if(closed)return;publish(state);if(active)refresh(false);});});
    }
    private void publish(Map<String,WidgetEngine.State> state){if(!state.equals(published)){published=state;listener.changed(state);}}
    void setActive(boolean active){this.active=active;if(active)refresh(false);}
    void refresh(boolean force){if(closed||!active||busy)return;busy=true;worker.execute(()->{engine.refresh(System.currentTimeMillis(),force,()->active&&!closed);Map<String,WidgetEngine.State> state=engine.snapshot();ui.post(()->{busy=false;if(!closed)publish(state);});});}
    void close(){closed=true;active=false;worker.shutdownNow();ui.removeCallbacksAndMessages(null);}
    private static String read(String address)throws Exception{
        HttpsURLConnection c=(HttpsURLConnection)new URL(address).openConnection();c.setConnectTimeout(5000);c.setReadTimeout(5000);c.setInstanceFollowRedirects(false);c.setRequestProperty("Accept","application/json");c.setRequestProperty("User-Agent","SketchTV/0.2");
        long deadline=System.nanoTime()+java.util.concurrent.TimeUnit.SECONDS.toNanos(12);
        try{
            if(c.getResponseCode()!=200)throw new IOException("HTTP "+c.getResponseCode());InputStream stream=c.getInputStream();if("gzip".equalsIgnoreCase(c.getContentEncoding()))stream=new GZIPInputStream(stream);
            try(InputStream in=stream;ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1){if(Thread.currentThread().isInterrupted()||System.nanoTime()>deadline)throw new IOException("Richiesta interrotta o troppo lenta");if(out.size()+n>524288)throw new IOException("Risposta troppo grande");out.write(b,0,n);}return new String(out.toByteArray(),java.nio.charset.StandardCharsets.UTF_8);}
        }finally{c.disconnect();}
    }
}
