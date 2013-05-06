package com.FSScanner;

import java.util.List;

import android.content.Context;
import android.os.Parcelable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.fedorvlasov.lazylist.ImageLoader;


public class CustomAdapter extends BaseAdapter {

    // logcat logging
    private static final boolean LOG_ENABLED = false;
	private static final String LOG_TAG = CustomAdapter.class.toString();

    // fields
	private LayoutInflater mLayoutInflater;
	private List<Parcelable> dataList;
	private final ImageLoader imageLoader;
	
	// constructor
	public CustomAdapter(Context mContext, List<Parcelable> dataList) {
		this.dataList = dataList;
		mLayoutInflater = LayoutInflater.from(mContext);
		imageLoader = new ImageLoader(mContext);
	}
	
	// custom implementation of BaseAdapter
	@Override
	public int getCount() {
		return dataList.size();
	}

	@Override
	public Object getItem(int position) {
		return dataList.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View view, ViewGroup parent) {
		
		if (LOG_ENABLED) Log.d(LOG_TAG, "getView(): " + position + " " + view);
		
		// object that is attached to each row of list to hold
		// references to views in that row
        ViewHolder viewHolder;
		
		// if view cannot be recycled, inflate it, get references
		// to its widgets and attach viewHolder.
        if (view == null) {
			view = mLayoutInflater.inflate(R.layout.list_layout, parent, false);
			viewHolder = new ViewHolder();
			viewHolder.imageView = (ImageView)view.findViewById(R.id.icon);
			viewHolder.nameText = (TextView)view.findViewById(R.id.nameText);
			viewHolder.addressText = (TextView)view.findViewById(R.id.addressText);
			viewHolder.distanceText = (TextView)view.findViewById(R.id.distanceText);
			viewHolder.checkinsText = (TextView)view.findViewById(R.id.checkinsText);
			view.setTag(viewHolder);
		}
		
		// set row views values
        Venue venue = (Venue)dataList.get(position);
		viewHolder = (ViewHolder)view.getTag();
		viewHolder.nameText.setText(venue.getName());
		viewHolder.addressText.setText(venue.getAddress());
		viewHolder.distanceText.setText(venue.getDistance() + "m"); 
		viewHolder.checkinsText.setText(Integer.toString(venue.getCheckinsCount()));
		
		// as icon may be downloaded from web it is done asynchronously.
        // imageLoader also does caching to memory and SD card.
        imageLoader.DisplayImage(venue.getIconUrl(), viewHolder.imageView);

		return view;
	}
	
	private class ViewHolder {
		ImageView imageView;
		TextView nameText;
		TextView addressText;
		TextView distanceText;
		TextView checkinsText;
	}
	
}
