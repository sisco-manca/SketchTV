package dev.sketchtv.home;
import android.content.Context;
import android.graphics.*;
import android.view.*;
import android.widget.FrameLayout;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.*;

/** One generated rectangle drives both Canvas drawing and the native focus/click target. */
final class HomeView extends FrameLayout {
    interface Actions {void open(String action);}
    private final View[] buttons=new View[LayoutSpec.TILES.length];
    private final Map<String,Bitmap> images=new HashMap<>();
    private Map<String,WidgetEngine.State> states=Collections.emptyMap();
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private final Typeface regular,bold;
    private final RectF rect=new RectF();
    private final Path clip=new Path(),gear=new Path();
    private final Shader background=new LinearGradient(0,0,1920,0,Config.BACKGROUND,Config.BACKGROUND_END,Shader.TileMode.CLAMP);
    private final SimpleDateFormat clock,date;
    private final Date now=new Date();
    private final Map<String,LineIcon> icons=new HashMap<>();
    private float scale=1,offsetX,offsetY;
    private int selected=0,network=NetworkSignal.UNKNOWN;
    private long minute=-1;
    private String timeText="",dateText="";

    HomeView(Context c,Actions actions){
        super(c);setWillNotDraw(false);setClipChildren(false);
        regular=Typeface.createFromAsset(c.getAssets(),"Poppins-Regular.ttf");bold=Typeface.createFromAsset(c.getAssets(),"Poppins-Bold.ttf");
        Locale locale=Locale.forLanguageTag(Config.LOCALE);clock=new SimpleDateFormat("HH:mm",locale);date=new SimpleDateFormat("EEEE, d MMMM yyyy",locale);TimeZone zone=TimeZone.getTimeZone(Config.TIME_ZONE);clock.setTimeZone(zone);date.setTimeZone(zone);
        icons.put("tv",new LineIcon(LineIcon.TV));icons.put("av",new LineIcon(LineIcon.AV));icons.put("hdmi1",new LineIcon(LineIcon.HDMI));icons.put("hdmi2",new LineIcon(LineIcon.HDMI));icons.put("sources",new LineIcon(LineIcon.SOURCE));icons.put("ethernet",new LineIcon(LineIcon.ETHERNET));
        for(LineIcon icon:icons.values())icon.setColorFilter(new PorterDuffColorFilter(Config.CARD_TEXT,PorterDuff.Mode.SRC_IN));
        gear.setFillType(Path.FillType.EVEN_ODD);for(int i=0;i<32;i++){double a=i*Math.PI/16;float r=(i%4==1||i%4==2)?34:27;float x=(float)Math.cos(a)*r,y=(float)Math.sin(a)*r;if(i==0)gear.moveTo(x,y);else gear.lineTo(x,y);}gear.close();gear.addCircle(0,0,14,Path.Direction.CW);
        for(int i=0;i<buttons.length;i++){
            LayoutSpec.Tile t=LayoutSpec.TILES[i];final int index=i;View v=new View(c);v.setId(View.generateViewId());v.setFocusable(true);v.setClickable(true);v.setContentDescription(t.label);
            v.setOnFocusChangeListener((view,has)->{if(has){selected=index;invalidate();}});v.setOnClickListener(view->actions.open(t.action));buttons[i]=v;addView(v,new FrameLayout.LayoutParams(1,1));
            if(!t.asset.isEmpty()&&!images.containsKey(t.asset))images.put(t.asset,load(t.asset));
        }
        for(int i=0;i<buttons.length;i++){int[] n=LayoutSpec.NEIGHBORS[i];buttons[i].setNextFocusLeftId(buttons[n[0]].getId());buttons[i].setNextFocusUpId(buttons[n[1]].getId());buttons[i].setNextFocusRightId(buttons[n[2]].getId());buttons[i].setNextFocusDownId(buttons[n[3]].getId());}
        for(int i=0;i<buttons.length;i++)if(LayoutSpec.TILES[i].kind.equals("app")){selected=i;break;}
        tick();post(()->buttons[selected].requestFocus());
    }
    private Bitmap load(String name){try(InputStream in=getContext().getAssets().open(name)){BitmapFactory.Options o=new BitmapFactory.Options();o.inJustDecodeBounds=true;BitmapFactory.decodeStream(in,null,o);int sample=1;while(o.outWidth/sample>640||o.outHeight/sample>400)sample*=2;try(InputStream data=getContext().getAssets().open(name)){o.inJustDecodeBounds=false;o.inSampleSize=sample;return BitmapFactory.decodeStream(data,null,o);}}catch(Exception e){return null;}}
    void update(Map<String,WidgetEngine.State> next){states=next;for(int i=0;i<buttons.length;i++){LayoutSpec.Tile t=LayoutSpec.TILES[i];WidgetEngine.State s=state(t);buttons[i].setContentDescription(t.label+(s==null?"":". "+s.status(System.currentTimeMillis())+(s.content==null?"":". "+android.text.TextUtils.join(". ",s.content.lines))));}invalidate();}
    void tick(){long m=System.currentTimeMillis()/60000;if(m==minute)return;minute=m;now.setTime(System.currentTimeMillis());timeText=clock.format(now);dateText=date.format(now);invalidate();}
    void setNetwork(int value,String description){boolean changed=network!=value;network=value;for(int i=0;i<buttons.length;i++)if(LayoutSpec.TILES[i].action.equals("network"))buttons[i].setContentDescription(description+". Apri impostazioni rete");if(changed)invalidate();}
    private WidgetEngine.State state(LayoutSpec.Tile t){return t.action.startsWith("widget:")?states.get(t.action.substring(7)):null;}
    @Override protected void onSizeChanged(int w,int h,int ow,int oh){super.onSizeChanged(w,h,ow,oh);scale=Math.min(w/1920f,h/1080f);offsetX=(w-1920*scale)/2;offsetY=(h-1080*scale)/2;for(int i=0;i<buttons.length;i++){LayoutSpec.Tile t=LayoutSpec.TILES[i];FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(Math.max(1,Math.round(t.w*scale)),Math.max(1,Math.round(t.h*scale)));lp.leftMargin=Math.round(offsetX+t.x*scale);lp.topMargin=Math.round(offsetY+t.y*scale);buttons[i].setLayoutParams(lp);}}
    private void text(Canvas c,String value,float x,float y,float size,int color,float width,boolean heavy){paint.setShader(null);paint.setStyle(Paint.Style.FILL);paint.setColor(color);paint.setTypeface(heavy?bold:regular);paint.setTextSize(size*Config.TEXT_SCALE);float min=Math.min(paint.getTextSize(),16);while(paint.measureText(value)>width&&paint.getTextSize()>min)paint.setTextSize(paint.getTextSize()-1);if(paint.measureText(value)>width){int n=value.length();while(n>0&&paint.measureText(value.substring(0,n)+"…")>width)n--;value=value.substring(0,n)+"…";}c.drawText(value,x,y,paint);}
    private void centered(Canvas c,String label,float w,float h,float font){paint.setTypeface(bold);paint.setTextSize(font*Config.TEXT_SCALE);float width=paint.measureText(label);float actual=Math.min(width,w-36);text(c,label,(w-actual)/2,h/2+font/3,font,Config.CARD_TEXT,w-36,true);}
    @Override protected void onDraw(Canvas c){super.onDraw(c);c.drawColor(Config.BACKGROUND);int save=c.save();c.translate(offsetX,offsetY);c.scale(scale,scale);paint.setStyle(Paint.Style.FILL);paint.setShader(background);c.drawRect(0,0,1920,1080,paint);paint.setShader(null);for(LayoutSpec.Tile t:LayoutSpec.TILES){int s=c.save();c.translate(t.x,t.y);drawTile(c,t);c.restoreToCount(s);}if(hasFocus()){LayoutSpec.Tile t=LayoutSpec.TILES[selected];paint.setShader(null);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(4);paint.setColor(Config.ACCENT);rect.set(t.x-5,t.y-5,t.x+t.w+5,t.y+t.h+5);c.drawRoundRect(rect,Config.RADIUS+4,Config.RADIUS+4,paint);paint.setStyle(Paint.Style.FILL);}c.restoreToCount(save);}
    private void drawTile(Canvas c,LayoutSpec.Tile t){
        float w=t.w,h=t.h,pad=Math.min(32,w*.08f);boolean clockTile=t.kind.equals("clock"),widget=t.kind.equals("widget");
        paint.setShader(null);paint.setStyle(Paint.Style.FILL);paint.setColor(t.color);rect.set(0,0,w,h);if(!clockTile)c.drawRoundRect(rect,Config.RADIUS,Config.RADIUS,paint);
        WidgetEngine.State state=state(t);
        if(clockTile){
            float size=Math.min(w*.34f,h*.55f);text(c,timeText,0,h*.55f,size,Config.TEXT,w,true);text(c,dateText,0,h*.75f,Math.min(40,h*.1f),Config.TEXT,w,false);
            String summary=Config.CITY+" · "+(state!=null&&state.content!=null&&!state.content.lines.isEmpty()?state.content.lines.get(0):"—");text(c,summary,0,h*.9f,Math.min(36,h*.09f),Config.TEXT,w,false);
            citation(c,state,0,h-5,w,Config.MUTED);return;
        }
        if(widget){
            float fs=Math.min(36,h*.105f);text(c,state!=null&&state.content!=null?state.content.title:t.label,pad,pad+fs,fs,Config.TEXT,w-2*pad,true);
            List<String> lines=state!=null&&state.content!=null?state.content.lines:Arrays.asList("Da configurare","Scegli fonte e contenuti");
            float step=(h-pad-fs-78)/(Math.max(4,lines.size())+1);for(int i=0;i<lines.size()&&i<5;i++)text(c,lines.get(i),pad,pad+fs+(i+1)*step,Math.min(29,step*.8f),Config.TEXT,w-pad*2,i<2);
            citation(c,state,pad,h-42,w-2*pad,Config.TEXT);text(c,state==null?"Da configurare":state.status(System.currentTimeMillis()),pad,h-18,16,Config.TEXT,w-2*pad,false);return;
        }
        Bitmap image=images.get(t.asset);if(image!=null){int save=c.save();clip.reset();clip.addRoundRect(rect,Config.RADIUS,Config.RADIUS,Path.Direction.CW);c.clipPath(clip);float fit=Math.min(w/image.getWidth(),h/image.getHeight());float iw=image.getWidth()*fit,ih=image.getHeight()*fit;rect.set((w-iw)/2,(h-ih)/2,(w+iw)/2,(h+ih)/2);paint.setColor(Color.WHITE);c.drawBitmap(image,null,rect,paint);c.restoreToCount(save);return;}
        if(t.action.equals("settings")){int save=c.save();c.translate(w/2,h/2);float z=Math.min(w,h)/105;c.scale(z,z);paint.setColor(Config.CARD_TEXT);c.drawPath(gear,paint);c.restoreToCount(save);return;}
        if(t.action.equals("network")){drawNetwork(c,w,h);return;}
        LineIcon icon=icons.get(t.action);if(icon!=null){int side=(int)Math.min(h*.7f,w*.23f);icon.setBounds((int)pad,(int)((h-side)/2),(int)pad+side,(int)((h+side)/2));icon.draw(c);text(c,t.label,pad+side+18,h/2+12,Math.min(34,h*.34f),Config.CARD_TEXT,w-2*pad-side-18,true);}else centered(c,t.label,w,h,Math.min(36,h*.25f));
    }
    private void citation(Canvas c,WidgetEngine.State state,float x,float y,float width,int color){String label="Fonte: ";if(state==null)label+="da configurare";else{LinkedHashSet<String> names=new LinkedHashSet<>();for(WidgetProvider.Source s:state.provider.sources())names.add(s.name);label+=android.text.TextUtils.join(" / ",names);}text(c,label,x,y,16,color,width,false);}
    private void drawNetwork(Canvas c,float w,float h){float z=Math.min(w/224,h/108);int save=c.save();c.translate(w/2,h/2);c.scale(z,z);if(network==NetworkSignal.ETHERNET){LineIcon icon=icons.get("ethernet");icon.setBounds(-34,-34,34,34);icon.draw(c);}else{paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(9);for(int k=1;k<=3;k++){float r=20+(k-1)*16;paint.setColor(network>=k?Config.CARD_TEXT:0xFFa0a0a0);rect.set(-r,22-r,r,22+r);c.drawArc(rect,225,90,false,paint);}paint.setStyle(Paint.Style.FILL);paint.setColor(Config.CARD_TEXT);if(network==NetworkSignal.UNKNOWN)text(c,"?",-7,28,20,Config.CARD_TEXT,20,true);else c.drawCircle(0,22,5,paint);if(network==NetworkSignal.OFFLINE){paint.setStrokeWidth(7);c.drawLine(-40,-34,40,30,paint);}}c.restoreToCount(save);}
    void dispose(){for(Bitmap b:images.values())if(b!=null)b.recycle();images.clear();}
}
