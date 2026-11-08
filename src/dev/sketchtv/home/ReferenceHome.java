package dev.sketchtv.home;
import android.content.Context;
import android.graphics.*;
import android.view.*;
import android.widget.FrameLayout;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.*;

/** One screen with real native focus/click targets, rendered in reference coordinates. */
final class ReferenceHome extends FrameLayout {
    interface Actions {void open(int index);}
    private static final float[][] BOXES={
        {108,155,790,595},{886,174,1318,558},{1380,174,1812,558},
        {108,617,455,821},{478,617,825,821},{848,617,1195,821},{1218,617,1565,821},{1588,617,1812,821},
        {108,869,431,973},{454,869,777,973},{800,869,1123,973},{1146,869,1469,973},{1492,869,1812,973},
        {1341,28,1565,136},{1588,28,1812,136}
    };
    private static final String[] LABELS={"Orario, meteo e impostazioni",Config.CONSTRUCTOR_NAME,Config.TEAM_NAME,Config.APP_LABELS[0],Config.APP_LABELS[1],Config.APP_LABELS[2],Config.APP_LABELS[3],"Altre app","TV in diretta","AV","HDMI 1","HDMI 2","Altre sorgenti","Impostazioni TV","Wi-Fi"};
    final String[][] data=new String[5][];
    final boolean[] errors=new boolean[5];
    final long[] fetched=new long[5];
    private final View[] buttons=new View[BOXES.length];
    private final Bitmap[] logos=new Bitmap[4],icons=new Bitmap[7];
    private final SimpleDateFormat clock=new SimpleDateFormat("HH:mm",Locale.ITALY),date=new SimpleDateFormat("EEEE, d MMMM yyyy",Locale.ITALY);
    private final Date now=new Date();
    private final Path[] logoClips=new Path[4];
    private final DashPathEffect dashed=new DashPathEffect(new float[]{12,6},0);
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private final Typeface regular,bold;
    private final RectF tmp=new RectF();
    private final Shader bg=new LinearGradient(0,0,1920,0,new int[]{0xFF000000,0xFF151517,0xFF2D2D33},null,Shader.TileMode.CLAMP);
    private final Shader red=new LinearGradient(886,0,1318,0,0xFF9D1215,0xFFFF7478,Shader.TileMode.CLAMP);
    private final Shader orange=new LinearGradient(1380,0,1812,0,0xFFFF3135,0xFFFFCB45,Shader.TileMode.CLAMP);
    private float zoom=1,dx=0,dy=0;
    private int selected=3;
    private int network=NetworkSignal.UNKNOWN;
    private long shownMinute=-1;
    private String timeText="",dateText="";
    private final Path gear=gearPath(1453,82);
    private final LineIcon[] sourceIcons={new LineIcon(LineIcon.TV),new LineIcon(LineIcon.AV),new LineIcon(LineIcon.HDMI),new LineIcon(LineIcon.HDMI),new LineIcon(LineIcon.SOURCE)};
    private final LineIcon ethernet=new LineIcon(LineIcon.ETHERNET);
    ReferenceHome(Context c,Actions actions){
        super(c);TimeZone zone=TimeZone.getTimeZone(Config.TIME_ZONE);clock.setTimeZone(zone);date.setTimeZone(zone);setWillNotDraw(false);setClipChildren(false);setClipToPadding(false);
        regular=Typeface.createFromAsset(c.getAssets(),"Poppins-Regular.ttf");bold=Typeface.createFromAsset(c.getAssets(),"Poppins-Bold.ttf");
        for(int i=0;i<4;i++){float[] b=BOXES[i+3];logoClips[i]=new Path();logoClips[i].addRoundRect(new RectF(b[0],b[1],b[2],b[3]),24,24,Path.Direction.CW);}
        String[] names={"netflix","prime","youtube","spotify"};for(int i=0;i<4;i++)logos[i]=bitmap(names[i]);
        String[] symbols={"race","ball","tv","av","hdmi1","hdmi2","sources"};for(int i=0;i<7;i++)icons[i]=bitmap(symbols[i]);
        for(int i=0;i<buttons.length;i++){
            final int index=i;View v=new View(c);v.setFocusable(true);v.setClickable(true);v.setContentDescription(LABELS[i]);
            v.setOnFocusChangeListener((view,has)->{if(has){selected=index;invalidate();}});
            v.setOnClickListener(view->actions.open(index));buttons[i]=v;addView(v,new FrameLayout.LayoutParams(1,1));
        }
        ethernet.setColorFilter(new PorterDuffColorFilter(0xFF191919,PorterDuff.Mode.SRC_IN));
        tick();post(()->buttons[3].requestFocus());
    }
    void tick(){
        long minute=System.currentTimeMillis()/60000;if(minute==shownMinute)return;shownMinute=minute;now.setTime(System.currentTimeMillis());timeText=clock.format(now);String day=date.format(now);dateText=day.substring(0,1).toUpperCase(Locale.ITALY)+day.substring(1);invalidate();
    }
    void setNetwork(int level,String label){buttons[14].setContentDescription(label+". Apri impostazioni rete");if(network==level)return;network=level;float[] b=BOXES[14];invalidate((int)(dx+(b[0]-8)*zoom),(int)(dy+(b[1]-8)*zoom),(int)(dx+(b[2]+8)*zoom),(int)(dy+(b[3]+8)*zoom));}
    private static Path gearPath(float x,float y){
        Path path=new Path();path.setFillType(Path.FillType.EVEN_ODD);
        for(int i=0;i<32;i++){double a=i*Math.PI/16;float r=(i%4==1||i%4==2)?34:27;float px=x+(float)Math.cos(a)*r,py=y+(float)Math.sin(a)*r;if(i==0)path.moveTo(px,py);else path.lineTo(px,py);}path.close();path.addCircle(x,y,14,Path.Direction.CW);return path;
    }
    private void drawNetwork(Canvas c){
        if(network==NetworkSignal.ETHERNET){ethernet.setBounds(1666,48,1734,116);ethernet.draw(c);return;}
        float x=1700,y=107;p.setShader(null);p.setStrokeCap(Paint.Cap.BUTT);p.setStrokeWidth(11);p.setStyle(Paint.Style.STROKE);
        for(int k=1;k<=3;k++){float r=26+(k-1)*18;p.setColor(network>=k?0xFF191919:0xFFA0A0A0);tmp.set(x-r,y-r,x+r,y+r);c.drawArc(tmp,225,90,false,p);}
        p.setStyle(Paint.Style.FILL);p.setColor(network>=0?0xFF191919:0xFFA0A0A0);
        if(network==NetworkSignal.UNKNOWN)text(c,"?",x-7,y+7,20,true,0xFF191919,20);else c.drawCircle(x,y,6,p);
        if(network==NetworkSignal.OFFLINE){p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(8);p.setColor(0xFF191919);c.drawLine(x-44,y-60,x+44,y+8,p);p.setStyle(Paint.Style.FILL);}
    }
    private void centered(Canvas c,String label,float x,float y,float size){p.setTypeface(bold);p.setTextSize(size);float width=p.measureText(label);text(c,label,x-width/2,y,size,true,0xFF191919,width+1);}
    private Bitmap bitmap(String name){try(InputStream in=getContext().getAssets().open(name+".png")){BitmapFactory.Options o=new BitmapFactory.Options();o.inPreferredConfig=Bitmap.Config.ARGB_8888;if(name.equals("netflix")||name.equals("prime")||name.equals("youtube")||name.equals("spotify"))o.inSampleSize=2;return BitmapFactory.decodeStream(in,null,o);}catch(Exception e){return null;}}
    @Override protected void onSizeChanged(int w,int h,int ow,int oh){
        super.onSizeChanged(w,h,ow,oh);zoom=Math.min(w/1920f,h/1080f);dx=(w-1920*zoom)/2;dy=(h-1080*zoom)/2;
        for(int i=0;i<buttons.length;i++){float[] b=BOXES[i];FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(Math.max(1,Math.round((b[2]-b[0])*zoom)),Math.max(1,Math.round((b[3]-b[1])*zoom)));lp.leftMargin=Math.round(dx+b[0]*zoom);lp.topMargin=Math.round(dy+b[1]*zoom);buttons[i].setLayoutParams(lp);}
    }
    void refresh(){
        buttons[0].setContentDescription("Orario e meteo: "+value(0,0,"non disponibile")+". Apri dettagli e impostazioni");
        buttons[1].setContentDescription(Config.CONSTRUCTOR_NAME+", costruttori "+value(2,0,"non disponibile")+", "+value(1,1,"")+" "+value(1,2,"")+", "+value(1,3,"")+" "+value(1,4,""));
        buttons[2].setContentDescription(value(3,0,Config.TEAM_NAME)+" "+value(3,1,"")+", "+value(3,2,"")+" "+value(3,3,"")+", classifica "+value(4,0,"non disponibile"));invalidate();
    }
    private String value(int row,int col,String fallback){return data[row]!=null&&col<data[row].length?data[row][col]:fallback;}
    private void text(Canvas c,String text,float x,float y,float size,boolean heavy,int color,float maxWidth){
        p.setShader(null);p.setStyle(Paint.Style.FILL);p.setColor(color);p.setTypeface(heavy?bold:regular);p.setTextAlign(Paint.Align.LEFT);p.setTextSize(size);
        while(p.measureText(text)>maxWidth&&p.getTextSize()>18)p.setTextSize(p.getTextSize()-1);
        if(p.measureText(text)>maxWidth){while(text.length()>1&&p.measureText(text+"…")>maxWidth)text=text.substring(0,text.length()-1);text+="…";}
        c.drawText(text,x,y,p);
    }
    private void right(Canvas c,String text,float x,float y,float size,float maxWidth){
        p.setTypeface(bold);p.setTextSize(size);while(p.measureText(text)>maxWidth&&p.getTextSize()>18)p.setTextSize(p.getTextSize()-1);
        float width=Math.min(maxWidth,p.measureText(text));text(c,text,x-width,y,p.getTextSize(),true,Color.WHITE,maxWidth);
    }
    private void image(Canvas c,Bitmap b,float l,float t,float r,float bottom){if(b==null)return;p.setShader(null);p.setStyle(Paint.Style.FILL);p.setColor(Color.WHITE);tmp.set(l,t,r,bottom);c.drawBitmap(b,null,tmp,p);}
    private void symbol(Canvas c,Bitmap b,float l,float t,float w,float h){if(b==null)return;float s=Math.min(w/b.getWidth(),h/b.getHeight());float bw=b.getWidth()*s,bh=b.getHeight()*s;image(c,b,l+(w-bw)/2,t+(h-bh)/2,l+(w+bw)/2,t+(h+bh)/2);}
    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);canvas.drawColor(Color.BLACK);int save=canvas.save();canvas.translate(dx,dy);canvas.scale(zoom,zoom);
        p.setStyle(Paint.Style.FILL);p.setShader(bg);canvas.drawRect(0,0,1920,1080,p);
        text(canvas,timeText,108,391,300,true,Color.WHITE,670);
        text(canvas,dateText,108,487,46,false,Color.WHITE,675);
        text(canvas,Config.CITY+" – "+value(0,0,"—"),108,546,46,false,Color.WHITE,675);
        if(errors[0]||stale(0))text(canvas,"Meteo salvato · seleziona per data e dettagli",108,583,18,false,0xFFBFC3CD,675);
        p.setShader(red);tmp.set(886,174,1318,558);canvas.drawRoundRect(tmp,48,48,p);
        p.setShader(orange);tmp.set(1380,174,1812,558);canvas.drawRoundRect(tmp,48,48,p);
        p.setShader(null);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(4);p.setPathEffect(dashed);
        p.setColor(0xFFE4DDDB);tmp.set(886,174,1318,558);canvas.drawRoundRect(tmp,48,48,p);
        p.setColor(0xFF293D50);tmp.set(1380,174,1812,558);canvas.drawRoundRect(tmp,48,48,p);
        p.setPathEffect(null);p.setStyle(Paint.Style.FILL);
        symbol(canvas,icons[0],926,209,176,55);symbol(canvas,icons[1],1420,209,80,80);
        text(canvas,"Formula 1",926,322,48,true,Color.WHITE,285);text(canvas,Config.CONSTRUCTOR_NAME,926,371,48,true,Color.WHITE,285);
        text(canvas,"Costruttori",1195,294,16,true,Color.WHITE,90);
        right(canvas,"P"+value(2,0,"—"),1278,346,42,90);
        text(canvas,value(1,0,"Ultimo GP · dati non disponibili"),926,409,20,true,Color.WHITE,352);
        text(canvas,value(1,1,"Pilota 1"),926,462,40,true,Color.WHITE,245);right(canvas,value(1,2,"—"),1278,462,40,90);
        text(canvas,value(1,3,"Pilota 2"),926,511,40,true,Color.WHITE,245);right(canvas,value(1,4,"—"),1278,511,40,90);
        text(canvas,Config.LEAGUE_ID.isEmpty()?"Calcio":Config.LEAGUE_ID,1420,329,20,true,Color.WHITE,300);
        text(canvas,value(3,0,Config.TEAM_NAME),1420,383,44,true,Color.WHITE,280);right(canvas,value(3,1,"—"),1772,383,44,60);
        text(canvas,value(3,2,"Dati non disponibili"),1420,431,44,true,Color.WHITE,280);right(canvas,value(3,3,"—"),1772,431,44,60);
        text(canvas,"Posizione in classifica: "+value(4,0,"—")+(data[4]!=null?"ª":""),1420,511,20,true,Color.WHITE,352);
        if(errors[1]||errors[2]||stale(1)||stale(2))text(canvas,"Dati salvati · verifica dettagli",926,543,16,false,Color.WHITE,352);
        if(errors[3]||errors[4]||stale(3)||stale(4))text(canvas,"Dati salvati · verifica dettagli",1420,543,16,false,Color.WHITE,352);
        for(int i=0;i<4;i++){
            float[] b=BOXES[i+3];tmp.set(b[0],b[1],b[2],b[3]);p.setShader(null);p.setColor(Color.WHITE);canvas.drawRoundRect(tmp,24,24,p);
            int clip=canvas.save();canvas.clipPath(logoClips[i]);
            if(logos[i]!=null)symbol(canvas,logos[i],b[0],b[1],b[2]-b[0],b[3]-b[1]);else centered(canvas,Config.APP_LABELS[i],(b[0]+b[2])/2,b[1]+115,36);canvas.restoreToCount(clip);
        }
        for(int i=0;i<5;i++){
            float[] b=BOXES[i+8];p.setShader(null);p.setStyle(Paint.Style.FILL);p.setColor(0xFFD9D9D9);tmp.set(b[0],b[1],b[2],b[3]);canvas.drawRoundRect(tmp,24,24,p);
            LineIcon sourceIcon=sourceIcons[i];sourceIcon.setColorFilter(new PorterDuffColorFilter(0xFF191919,PorterDuff.Mode.SRC_IN));sourceIcon.setBounds((int)b[0]+30,(int)b[1]+15,(int)b[0]+104,(int)b[1]+89);sourceIcon.draw(canvas);
            if(i<2)text(canvas,i==0?"tv":"av",b[0]+(i==0?184:168),b[1]+74,62,true,0xFF191919,145);
            else if(i<4)text(canvas,"HDMI "+(i-1),b[0]+135,b[1]+74,34,true,0xFF191919,163);
            else {text(canvas,"altre",b[0]+135,b[1]+47,34,true,0xFF191919,176);text(canvas,"sorgenti",b[0]+135,b[1]+82,34,true,0xFF191919,176);}
        }
        p.setShader(null);p.setStyle(Paint.Style.FILL);p.setColor(0xFFD9D9D9);tmp.set(1588,617,1812,821);canvas.drawRoundRect(tmp,24,24,p);
        centered(canvas,"altre",1700,712,54);centered(canvas,"app",1700,765,54);
        p.setShader(null);p.setStyle(Paint.Style.FILL);p.setColor(0xFFD9D9D9);tmp.set(1341,28,1565,136);canvas.drawRoundRect(tmp,24,24,p);tmp.set(1588,28,1812,136);canvas.drawRoundRect(tmp,24,24,p);
        p.setColor(0xFF191919);canvas.drawPath(gear,p);drawNetwork(canvas);
        if(hasFocus()){
            float[] b=BOXES[selected];tmp.set(b[0]-7,b[1]-7,b[2]+7,b[3]+7);p.setShader(null);p.setColor(0xFF8FCBFF);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(5);canvas.drawRoundRect(tmp,selected<3?50:28,selected<3?50:28,p);p.setStyle(Paint.Style.FILL);
        }
        canvas.restoreToCount(save);
    }
    private boolean stale(int i){return fetched[i]>0&&System.currentTimeMillis()-fetched[i]>60*60*1000L;}
    void dispose(){for(Bitmap b:logos)if(b!=null)b.recycle();for(Bitmap b:icons)if(b!=null)b.recycle();}
}
