package com.aware.plugin.collapse_detector;

/**
 * Created by pratyush on 16/02/15.
 */

import android.app.IntentService;
import android.content.Intent;

import com.aware.ESM;


public class PopUp extends IntentService {

    private static String Q;

    void initialize() {

        Q = "{'esm':{" +
                "'esm_type':" + ESM.TYPE_ESM_QUICK_ANSWERS + "," +
                "'esm_title': 'Fall Detected (Yes/No)'," +
                "'esm_instructions': 'Did you or your phone fell down?'," +
                "'esm_quick_answers': ['No','Yes']," +
                "'esm_expiration_threashold': 0," +
                "'esm_trigger': ''" +
                "}}";
    }

    public PopUp() {
        super("PopUp");
        initialize();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Intent queue_esm = new Intent(ESM.ACTION_AWARE_QUEUE_ESM);
        String esm = "[" + Q + "]";
        queue_esm.putExtra(ESM.EXTRA_ESM, esm);
        this.sendBroadcast(queue_esm);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
