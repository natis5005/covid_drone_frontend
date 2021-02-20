package com.CovidDrone.UseCases;

import android.os.Parcel;
import android.os.Parcelable;

import com.CovidDrone.models.UserLocation;

public class Request implements Parcelable {
    private String title;
    private UserLocation userData;
    private String requestId;

    public Request(){

    }

    public void setTitle(String title){
        this.title = title;
    }

    public void setUserData(UserLocation userData){
        this.userData = userData;
    }

    public void setRequestId(String requestId){
        this.requestId = requestId;
    }

    public Request(String title, UserLocation userLocation, String requestId){
        this.title = title;
        this.userData = userLocation;
        this.requestId = requestId;
    }

    protected Request(Parcel in) {
        title = in.readString();
        userData = in.readParcelable(UserLocation.class.getClassLoader());
        requestId = in.readString();
    }

    public static final Creator<Request> CREATOR = new Creator<Request>() {
        @Override
        public Request createFromParcel(Parcel in) {
            return new Request(in);
        }

        @Override
        public Request[] newArray(int size) {
            return new Request[size];
        }
    };

    public UserLocation getUserData(){
        return userData;
    }

    public String getTitle(){
        return title;
    }

    public String getRequestId(){
        return requestId;
    }

    @Override
    public String toString() {
        return "Request{" +
                "title=" + title +
                "requestId" + requestId +
                "userData=" + userData +
                '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(title);
        dest.writeParcelable(userData, flags);
        dest.writeString(requestId);
    }
}
