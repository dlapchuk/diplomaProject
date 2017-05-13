package info.androidhive.firebase;

import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.database.GenericTypeIndicator;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by user on 19.02.2017.
 */

public class Goal  implements Serializable {
    private String key;
    private double averageSpeed;
    private float distance;
    private long duration;
    private Date startDate;
    private Date endDate;
    private String name;
    //GenericTypeIndicator <LinkedList<LatLng>> genericTypeIndicator = new GenericTypeIndicator<LinkedList<LatLng>>() {};

    public Goal(){
    }

    public Goal(double averageSpeed, float distance, long duration, Date startDate, Date endDate, String name, LinkedList locations) {
        this.averageSpeed = averageSpeed;
        this.distance = distance;
        this.duration = duration;
        this.startDate = startDate;
        this.endDate = endDate;
        this.name = name;
        //this.locations = (LinkedList<LatLng>)locations;
    }

    public String getKey(){ return key; }

    public void setKey(String key){ this.key = key; }

    public double getAverageSpeed() {
        return averageSpeed;
    }

    public void setAverageSpeed(double averageSpeed) {
        this.averageSpeed = averageSpeed;
    }

    public float getDistance() {
        return distance;
    }

    public void setDistance(float distance) {
        this.distance = distance;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
