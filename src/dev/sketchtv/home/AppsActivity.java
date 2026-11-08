package dev.sketchtv.home;
import android.app.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.util.LruCache;
import android.view.*;
import android.widget.*;
import java.util.*;
import java.util.concurrent.*;

/** Automatically refreshed, paged native app catalog. Icons are loaded only for the visible page. */
public class AppsActivity extends Activity {
    private static final int PER_PAGE=8;
    private final ExecutorService loader=Executors.newSingleThreadExecutor();
    private final Handler ui=new Handler(Looper.getMainLooper());
    private final LruCache<String,Bitmap> icons=new LruCache<String,Bitmap>(1024*1024){@Override protected int sizeOf(String key,Bitmap b){return b.getByteCount();}};
    private final List<AppCatalog.Entry> apps=new ArrayList<>();
    private volatile boolean destroyed=false;
    private volatile int generation=0;
    private boolean visible=false,loading=false,dirty=true,receiverOn=false;
    private int page=0;private float scale=1;
    private String focusedPackage;
    private Typeface regular,bold;
    private LinearLayout grid;
    private TextView status,previous,next;
    private final Runnable refresh=()->scan();
    private final BroadcastReceiver changed=new BroadcastReceiver(){@Override public void onReceive(Context c,Intent i){dirty=true;if(visible)schedule();}};
    private int px(int v){return Math.round(v*scale);}
    private TextView text(String label,int size,boolean heavy){TextView t=new TextView(this);t.setText(label);t.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,px(size));t.setTypeface(heavy?bold:regular);t.setTextColor(Color.WHITE);return t;}
    private GradientDrawable surface(){GradientDrawable bg=new GradientDrawable();bg.setColor(0xFFEEEEEE);bg.setCornerRadius(px(24));return bg;}
    private void focusStyle(View view,GradientDrawable bg){view.setFocusable(true);view.setClickable(true);view.setBackground(bg);view.setOnFocusChangeListener((v,has)->bg.setStroke(px(has?5:0),0xFF8FCBFF));}
    @Override protected void onCreate(Bundle state){
        super.onCreate(state);scale=Math.min(getResources().getDisplayMetrics().widthPixels/1920f,getResources().getDisplayMetrics().heightPixels/1080f);
        regular=Typeface.createFromAsset(getAssets(),"Poppins-Regular.ttf");bold=Typeface.createFromAsset(getAssets(),"Poppins-Bold.ttf");
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(px(108),px(70),px(108),px(70));root.setBackground(new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,new int[]{0xFF000000,0xFF2D2D33}));
        root.addView(text("Altre app",58,true));status=text("Caricamento delle app installate…",26,false);root.addView(status);
        grid=new LinearLayout(this);grid.setOrientation(LinearLayout.VERTICAL);LinearLayout.LayoutParams gp=new LinearLayout.LayoutParams(-1,px(524));gp.topMargin=px(40);root.addView(grid,gp);
        LinearLayout footer=new LinearLayout(this);footer.setGravity(Gravity.CENTER_VERTICAL);LinearLayout.LayoutParams fp=new LinearLayout.LayoutParams(-1,px(74));fp.topMargin=px(40);root.addView(footer,fp);
        previous=button("◀ Precedente",()->{if(page>0){page--;render();}});next=button("Successiva ▶",()->{if((page+1)*PER_PAGE<apps.size()){page++;render();}});TextView back=button("Torna alla Home",this::finish);
        for(TextView v:new TextView[]{previous,next,back}){LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-1,1);if(v!=previous)lp.leftMargin=px(24);footer.addView(v,lp);}
        setContentView(root);
        IntentFilter filter=new IntentFilter();filter.addAction(Intent.ACTION_PACKAGE_ADDED);filter.addAction(Intent.ACTION_PACKAGE_REMOVED);filter.addAction(Intent.ACTION_PACKAGE_CHANGED);filter.addAction(Intent.ACTION_PACKAGE_REPLACED);filter.addDataScheme("package");
        if(Build.VERSION.SDK_INT>=33)registerReceiver(changed,filter,Context.RECEIVER_EXPORTED);else registerReceiver(changed,filter);receiverOn=true;
    }
    private TextView button(String title,Runnable action){TextView t=text(title,28,true);t.setTextColor(0xFF191919);t.setGravity(Gravity.CENTER);focusStyle(t,surface());t.setOnClickListener(v->action.run());return t;}
    @Override protected void onResume(){super.onResume();visible=true;overridePendingTransition(0,0);dirty=true;schedule();}
    @Override protected void onPause(){visible=false;ui.removeCallbacks(refresh);super.onPause();}
    private void schedule(){ui.removeCallbacks(refresh);if(!destroyed&&visible)ui.postDelayed(refresh,180);}
    private void scan(){
        if(destroyed||!visible||loading||!dirty)return;dirty=false;loading=true;
        loader.execute(()->{try{List<AppCatalog.Entry> result=AppCatalog.read(getPackageManager());ui.post(()->{loading=false;if(destroyed)return;apps.clear();apps.addAll(result);page=Math.min(page,Math.max(0,(apps.size()-1)/PER_PAGE));if(visible)render();if(dirty&&visible)schedule();});}catch(Exception e){ui.post(()->{loading=false;if(!destroyed)status.setText("Elenco non disponibile. Riapri Altre app per riprovare.");});}});
    }
    private void render(){
        int token=++generation;grid.removeAllViews();int pages=Math.max(1,(apps.size()+PER_PAGE-1)/PER_PAGE);page=Math.min(page,pages-1);status.setText(apps.size()+" app · Pagina "+(page+1)+" di "+pages+" · Aggiornamento automatico");
        previous.setEnabled(page>0);previous.setFocusable(page>0);previous.setAlpha(page>0?1f:0.4f);next.setEnabled(page+1<pages);next.setFocusable(page+1<pages);next.setAlpha(page+1<pages?1f:0.4f);
        View first=null,restore=null;
        for(int row=0;row<2;row++){
            LinearLayout line=new LinearLayout(this);LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-1,px(250));if(row>0)rp.topMargin=px(24);grid.addView(line,rp);
            for(int col=0;col<4;col++){
                int at=page*PER_PAGE+row*4+col;LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(0,-1,1);if(col>0)cp.leftMargin=px(24);
                if(at>=apps.size()){line.addView(new View(this),cp);continue;}
                AppCatalog.Entry app=apps.get(at);LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setGravity(Gravity.CENTER);card.setPadding(px(16),px(16),px(16),px(16));GradientDrawable bg=surface();focusStyle(card,bg);card.setContentDescription(app.label+(app.intent==null?", informazioni app":""));
                card.setOnFocusChangeListener((v,has)->{bg.setStroke(px(has?5:0),0xFF8FCBFF);if(has)focusedPackage=app.pkg;});card.setOnClickListener(v->launch(app));
                ImageView image=new ImageView(this);image.setScaleType(ImageView.ScaleType.FIT_CENTER);card.addView(image,new LinearLayout.LayoutParams(px(110),px(110)));
                TextView label=text(app.label+(app.intent==null?" · Info app":""),28,true);label.setTextColor(0xFF191919);label.setGravity(Gravity.CENTER);label.setMaxLines(2);label.setEllipsize(android.text.TextUtils.TruncateAt.END);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=px(12);card.addView(label,lp);line.addView(card,cp);
                if(first==null)first=card;if(app.pkg.equals(focusedPackage))restore=card;loadIcon(app.pkg,image,token);
            }
        }
        if(restore!=null)restore.requestFocus();else if(first!=null)first.requestFocus();
        if(apps.isEmpty())status.setText("Nessuna app disponibile. Apri Google Play dalle impostazioni della TV.");
    }
    private void loadIcon(String pkg,ImageView image,int token){
        Bitmap cached=icons.get(pkg);if(cached!=null){image.setImageBitmap(cached);return;}
        loader.execute(()->{if(destroyed||token!=generation)return;try{Drawable d=getPackageManager().getApplicationIcon(pkg);Bitmap b=Bitmap.createBitmap(128,128,Bitmap.Config.ARGB_8888);int iw=Math.max(1,d.getIntrinsicWidth()),ih=Math.max(1,d.getIntrinsicHeight());float fit=Math.min(128f/iw,128f/ih);int w=Math.round(iw*fit),h=Math.round(ih*fit);d.setBounds((128-w)/2,(128-h)/2,(128+w)/2,(128+h)/2);d.draw(new Canvas(b));ui.post(()->{if(destroyed){b.recycle();return;}icons.put(pkg,b);if(token==generation)image.setImageBitmap(b);});}catch(Exception ignored){}});
    }
    private void launch(AppCatalog.Entry app){
        Intent intent=app.intent==null?new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+app.pkg)):new Intent(app.intent).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try{startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION));overridePendingTransition(0,0);}catch(Exception e){dirty=true;schedule();new AlertDialog.Builder(this).setTitle(app.label).setMessage("Impossibile aprire l'app. L'elenco verra aggiornato.").setPositiveButton("OK",null).show();}
    }
    @Override public void finish(){super.finish();overridePendingTransition(0,0);}
    @Override protected void onDestroy(){destroyed=true;generation++;ui.removeCallbacksAndMessages(null);if(receiverOn)unregisterReceiver(changed);loader.shutdownNow();icons.evictAll();super.onDestroy();}
}
