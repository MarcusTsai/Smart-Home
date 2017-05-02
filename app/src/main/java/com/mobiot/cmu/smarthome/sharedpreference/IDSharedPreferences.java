package com.mobiot.cmu.smarthome.sharedpreference;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

public class IDSharedPreferences {
    private static final String ID_SHARED_PREFERENCES_STORE = "IDSharedPrefs";
    private static final String INSTANCE_ID = "InstanceID";
    private static IDSharedPreferences instance = null;
    private SharedPreferences mSharedPreferences = null;

    private Editor mSharedPreferencesEditor = null;


    public static IDSharedPreferences getInstance(Context ctx) {
        if (instance == null) {
            instance = new IDSharedPreferences(ctx);
        }
        return instance;
    }

    private IDSharedPreferences(Context appContext) {
        mSharedPreferences = appContext.getSharedPreferences(ID_SHARED_PREFERENCES_STORE, 0); // 0 - for private mode
        mSharedPreferencesEditor = mSharedPreferences.edit();
    }
    
    
    public String getInstanceID() {
         String instanceID = mSharedPreferences.getString(INSTANCE_ID, "");
    	 return instanceID;
	}

	public boolean setInstanceID(String instanceID) {
        mSharedPreferencesEditor.putString(INSTANCE_ID, instanceID);
        return (mSharedPreferencesEditor.commit());
	}
    
}
