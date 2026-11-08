package dev.sketchtv.home;
import java.util.*;

public class WidgetEngineTest {
    static int checks=0;
    static void check(boolean b,String message){checks++;if(!b)throw new AssertionError(message);}
    static class Provider implements WidgetProvider {
        String id,url="https://example.invalid/data";boolean enabled=true;long interval=600000;int count=1;
        Provider(String id){this.id=id;}
        public String id(){return id;}public String title(){return "Example";}public boolean enabled(){return enabled;}public long intervalMillis(){return interval;}
        public List<Source> sources(){List<Source> s=new ArrayList<>();for(int i=0;i<count;i++)s.add(new Source("Test source",url+i,"Test license"));return s;}
        public Content parse(List<String> r)throws Exception{if(r.contains("bad"))throw new Exception("Malformed data");return new Content("Example",r.get(0));}
    }
    static class Store implements WidgetEngine.Cache {
        Map<String,WidgetEngine.Cached> data=new HashMap<>();boolean fail;
        public WidgetEngine.Cached load(String k){return data.get(k);}
        public void save(String k,WidgetEngine.Cached v)throws Exception{if(fail)throw new Exception("Disk");data.put(k,v);}
    }
    static class Fetch implements WidgetEngine.Transport {
        int calls;String value="value";boolean fail;
        public String read(String url)throws Exception{calls++;if(fail)throw new Exception("Offline");return value;}
    }
    static WidgetEngine engine(List<WidgetProvider> p,Fetch f,Store s){return new WidgetEngine(p,f,s);}
    public static void main(String[] args){
        long now=10000000L;Provider p=new Provider("one"),disabled=new Provider("off"),slow=new Provider("slow");disabled.enabled=false;slow.interval=1800000;
        Fetch f=new Fetch();Store store=new Store();WidgetEngine e=engine(Arrays.asList(p,disabled,slow),f,store);
        check(e.snapshot().get("one").content==null,"starts empty");check(e.snapshot().get("off").status(now).equals("Da configurare"),"disabled label");
        e.refresh(now,false,()->true);check(f.calls==2,"only enabled providers fetch");check(e.nextRefreshDelay(now)==600000,"next provider deadline");check(e.snapshot().get("one").fetched==now,"download timestamp");
        e.refresh(now+1000,false,()->true);check(f.calls==2,"does not refetch while fresh");
        e.refresh(now+600000,false,()->true);check(f.calls==3,"independent intervals");
        f.fail=true;e.refresh(now+1200000,false,()->true);WidgetEngine.State old=e.snapshot().get("one");check(old.error&&old.content!=null,"offline retains valid data");check(old.fetched==now+600000,"failure never changes timestamp");check(old.stale(now+1200000),"offline marked saved");
        int calls=f.calls;e.refresh(now+1200001,false,()->true);check(f.calls==calls,"failed source retry throttled");check(e.nextRefreshDelay(now+1200001)==59999,"retry deadline");
        f.fail=false;f.value="recovered";e.refresh(now+1260000,false,()->true);check(!e.snapshot().get("one").error,"recovers after retry interval");
        f.value="bad";e.refresh(now+1270000,true,()->true);check(e.snapshot().get("one").content.lines.get(0).equals("recovered"),"parser errors retain good content");
        calls=f.calls;e.refresh(now+1270001,true,()->true);check(f.calls==calls,"manual refresh debounced");
        WidgetEngine restored=engine(Arrays.asList(p),new Fetch(),store);restored.restore();check(restored.snapshot().get("one").content.lines.get(0).equals("recovered"),"restore cache");
        p.url="https://example.invalid/different";WidgetEngine changed=engine(Arrays.asList(p),new Fetch(),store);changed.restore();check(changed.snapshot().get("one").content==null,"different config cannot reuse another endpoint cache");
        Fetch cancelFetch=new Fetch();WidgetEngine cancel=engine(Arrays.asList(p),cancelFetch,new Store());cancel.refresh(now,false,()->false);check(cancelFetch.calls==0,"paused home never fetches");
        p.count=2;Fetch partial=new Fetch();WidgetEngine paused=engine(Arrays.asList(p),partial,new Store());paused.refresh(now,false,()->partial.calls<1);check(partial.calls==1&&paused.snapshot().get("one").content==null,"never publishes partial multi-feed data");
        paused.refresh(now+1,false,()->true);check(paused.snapshot().get("one").content!=null,"resume retries cancelled requests immediately");
        Store failing=new Store();failing.fail=true;WidgetEngine live=engine(Arrays.asList(p),new Fetch(),failing);live.refresh(now,false,()->true);check(live.snapshot().get("one").content!=null&&!live.snapshot().get("one").error,"disk failure does not discard live content");
        check(live.snapshot().get("one").stale(now-1),"future timestamps flagged");
        try{e.snapshot().clear();throw new AssertionError("mutable snapshot");}catch(UnsupportedOperationException good){checks++;}
        try{new WidgetEngine(Arrays.asList(p,p),f,store);throw new AssertionError("duplicate IDs");}catch(IllegalArgumentException good){checks++;}
        p.url="http://example.invalid/";try{new WidgetEngine(Arrays.asList(p),f,store);throw new AssertionError("insecure source");}catch(IllegalArgumentException good){checks++;}
        WidgetEngine idle=engine(Arrays.asList(disabled),new Fetch(),new Store());check(idle.nextRefreshDelay(now)==Long.MAX_VALUE,"no wakeups for disabled widgets");
        try{new WidgetProvider.Content("Title",new String(new char[241]).replace('\0','x'));throw new AssertionError("Unbounded remote row accepted");}catch(IllegalArgumentException good){checks++;}
        try{new WidgetProvider.Content("Title","line\nbreak");throw new AssertionError("Multiline row accepted");}catch(IllegalArgumentException good){checks++;}
        System.out.println("PASS: "+checks+" widget cache, scheduling, attribution and failure checks");
    }
}
