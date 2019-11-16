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

public class InfoFragment extends Fragment implements NfcRF430Handler {
    TextView infotext;
    Button infoexportbutton;

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState){
        View v=inflater.inflate(R.layout.fragment_info, container, false);

        infotext=v.findViewById(R.id.infotext);
        infoexportbutton=v.findViewById(R.id.infoexportbutton);
        infoexportbutton.setOnClickListener(new View.OnClickListener(){
                @Override
            public void onClick(View view){
                    // Gets a handle to the clipboard service.
                    ClipboardManager clipboard = (ClipboardManager)
                            view.getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                    // Make the clip.
                    ClipData clip = ClipData.newPlainText("RF430FRL152H Info", infotext.getText());
                    // Set the clipboard's primary clip.
                    clipboard.setPrimaryClip(clip);
                }
        });

        return v;
    }

    @Override
    public void tagTapped(NfcRF430 tag) {
        Log.d("GoodV", "Tag tapped to grab info.");

        try {
            infotext.setText(tag.getInfo());
        }catch(IOException e){
            infotext.setText("Read error.");
        }
    }
}
