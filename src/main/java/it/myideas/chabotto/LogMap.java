package it.myideas.chabotto;

import java.time.ZonedDateTime;
import java.util.Date;
import java.util.HashMap;

/**
 * Simple class wrap a map with some syntax-sugar, and automagically transform it as a json string 
 * @author Tommaso Doninelli
 *
 */
public class LogMap {

    private HashMap<String, String> data = new HashMap<>();
    
    public LogMap put(String key, String value) {
        data.put(key, value);
        return this;
    }
    
    public LogMap put(String key, Integer value) {
        data.put(key, Integer.toString(value));
        return this;
    }
    
    public LogMap put(String key, ZonedDateTime value) {
        data.put(key, value.toString());
        return this;
    }
    
    /**
     * Calling this method is time-expensive!
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("{");        
        data.entrySet().forEach(entry -> {
            sb.append("'").append(entry.getKey()).append("':'").append(entry.getValue()).append("'").append(",");
        });
        sb.append("}");

        return sb.toString();
    }


    public LogMap put(String key, Date value) {
        data.put(key, value.toString());
        return this;
    }

}