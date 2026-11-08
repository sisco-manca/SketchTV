package dev.sketchtv.home;
import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.media.tv.*;
import android.net.Uri;
import android.net.*;
import android.net.wifi.*;
import android.os.*;
import android.provider.Settings;
import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.zip.GZIPInputStream;
import javax.net.ssl.HttpsURLConnection;

public class MainActivity extends Activity {
    private static final String[] APPS=Config.APPS;
    private ReferenceHome home;
    private SharedPreferences cache;
    private final Handler ui=new Handler(Looper.getMainLooper());
    private final Runnable homeOk=()->HomeButton.onHomeOk(this);
    private final ExecutorService io=Executors.newSingleThreadExecutor();
    private final ExecutorService stateIo=Executors.newSingleThreadExecutor();
    private boolean cacheReady=false,inputsDirty=true,inputsLoading=false,netLoading=false,netDirty=true;
    private boolean receiverOn=false,networkCallbackOn=false;
    private final Map<String,String> inputNames=new HashMap<>();
    private final Map<String,List<Intent>> inputIntents=new HashMap<>();
    private Intent liveIntent;
    private final Runnable inputRefresh=()->refreshInputs();
    private final Runnable netRefresh=()->refreshNetwork();
    private final BroadcastReceiver netReceiver=new BroadcastReceiver(){
        @Override public void onReceive(Context c,Intent i){requestNetwork();}
    };
    private final ConnectivityManager.NetworkCallback netCallback=new ConnectivityManager.NetworkCallback(){
        @Override public void onAvailable(Network n){ui.post(()->requestNetwork());}
        @Override public void onLost(Network n){ui.post(()->requestNetwork());}
        @Override public void onCapabilitiesChanged(Network n,NetworkCapabilities c){ui.post(()->requestNetwork());}
    };
    private volatile boolean visible=false,destroyed=false;
    private boolean loading=false;
    private long lastAttempt=0;
    private final List<TvInputInfo> inputs=new ArrayList<>();
    private TvInputInfo av,hdmi1,hdmi2;
    private final Runnable minute=new Runnable(){public void run(){if(visible){home.tick();ui.postDelayed(this,60000-System.currentTimeMillis()%60000);}}};
    private final Runnable poll=new Runnable(){public void run(){if(visible){fetch();ui.postDelayed(this,10*60*1000L);}}};
    private final TvInputManager.TvInputCallback inputChanged=new TvInputManager.TvInputCallback(){
        @Override public void onInputAdded(String id){loadInputs();}
        @Override public void onInputRemoved(String id){loadInputs();}
        @Override public void onInputUpdated(String id){loadInputs();}
    };
    @Override protected void onCreate(Bundle state){
        super.onCreate(state);cache=getSharedPreferences("reference-cache-v1",0);
        home=new ReferenceHome(this,this::open);setContentView(home);loadInputs();loadCache();ui.postDelayed(()->watchNetwork(),150);
        TvInputManager tim=(TvInputManager)getSystemService(TV_INPUT_SERVICE);if(tim!=null)tim.registerCallback(inputChanged,ui);
    }
    @Override protected void onResume(){super.onResume();visible=true;home.tick();if(inputsDirty)ui.postDelayed(inputRefresh,180);requestNetwork();ui.post(minute);ui.postDelayed(poll,400);overridePendingTransition(0,0);HomeButton.onHomeResumed(this);ui.postDelayed(homeOk,1500);}
    @Override protected void onPause(){visible=false;ui.removeCallbacks(minute);ui.removeCallbacks(poll);ui.removeCallbacks(homeOk);ui.removeCallbacks(inputRefresh);ui.removeCallbacks(netRefresh);HomeButton.onHomeOk(this);super.onPause();}
    @Override protected void onDestroy(){destroyed=true;visible=false;ui.removeCallbacksAndMessages(null);io.shutdownNow();stateIo.shutdownNow();if(receiverOn)unregisterReceiver(netReceiver);if(networkCallbackOn)((ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE)).unregisterNetworkCallback(netCallback);TvInputManager tim=(TvInputManager)getSystemService(TV_INPUT_SERVICE);if(tim!=null)tim.unregisterCallback(inputChanged);home.dispose();super.onDestroy();}
    @Override public void onBackPressed(){ /* Stay on the home screen. */ }
    @Override public boolean onKeyUp(int key,android.view.KeyEvent event){if(key==android.view.KeyEvent.KEYCODE_MENU){settings();return true;}return super.onKeyUp(key,event);}
    private void loadCache(){
        stateIo.execute(()->{
            String[][] parsed=new String[5][];long[] times=new long[5];
            Map<String,?> saved=cache.getAll();
            for(int i=0;i<5;i++)try{if(!Config.enabled(i))continue;Object raw=saved.get("raw"+i),stamp=saved.get("time"+i);if(raw instanceof String){parsed[i]=ReferenceData.parse(i,(String)raw);times[i]=stamp instanceof Long?(Long)stamp:0;}}catch(Exception ignored){}
            ui.post(()->{if(destroyed)return;for(int i=0;i<5;i++)if(parsed[i]!=null&&home.fetched[i]<=times[i]){home.data[i]=parsed[i];home.fetched[i]=times[i];}cacheReady=true;home.refresh();if(visible)fetch();});
        });
    }
    private void fetch(){
        long now=System.currentTimeMillis();if(!visible||destroyed||!cacheReady||loading||now-lastAttempt<60000)return;
        boolean due=false;for(int i=0;i<5;i++)if(Config.enabled(i)&&now-home.fetched[i]>=10*60*1000L)due=true;if(!due)return;
        loading=true;lastAttempt=now;
        io.execute(()->{
            for(int i=0;i<5;i++){
                if(!visible||destroyed)break;if(!Config.enabled(i))continue;final int index=i;
                try{
                    String raw=read(ReferenceData.URLS[i]);String[] parsed=ReferenceData.parse(i,raw);long time=System.currentTimeMillis();
                    cache.edit().putString("raw"+i,raw).putLong("time"+i,time).apply();
                    ui.post(()->{if(!destroyed){home.data[index]=parsed;home.fetched[index]=time;home.errors[index]=false;if(visible)home.refresh();}});
                }catch(Exception e){ui.post(()->{if(!destroyed){home.errors[index]=true;if(visible)home.refresh();}});}
            }
            ui.post(()->loading=false);
        });
    }
    private static String read(String address)throws Exception{
        HttpsURLConnection c=(HttpsURLConnection)new URL(address).openConnection();c.setConnectTimeout(8000);c.setReadTimeout(10000);c.setInstanceFollowRedirects(false);c.setRequestProperty("Accept","application/json");c.setRequestProperty("User-Agent","SketchTV/0.1");
        try{if(c.getResponseCode()!=200)throw new IOException("HTTP "+c.getResponseCode());InputStream stream=c.getInputStream();if("gzip".equalsIgnoreCase(c.getContentEncoding()))stream=new GZIPInputStream(stream);
            try(InputStream in=stream;ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1){if(out.size()+n>524288)throw new IOException("Risposta troppo grande");out.write(b,0,n);}return new String(out.toByteArray(),java.nio.charset.StandardCharsets.UTF_8);}
        }finally{c.disconnect();}
    }
    private void open(int index){
        if(index==0){weather();return;}if(index==1){details(Config.CONSTRUCTOR_NAME,new int[]{1,2});return;}if(index==2){details(Config.TEAM_NAME,new int[]{3,4});return;}
        if(index>=3&&index<=6){launch(APPS[index-3]);return;}
        if(index==7){startActivity(new Intent(this,AppsActivity.class).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION));overridePendingTransition(0,0);return;}
        if(index==13){openSettings(false);return;}if(index==14){openSettings(true);return;}
        if(index==8){liveTv();return;}if(index==9){source(av,"AV");return;}if(index==10){source(hdmi1,"HDMI 1");return;}if(index==11){source(hdmi2,"HDMI 2");return;}others();
    }
    private void settings(){new AlertDialog.Builder(this).setTitle("Sketch TV").setItems(new String[]{"Impostazioni TV","Aggiorna dati","Tutte le sorgenti","Home originale"},(d,n)->{if(n==0)startActivity(new Intent(Settings.ACTION_SETTINGS));else if(n==1){lastAttempt=0;for(int i=0;i<5;i++)home.fetched[i]=0;fetch();}else if(n==2)listSources(inputs,"Tutte le sorgenti");else HomeButton.openGoogleTv(this);}).setNegativeButton("Chiudi",null).show();}
    private String values(int i){
        String[] d=home.data[i];if(d==null)return "Dati non disponibili";
        if(i==0)return Config.CITY+" · "+d[0]+"\nRilevazione: "+d[1].replace('T',' ')+"\n"+d[2];
        if(i==1)return d[0]+"\n"+d[1]+": "+d[2]+"\n"+d[3]+": "+d[4]+"\nStagione "+d[5]+" · GP "+d[6];
        if(i==2)return "Classifica costruttori: P"+d[0]+"\nStagione "+d[1]+" · dopo il GP "+d[2];
        if(i==3)return d[0]+" "+d[1]+" – "+d[3]+" "+d[2]+"\nUltima partita conclusa: "+d[4];
        return Config.TEAM_NAME+": "+d[0]+"ª posizione\nClassifica campionato configurato · stagione "+d[1];
    }
    private String provenance(int i){SimpleDateFormat f=new SimpleDateFormat("dd/MM/yyyy HH:mm",Locale.ITALY);f.setTimeZone(TimeZone.getTimeZone(Config.TIME_ZONE));return ReferenceData.SOURCES[i]+"\n"+ReferenceData.URLS[i]+"\nScaricato: "+(home.fetched[i]==0?"mai":f.format(new Date(home.fetched[i])))+(home.errors[i]?"\nUltimo tentativo fallito: dati salvati, se presenti.":"");}
    private void details(String title,int[] sources){StringBuilder b=new StringBuilder();for(int i:sources)b.append(values(i)).append("\n\n").append(provenance(i)).append("\n\n");b.append("Aggiornamento ogni 10 minuti mentre la Home è visibile. Dati sportivi non in diretta. Il risultato della partita è mostrato con la squadra di casa sopra.");new AlertDialog.Builder(this).setTitle(title).setMessage(b.toString()).setPositiveButton("Chiudi",null).show();}
    private void weather(){new AlertDialog.Builder(this).setTitle(Config.CITY+" · meteo e orario").setMessage(values(0)+"\n\n"+provenance(0)+"\n\nOrario e data: fuso configurato. Le fonti possono non essere disponibili.").setPositiveButton("Chiudi",null).setNeutralButton("Impostazioni",(d,n)->settings()).show();}
    private void launch(String pkg){Intent i=getPackageManager().getLeanbackLaunchIntentForPackage(pkg);if(i==null)i=getPackageManager().getLaunchIntentForPackage(pkg);if(i==null){message("App non disponibile","L'app non è installata o non offre un avvio compatibile.");return;}try{startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_NO_ANIMATION));}catch(Exception e){message("Avvio non riuscito","Apri l'app dalla Home originale.");}}
    private void liveTv(){try{startActivity(new Intent(liveIntent!=null?liveIntent:new Intent(Intent.ACTION_VIEW,TvContract.Channels.CONTENT_URI)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_NO_ANIMATION));overridePendingTransition(0,0);}catch(Exception e){message("TV in diretta","Usa il tasto TV del telecomando.");}}
    private String label(TvInputInfo in){String n=inputNames.get(in.getId());return n!=null?n:in.getId();}
    private String readLabel(TvInputInfo in){CharSequence n=in.loadCustomLabel(this);if(n==null||n.toString().trim().isEmpty())n=in.loadLabel(this);return n==null?in.getId():n.toString().trim();}
    private void loadInputs(){inputsDirty=true;ui.removeCallbacks(inputRefresh);if(visible)ui.postDelayed(inputRefresh,180);}
    private void refreshInputs(){
        if(destroyed||!visible||!inputsDirty||inputsLoading)return;inputsDirty=false;inputsLoading=true;
        stateIo.execute(()->{
            try{
                List<TvInputInfo> next=new ArrayList<>();Map<String,String> names=new HashMap<>();Map<String,List<Intent>> routes=new HashMap<>();
                TvInputManager tim=(TvInputManager)getSystemService(TV_INPUT_SERVICE);
                if(tim!=null)for(TvInputInfo in:tim.getTvInputList())if(in.isPassthroughInput()&&in.getParentId()==null&&!in.isHidden(this))next.add(in);
                Collections.sort(next,(a,b)->a.getId().compareTo(b.getId()));TvInputInfo a=null,h1=null,h2=null;
                for(TvInputInfo in:next){
                    String label=readLabel(in);names.put(in.getId(),label);routes.put(in.getId(),buildInputIntents(in.getId()));
                    String name=label.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]","");
                    if(in.getType()==TvInputInfo.TYPE_COMPOSITE||name.matches("av[0-9]*")||name.equals("cvbs")){if(a==null)a=in;}
                    if(in.getType()==TvInputInfo.TYPE_HDMI){CharSequence original=in.loadLabel(this);String physical=original==null?"":original.toString().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]","");String port=physical.matches("hdmi[12](arc|earc)?")?physical:name;if(port.matches("hdmi1(arc|earc)?"))h1=in;if(port.matches("hdmi2(arc|earc)?"))h2=in;}
                }
                final TvInputInfo nextAv=a,nextH1=h1,nextH2=h2;
                final Intent nextTv=null; // Use the system TV contract, without a device-specific package.
                ui.post(()->{inputsLoading=false;if(destroyed)return;inputs.clear();inputs.addAll(next);inputNames.clear();inputNames.putAll(names);inputIntents.clear();inputIntents.putAll(routes);av=nextAv;hdmi1=nextH1;hdmi2=nextH2;liveIntent=nextTv;if(inputsDirty&&visible)ui.postDelayed(inputRefresh,180);});
            }catch(Exception e){ui.post(()->{inputsLoading=false;inputsDirty=true;});}
        });
    }
    private void source(TvInputInfo input,String name){if(input==null){message(name,"La TV non identifica con certezza questo ingresso. Selezionalo da Altre sorgenti o dal tasto ingressi del telecomando.");return;}openInput(input.getId());}
    private void others(){ArrayList<TvInputInfo> other=new ArrayList<>();for(TvInputInfo in:inputs)if(in!=av&&in!=hdmi1&&in!=hdmi2)other.add(in);listSources(other.isEmpty()?inputs:other,"Altre sorgenti");}
    private void listSources(List<TvInputInfo> list,String title){if(list.isEmpty()){message(title,"Nessun ingresso segnalato dalla TV. Usa il tasto ingressi del telecomando.");return;}ArrayList<TvInputInfo> copy=new ArrayList<>(list);String[] labels=new String[copy.size()];for(int i=0;i<labels.length;i++)labels[i]=label(copy.get(i));new AlertDialog.Builder(this).setTitle(title).setItems(labels,(d,n)->openInput(copy.get(n).getId())).setNegativeButton("Chiudi",null).show();}
    private List<Intent> buildInputIntents(String id){
        Intent view=new Intent(Intent.ACTION_VIEW,TvContract.buildChannelUriForPassthroughInput(id));ArrayList<Intent> tries=new ArrayList<>();ResolveInfo def=getPackageManager().resolveActivity(view,PackageManager.MATCH_DEFAULT_ONLY);
        if(def!=null&&def.activityInfo!=null&&"com.google.android.tv.inputplayer".equals(def.activityInfo.packageName))tries.add(new Intent(view).setClassName(def.activityInfo.packageName,def.activityInfo.name));
        String[][] live={{"com.android.tv","com.android.tv.MainActivity"},{"com.droidlogic.android.tv","com.android.tv.TvActivity"}};for(String[] c:live){Intent t=new Intent(view).setClassName(c[0],c[1]);if(t.resolveActivity(getPackageManager())!=null)tries.add(t);}tries.add(view);
        return tries;
    }
    private void openInput(String id){
        List<Intent> routes=inputIntents.get(id);
        if(routes==null)routes=Collections.singletonList(new Intent(Intent.ACTION_VIEW,TvContract.buildChannelUriForPassthroughInput(id)));
        for(Intent route:routes)try{startActivity(new Intent(route).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_NO_ANIMATION));overridePendingTransition(0,0);return;}catch(Exception ignored){}
        message("Ingresso non aperto","Usa il tasto ingressi del telecomando.");
    }
    private void openSettings(boolean wifi){
        String[] actions=wifi?new String[]{Settings.ACTION_WIFI_SETTINGS,Settings.ACTION_WIRELESS_SETTINGS,Settings.ACTION_SETTINGS}:new String[]{Settings.ACTION_SETTINGS};
        for(String action:actions)try{startActivity(new Intent(action).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION));overridePendingTransition(0,0);return;}catch(Exception ignored){}
        message("Impostazioni","Apri le impostazioni dal telecomando.");
    }
    private void watchNetwork(){
        if(destroyed)return;
        try{IntentFilter f=new IntentFilter();f.addAction(WifiManager.RSSI_CHANGED_ACTION);f.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);f.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);if(Build.VERSION.SDK_INT>=33)registerReceiver(netReceiver,f,Context.RECEIVER_EXPORTED);else registerReceiver(netReceiver,f);receiverOn=true;}catch(Exception ignored){}
        try{((ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE)).registerDefaultNetworkCallback(netCallback);networkCallbackOn=true;}catch(Exception ignored){}
        requestNetwork();
    }
    private void requestNetwork(){if(destroyed)return;netDirty=true;ui.removeCallbacks(netRefresh);if(visible)ui.postDelayed(netRefresh,100);}
    private void refreshNetwork(){
        if(destroyed||!visible||netLoading)return;netLoading=true;netDirty=false;
        stateIo.execute(()->{
            int level=NetworkSignal.OFFLINE;String description="Wi-Fi non connesso";
            try{ConnectivityManager cm=(ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);Network n=cm.getActiveNetwork();NetworkCapabilities caps=n==null?null:cm.getNetworkCapabilities(n);
                if(caps!=null&&caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)){level=NetworkSignal.ETHERNET;description="Connessione Ethernet";}
                else if(caps!=null&&caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)){level=NetworkSignal.UNKNOWN;description="Wi-Fi connesso, segnale non disponibile";try{WifiManager wm=(WifiManager)getApplicationContext().getSystemService(WIFI_SERVICE);WifiInfo info=wm.getConnectionInfo();if(info!=null){int rssi=info.getRssi();level=NetworkSignal.wifiLevel(rssi);if(level>=0)description="Wi-Fi: "+level+" su 3 tacche, "+rssi+" dBm";}}catch(Exception ignored){}}
            }catch(Exception ignored){}
            final int signal=level;final String label=description;
            ui.post(()->{netLoading=false;if(destroyed)return;home.setNetwork(signal,label);if(netDirty&&visible)ui.postDelayed(netRefresh,100);});
        });
    }
    private void message(String title,String body){new AlertDialog.Builder(this).setTitle(title).setMessage(body).setPositiveButton("OK",null).show();}
}
