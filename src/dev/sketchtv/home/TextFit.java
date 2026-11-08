package dev.sketchtv.home;

/** Logarithmic text fitting; independent of Android for regression tests. */
final class TextFit {
    interface Measure {float width(String text,float size);}
    static final class Result {
        final String text;final float size;
        Result(String text,float size){this.text=text;this.size=size;}
    }
    static Result fit(String text,float requested,float width,Measure measure){
        float low=Math.min(16,requested),high=requested;
        if(measure.width(text,high)<=width)return new Result(text,high);
        if(measure.width(text,low)<=width){
            for(int i=0;i<10&&high-low>.25f;i++){float mid=(low+high)/2;if(measure.width(text,mid)<=width)low=mid;else high=mid;}
            return new Result(text,low);
        }
        if(measure.width("…",low)>width)return new Result("",low);
        int count=text.codePointCount(0,text.length()),left=0,right=count;
        while(left<right){int mid=(left+right+1)/2;String candidate=text.substring(0,text.offsetByCodePoints(0,mid))+"…";if(measure.width(candidate,low)<=width)left=mid;else right=mid-1;}
        return new Result(text.substring(0,text.offsetByCodePoints(0,left))+"…",low);
    }
}
