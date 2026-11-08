package dev.sketchtv.home;
import java.util.*;
import java.security.MessageDigest;

/** Synchronous core. A repository runs it off the UI thread; dependencies are replaceable in tests. */
final class WidgetEngine {
    interface Transport {String read(String url)throws Exception;}
    interface Cache {Cached load(String key)throws Exception;void save(String key,Cached value)throws Exception;}
    interface Active {boolean get();}
    static final class Cached {
        final List<String> raw; final long fetched;
        Cached(List<String> raw,long fetched){this.raw=Collections.unmodifiableList(new ArrayList<>(raw));this.fetched=fetched;}
    }
    static final class State {
        final WidgetProvider provider;final WidgetProvider.Content content;final long fetched;final boolean error;
        State(WidgetProvider p,WidgetProvider.Content c,long f,boolean e){provider=p;content=c;fetched=f;error=e;}
        boolean stale(long now){return content!=null&&(error||now<fetched||now-fetched>Math.max(3600000L,provider.intervalMillis()*2));}
        String status(long now){return !provider.enabled()?"Da configurare":content==null?(error?"Fonte non disponibile":"In attesa dei dati"):stale(now)?"Dati salvati · verifica data":"Aggiornato";}
    }
    private final List<WidgetProvider> providers;
    private final Map<String,State> states=new LinkedHashMap<>();
    private final Map<String,Long> attempts=new HashMap<>();
    private final Transport transport;private final Cache cache;
    WidgetEngine(List<WidgetProvider> providers,Transport transport,Cache cache){
        this.providers=new ArrayList<>(providers);this.transport=transport;this.cache=cache;
        for(WidgetProvider p:providers){
            if(states.containsKey(p.id())||p.sources().isEmpty()||p.intervalMillis()<600000)throw new IllegalArgumentException("Invalid widget contract: "+p.id());
            for(WidgetProvider.Source s:p.sources())if(s.name.trim().isEmpty()||s.license.trim().isEmpty()||!s.url.startsWith("https://"))throw new IllegalArgumentException("Every source needs attribution and HTTPS");
            states.put(p.id(),new State(p,null,0,false));
        }
    }
    void restore(){
        for(WidgetProvider p:providers)if(p.enabled())try{Cached c=cache.load(key(p));if(c!=null&&c.fetched>0&&c.raw.size()==p.sources().size())states.put(p.id(),new State(p,p.parse(c.raw),c.fetched,false));}catch(Exception ignored){}
    }
    void refresh(long now,boolean force,Active active){
        for(WidgetProvider p:providers){
            if(!active.get())break;if(!p.enabled())continue;
            State old=states.get(p.id());Long last=attempts.get(p.id());
            // Force still respects a short debounce; normal retries cannot spin on an offline source.
            if(last!=null&&now>=last&&now-last<(force?5000L:old.error?60000L:p.intervalMillis()))continue;
            if(!force&&!old.error&&old.content!=null&&now>=old.fetched&&now-old.fetched<p.intervalMillis())continue;
            attempts.put(p.id(),now);
            try{
                List<String> raw=new ArrayList<>();
                for(WidgetProvider.Source s:p.sources()){if(!active.get()){attempts.remove(p.id());return;}raw.add(transport.read(s.url));}
                if(!active.get()){attempts.remove(p.id());return;}
                WidgetProvider.Content content=p.parse(Collections.unmodifiableList(raw));
                if(content==null)throw new IllegalArgumentException("Empty widget content");
                states.put(p.id(),new State(p,content,now,false));
                try{cache.save(key(p),new Cached(raw,now));}catch(Exception ignored){} // Persistence failure must not discard valid live data.
            }catch(Exception e){states.put(p.id(),new State(p,old.content,old.fetched,true));}
        }
    }
    Map<String,State> snapshot(){return Collections.unmodifiableMap(new LinkedHashMap<>(states));}
    private String key(WidgetProvider p)throws Exception{
        MessageDigest hash=MessageDigest.getInstance("SHA-256");
        StringBuilder s=new StringBuilder(p.id());for(WidgetProvider.Source src:p.sources())s.append('\n').append(src.url);
        byte[] bytes=hash.digest(s.toString().getBytes("UTF-8"));StringBuilder result=new StringBuilder();for(byte b:bytes)result.append(String.format(Locale.ROOT,"%02x",b&255));return result.toString();
    }
}
