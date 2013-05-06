package com.FSScanner;

import android.os.Parcel;
import android.os.Parcelable;


public class Venue implements Parcelable {

	// fields
	private String name;
	private String address;
	private int distance;
	private String iconUrl;
	private double latitude;
	private double longitude;
	private int checkinsCount;
	
	
	// Constructors
	Venue( ) {
		this(null, null, 0, null, 0, 0, 0);
	}
	
	Venue(String name, String address, int distance, String iconUrl, long latitude, long longitude, int checkinsCount) {
		this.name = name;
		this.address = address;
		this.distance = distance;
		this.iconUrl = iconUrl;
		this.latitude = latitude;
		this.longitude = longitude;
		this.checkinsCount = checkinsCount;
	}

	
	// Setters & getters
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public int getDistance() {
		return distance;
	}

	public void setDistance(int distance) {
		this.distance = distance;
	}

	public String getIconUrl() {
		return iconUrl;
	}

	public void setIconUrl(String iconUrl) {
		this.iconUrl = iconUrl;
	}

	public double getLatitude() {
		return latitude;
	}

	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}

	public double getLongitude() {
		return longitude;
	}

	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}

	public int getCheckinsCount() {
		return checkinsCount;
	}

	public void setCheckinsCount(int checkinsCount) {
		this.checkinsCount = checkinsCount;
	}


	// implement Parcelable so that ArraysList of venues can be put to bundle in
    // onSaveInstanceState()
	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(name);
		dest.writeString(address);
		dest.writeInt(distance);
		dest.writeString(iconUrl);
		dest.writeDouble(latitude);
		dest.writeDouble(longitude);
	}
	
    public static final Parcelable.Creator<Venue> CREATOR
    	= new Parcelable.Creator<Venue>() {
    	public Venue createFromParcel(Parcel in) {
    		return new Venue(in);
    	}

    	public Venue[] newArray(int size) {
    		return new Venue[size];
    	}
    };
    
    private Venue(Parcel in) {
		this.name = in.readString();
		this.address = in.readString();
		this.distance = in.readInt();
		this.iconUrl = in.readString();
		this.latitude = in.readLong();
		this.longitude = in.readLong();
    }
}
