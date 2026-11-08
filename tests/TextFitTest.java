package dev.sketchtv.home;
public class TextFitTest {
    static int calls;
    static float measure(String text,float size){calls++;return text.codePointCount(0,text.length())*size;}
    static void check(boolean b,String message){if(!b)throw new AssertionError(message);}
    public static void main(String[] args){
        TextFit.Result r=TextFit.fit("Short",20,200,TextFitTest::measure);check(r.text.equals("Short")&&r.size==20,"preserves fitting text");
        calls=0;r=TextFit.fit("0123456789",300,500,TextFitTest::measure);check(r.size<=50&&r.size>49,"fits font");check(calls<15,"bounded logarithmic sizing");
        r=TextFit.fit("A very long example",24,70,TextFitTest::measure);check(r.text.endsWith("…")&&measure(r.text,r.size)<=70,"ellipsizes at minimum");
        r=TextFit.fit("A",24,1,TextFitTest::measure);check(r.text.isEmpty(),"does not overflow even with ellipsis");
        r=TextFit.fit("😀😀😀😀😀",24,50,TextFitTest::measure);check(!r.text.contains("\uFFFD")&&r.text.codePointCount(0,r.text.length())<=3,"unicode intact");
        r=TextFit.fit("",24,50,TextFitTest::measure);check(r.text.isEmpty(),"empty label");
        System.out.println("PASS: bounded text sizing, width limits and Unicode ellipsis");
    }
}
