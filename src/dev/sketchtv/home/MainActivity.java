package dev.sketchtv.home;
import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.media.tv.*;
import android.net.*;
import android.net.wifi.*;
import android.os.*;
import android.provider.Settings;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

public class MainActivity extends Activity {
    private HomeView home;
    private WidgetRepository widgets;
    private Map<String,WidgetEngine.State> widgetStates=Collections.emptyMap();
    private final Handler ui=new Handler(Looper.getMainLooper());
    private final Runnable homeOk=()->HomeButton.onHomeOk(this);
    private final ExecutorService stateIo=Executors.newSingleThreadExecutor();
    private boolean inputsDirty=true,inputsLoading=false,netLoading=false,netDirty=true;
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
    private final List<TvInputInfo> inputs=new ArrayList<>();
    private TvInputInfo av,hdmi1,hdmi2;
    private final Runnable minute=new Runnable(){public void run(){if(visible){home.tick();ui.postDelayed(this,60000-System.currentTimeMillis()%60000);}}};
    private final Runnable poll=new Runnable(){public void run(){if(visible){widgets.refresh(false);ui.postDelayed(this,30000L);}}};
    private final TvInputManager.TvInputCallback inputChanged=new TvInputManager.TvInputCallback(){
        @Override public void onInputAdded(String id){loadInputs();}
        @Override public void onInputRemoved(String id){loadInputs();}
        @Override public void onInputUpdated(String id){loadInputs();}
    };
    @Override protected void onCreate(Bundle state){
        super.onCreate(state);
        home=new HomeView(this,this::open);setContentView(home);widgets=new WidgetRepository(this,BuiltinWidgets.create(),next->{widgetStates=next;home.update(next);});loadInputs();ui.postDelayed(()->watchNetwork(),150);
        TvInputManager tim=(TvInputManager)getSystemService(TV_INPUT_SERVICE);if(tim!=null)tim.registerCallback(inputChanged,ui);
    }
    @Override protected void onResume(){super.onResume();visible=true;widgets.setActive(true);home.tick();if(inputsDirty)ui.postDelayed(inputRefresh,180);requestNetwork();ui.post(minute);ui.postDelayed(poll,400);overridePendingTransition(0,0);HomeButton.onHomeResumed(this);ui.postDelayed(homeOk,1500);}
    @Override protected void onPause(){visible=false;widgets.setActive(false);ui.removeCallbacks(minute);ui.removeCallbacks(poll);ui.removeCallbacks(homeOk);ui.removeCallbacks(inputRefresh);ui.removeCallbacks(netRefresh);HomeButton.onHomeOk(this);super.onPause();}
    @Override protected void onDestroy(){destroyed=true;visible=false;ui.removeCallbacksAndMessages(null);widgets.close();stateIo.shutdownNow();if(receiverOn)unregisterReceiver(netReceiver);if(networkCallbackOn)((ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE)).unregisterNetworkCallback(netCallback);TvInputManager tim=(TvInputManager)getSystemService(TV_INPUT_SERVICE);if(tim!=null)tim.unregisterCallback(inputChanged);home.dispose();super.onDestroy();}
    @Override public void onBackPressed(){ /* Stay on the home screen. */ }
    @Override public boolean onKeyUp(int key,android.view.KeyEvent event){if(key==android.view.KeyEvent.KEYCODE_MENU){settings();return true;}return super.onKeyUp(key,event);}
    private void open(String action){
        if(action.startsWith("widget:")){details(action.substring(7));return;}
        if(action.startsWith("app:")){launch(action.substring(4));return;}
        switch(action){
            case "apps":startActivity(new Intent(this,AppsActivity.class).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION));overridePendingTransition(0,0);break;
            case "settings":openSettings(false);break;
            case "network":openSettings(true);break;
            case "tv":liveTv();break;
            case "av":source(av,"AV");break;
            case "hdmi1":source(hdmi1,"HDMI 1");break;
            case "hdmi2":source(hdmi2,"HDMI 2");break;
            case "sources":others();break;
            default:message("Azione non disponibile",action);
        }
    }
    private void settings(){new AlertDialog.Builder(this).setTitle("Sketch TV").setItems(new String[]{"Impostazioni TV","Aggiorna dati","Tutte le sorgenti","Home originale"},(d,n)->{if(n==0)openSettings(false);else if(n==1)widgets.refresh(true);else if(n==2)listSources(inputs,"Tutte le sorgenti");else HomeButton.openGoogleTv(this);}).setNegativeButton("Chiudi",null).show();}
    private void details(String id){
        WidgetEngine.State state=widgetStates.get(id);if(state==null){message("Widget","Widget non registrato");return;}
        SimpleDateFormat date=new SimpleDateFormat("dd/MM/yyyy HH:mm",Locale.forLanguageTag(Config.LOCALE));date.setTimeZone(TimeZone.getTimeZone(Config.TIME_ZONE));
        StringBuilder text=new StringBuilder(state.status(System.currentTimeMillis())).append("\n\n");
        if(state.content!=null){text.append(state.content.title).append("\n");for(String line:state.content.lines)text.append(line).append("\n");}
        text.append("\nDownload: ").append(state.fetched==0?"mai":date.format(new Date(state.fetched))).append("\nFuso orario: ").append(Config.TIME_ZONE).append("\nAggiornamento: ").append(state.provider.intervalMillis()/60000).append(" minuti con Home visibile.\n\n");
        for(WidgetProvider.Source source:state.provider.sources())text.append(source.name).append("\n").append(source.url).append("\n").append(source.license).append("\n\n");
        text.append("La data di download non garantisce che la fonte abbia pubblicato nuovi dati. I risultati sportivi non sono in diretta.");
        new AlertDialog.Builder(this).setTitle(state.provider.title()).setMessage(text.toString()).setPositiveButton("Chiudi",null).setNeutralButton("Aggiorna",(d,n)->widgets.refresh(true)).show();
    }
    private void launch(String pkg){if(pkg.isEmpty()){message("Scorciatoia da configurare","Scegli il package dell’app in project.json, oppure apri Altre app.");return;}Intent i=getPackageManager().getLeanbackLaunchIntentForPackage(pkg);if(i==null)i=getPackageManager().getLaunchIntentForPackage(pkg);if(i==null){message("App non disponibile","L'app non è installata o non offre un avvio compatibile.");return;}try{startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_NO_ANIMATION));}catch(Exception e){message("Avvio non riuscito","Apri l'app dalla Home originale.");}}
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
        try{if(Build.VERSION.SDK_INT>=24)((ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE)).registerDefaultNetworkCallback(netCallback);networkCallbackOn=Build.VERSION.SDK_INT>=24;}catch(Exception ignored){}
        requestNetwork();
    }
    private void requestNetwork(){if(destroyed)return;netDirty=true;ui.removeCallbacks(netRefresh);if(visible)ui.postDelayed(netRefresh,100);}
    private void refreshNetwork(){
        if(destroyed||!visible||netLoading)return;netLoading=true;netDirty=false;
        stateIo.execute(()->{
            int level=NetworkSignal.OFFLINE;String description="Wi-Fi non connesso";
            try{ConnectivityManager cm=(ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);Network n=Build.VERSION.SDK_INT>=23?cm.getActiveNetwork():null;NetworkCapabilities caps=n==null?null:cm.getNetworkCapabilities(n);
                if(caps!=null&&caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)){level=NetworkSignal.ETHERNET;description="Connessione Ethernet";}
                else if(caps!=null&&caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)){level=NetworkSignal.UNKNOWN;description="Wi-Fi connesso, segnale non disponibile";try{WifiManager wm=(WifiManager)getApplicationContext().getSystemService(WIFI_SERVICE);WifiInfo info=wm.getConnectionInfo();if(info!=null){int rssi=info.getRssi();level=NetworkSignal.wifiLevel(rssi);if(level>=0)description="Wi-Fi: "+level+" su 3 tacche, "+rssi+" dBm";}}catch(Exception ignored){}}
            }catch(Exception ignored){}
            final int signal=level;final String label=description;
            ui.post(()->{netLoading=false;if(destroyed)return;home.setNetwork(signal,label);if(netDirty&&visible)ui.postDelayed(netRefresh,100);});
        });
    }
    private void message(String title,String body){new AlertDialog.Builder(this).setTitle(title).setMessage(body).setPositiveButton("OK",null).show();}
}
