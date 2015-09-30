package com.ashapkaatgmail.spotifystreamer.Helpers;


import android.os.Parcel;
import android.os.Parcelable;

import java.util.HashMap;
import java.util.Map;

public class HashMapWrapperParcelable<K, V> implements Parcelable {

    private final HashMap<K, V> mMap = new HashMap<>();

    public HashMapWrapperParcelable(){
        // empty
    }

    public void put(K key, V value) {
        mMap.put(key, value);
    }

    public V get(K key) {
        V value = mMap.get(key);
        return value;
    }

    public HashMap<K, V> getHashMap() {
        return mMap;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        int size = mMap.size();
        dest.writeInt(size);
        if (size > 0) {
            for (Map.Entry<K, V> entry : mMap.entrySet()) {
                dest.writeValue(entry.getKey());
                dest.writeValue(entry.getValue());
            }
        }
    }

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public HashMapWrapperParcelable createFromParcel(Parcel in) {
            return new HashMapWrapperParcelable(in);
        }
        public HashMapWrapperParcelable[] newArray(int size) {
            return new HashMapWrapperParcelable[size];
        }
    };

    private HashMapWrapperParcelable(Parcel in) {
        int size = in.readInt();
        for (int i = 0; i < size; ++i) {
            K key = (K)in.readValue(null);
            V value = (V)in.readValue(null);
            mMap.put(key, value);
        }
    }

}
