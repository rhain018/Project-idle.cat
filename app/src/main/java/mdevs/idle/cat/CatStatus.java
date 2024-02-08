package mdevs.idle.cat;

import java.util.Date;

/**
 * Created by mdevs on 09/10/2023.
 */
public class CatStatus {
    // Cat
    public Integer hungry = 0;
    public Integer intimacy = 0;
    // Inventory
    public Integer food = 0;

    public Integer $coin = 0;
    // System
    public Long last_login_sec = 0L;
    public Date creation_date;
}