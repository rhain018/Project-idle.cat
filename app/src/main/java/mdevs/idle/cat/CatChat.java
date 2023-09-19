package mdevs.idle.cat;

/**
 * Created by mdevs on 09/16/2023.
 */
public class CatChat {
    public CatChat() {}
    public Integer chat(Integer size){
        Double r = Math.floor(Math.random() * size);
        return r.intValue();
    }
}
