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

import static android.support.v4.content.ContextCompat.getSystemService;


/* This fragment exists to dump various memories of an RF430 over the NFC connection.
 */

public class DumpFragment extends Fragment implements NfcRF430Handler {
    TextView dumptext;
    Button dumpexportbutton;

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState){
        View v=inflater.inflate(R.layout.fragment_dump, container, false);

        dumptext=v.findViewById(R.id.dumptext);
        dumpexportbutton=v.findViewById(R.id.dumpexportbutton);
        dumpexportbutton.setOnClickListener(new View.OnClickListener(){
                @Override
            public void onClick(View view){
                    // Gets a handle to the clipboard service.
                    ClipboardManager clipboard = (ClipboardManager)
                            view.getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                    // Make the clip.
                    ClipData clip = ClipData.newPlainText("RF430FRL152H Dump", dumptext.getText());
                    // Set the clipboard's primary clip.
                    clipboard.setPrimaryClip(clip);
                }
        });

        return v;
    }

    @Override
    public void tagTapped(NfcRF430 tag) {
        Log.d("GoodV", "Tag tapped to dump.");

        /* The RF430 chips take a long while to dump.  We basically try to dump each section,
           assuming that illegal or unavailable sections will quickly fail and move on to the
           next.
         */


        //TODO -- Dumping would be a lot nicer as a background thread.
        try {
            dumptext.setText(tag.dumpTITXT());
        } catch (IOException e) {
            dumptext.setText("Read error.");
        }
    }
}
