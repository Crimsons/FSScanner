package com.FSScanner;

import android.os.Parcelable;
import java.util.Comparator;


/**
 * Comparator to compare venue objects in ascending order.
 * Constructor takes a parameter that indicates comparison method
 */
public class VenueComparator implements Comparator<Parcelable> {

    public enum CompareBy { DISTANCE, POPULARITY }
    CompareBy compareBy;


    /**
     * Constructor
     * @param compareBy Indicates comparison method
     */
    VenueComparator( CompareBy compareBy ) {
        this.compareBy = compareBy;
    }


    @Override
    public int compare(Parcelable lhs, Parcelable rhs) {
        int lhsValue, rhsValue;

        switch (compareBy) {
            case DISTANCE:
                lhsValue = ((Venue)lhs).getDistance();
                rhsValue = ((Venue)rhs).getDistance();
                break;
            case POPULARITY:
                lhsValue = ((Venue)lhs).getCheckinsCount();
                rhsValue = ((Venue)rhs).getCheckinsCount();
                break;
            default:
                lhsValue = 0;
                rhsValue = 0;
        }
        return lhsValue < rhsValue ? -1
                : lhsValue > rhsValue ? 1
                : 0;
    }
}
