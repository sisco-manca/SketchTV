package dev.sketchtv.home;
public class CatalogRefreshTest {
    static void check(boolean value,String why){if(!value)throw new AssertionError(why);}
    public static void main(String[] args){
        CatalogRefresh state=new CatalogRefresh();check(state.needsRender(),"initial grid");state.accept(1);state.rendered();check(!state.needsRender(),"render consumes dirty state");
        state.accept(1);check(!state.needsRender(),"same scan retains UI");state.accept(2);check(state.needsRender(),"changed hidden scan remains dirty");state.accept(2);check(state.needsRender(),"same subsequent scan cannot lose pending render");state.rendered();check(!state.needsRender(),"render publishes latest");
        state.invalidate();state.accept(2);check(state.needsRender(),"icon update redraws with unchanged app names");
        System.out.println("PASS: catalog retains UI and never loses offscreen updates");
    }
}
