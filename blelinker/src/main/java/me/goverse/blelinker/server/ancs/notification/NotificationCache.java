package me.goverse.blelinker.server.ancs.notification;

import java.util.ArrayList;
import java.util.List;

/**
 * @author gaoyu
 */
public class NotificationCache {

    private static final String TAG = "NotificationCache";
    private List<Notification> cache = new ArrayList<Notification>();

    /**
     *getLatestSentNotification
     * @param id id
     * @return Notification
     */
    public synchronized Notification getLatestSentNotification(int id){
        Notification latest=null;
        for(Notification notification : cache){
            if(notification.isSending() && notification.getId() == id){
                latest = notification;
            }
        }
        return latest;
    }

    /**
     * getNotificationAt
     * @param position position
     * @return Notification
     */
    public synchronized Notification getNotificationAt(int position){
        if(position < cache.size()){
            return cache.get(position);
        }
        return null;
    }

    /**
     * getFirstUnsentNotification
     * @return Notification
     */
    public synchronized Notification getFirstUnsentNotification(){
        for(Notification notification : cache){
            if(!notification.isSending()){
                return notification;
            }
        }
        return null;
    }

    /**
     * removeExpiredIdle
     * @param beforeAddedTime beforeAddedTime
     */
    public synchronized void removeExpiredIdle(long beforeAddedTime){
        List<Notification> newCache = new ArrayList<Notification>();
        Notification notification;
        for(int i = 0; i < cache.size(); ++i){
            notification = cache.get(i);
//            Log.i(TAG,"beforeAddedTime:"+beforeAddedTime);
//            Log.i(TAG,"notification.getAddedTime():"+notification.getAddedTime());

            if(!notification.isSending() && notification.getAddedTime() <= beforeAddedTime){
                continue;
            }
            newCache.add(notification);
        }

        if(newCache.size() != cache.size()){
            cache = newCache;
        }
    }

    /**
     * removeAllBefore
     * @param id id
     */
    public synchronized void removeAllBefore(int id){
        int position = 0;
        for (; position < cache.size(); ++position){
            if(cache.get(position).getId() == id){
                break;
            }
        }

        if(position == cache.size()){
            cache.clear();
            return;
        }

        List<Notification> newCache = new ArrayList<Notification>();

        position -= 30;
        if(position < 0){
            position = 0;
        }

        for (; position < cache.size(); ++position){
            newCache.add(cache.get(position));
        }
        cache = newCache;
    }

    /**
     * removeExpiredAll
     * @param beforeAddedTime beforeAddedTime
     */
    public synchronized void removeExpiredAll(long beforeAddedTime){
        List<Notification> newCache = new ArrayList<Notification>();
        Notification notification;
        for(int i = 0; i < cache.size(); ++i){
            notification = cache.get(i);
            if(notification.getAddedTime() < beforeAddedTime){
                continue;
            }
            newCache.add(notification);
        }
        if(newCache.size() != cache.size()){
            cache = newCache;
        }
    }

    /**
     * addNotification
     * @param notification notification
     */
    public synchronized void addNotification(Notification notification){
        cache.add(notification);
    }

    /**
     * clear
     */
    public synchronized void clear(){
        cache.clear();
    }
}
