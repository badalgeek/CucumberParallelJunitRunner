package io.badal.cucumber;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by badal on 12/18/15.
 */
public class CucumberConfigProvider {

    public static CucumberConfigProvider instance;
    public Map<String, Object> propertyMap;

    private CucumberConfigProvider() {
        propertyMap = new HashMap<>();
    }

    public synchronized static CucumberConfigProvider getInstance() {
        if (instance == null) {
            instance = new CucumberConfigProvider();
        }
        return instance;
    }

    public synchronized boolean isPropertyExist(String key) {
        return propertyMap.containsKey(key);
    }

    public synchronized String getStringProperty(String key) {
        return getPropertyAndCast(key, String.class);
    }

    public synchronized String[] getStringArrayProperty(String key) {
        return getPropertyAndCast(key, String[].class);
    }

    public synchronized Integer getIntProperty(String key) {
        return getPropertyAndCast(key, Integer.class);
    }

    public synchronized void setProperty(String key, Object value) {
        propertyMap.put(key, value);
    }

    private <T> T getPropertyAndCast(String key, Class<T> clazz) {
        Object o = propertyMap.get(key);
        if (o != null && clazz.isInstance(o)) {
            return (T) o;
        } else if (o != null) {
            throw new ClassCastException("Expect StringType for property " + key + " but found " + o.getClass());
        }
        throw new NullPointerException("Could not found property " + key);
    }

}
