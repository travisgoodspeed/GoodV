package com.kk4vcz.goodv;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.io.IOException;


/* This fragment exists to show the tag info.
 */

public class EraseFragment extends Fragment implements NfcRF430Handler {
    TextView erasetext;

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState){
        View v=inflater.inflate(R.layout.fragment_erase, container, false);

        erasetext=v.findViewById(R.id.erasetext);

        return v;
    }

    @Override
    public void tagTapped(NfcRF430 tag) {
        Log.d("GoodV", "Tag tapped to erase.");
        try {
            tag.erase();

            //And finally we brag about it.
            String info =
                    "Erased tag "+GoodVUtil.byteArrayToHex(tag.getSerialNumber())+"\n\n";

            erasetext.setText(info);
        }catch(IOException e){
            erasetext.setText("Erase error.");
            e.printStackTrace();
        }
    }
}
