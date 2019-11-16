package com.kk4vcz.goodv;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

public class ProgramFragment extends Fragment implements NfcRF430Handler{
    EditText programtext;
    Button programbutton;
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState){
        View v=inflater.inflate(R.layout.fragment_program, container, false);

        programtext=v.findViewById(R.id.program_text);
        programbutton=v.findViewById(R.id.program_button);

        return v;
    }

    @Override
    public void tagTapped(NfcRF430 tag) {

        Log.d("GoodV", "Tag tapped to program.");
        try {
            if(tag.writeTITXT(programtext.getText().toString()))
                programbutton.setText("Programmed successfully!");
            else
                programbutton.setText("Programming error. :(");
        }catch(Exception e){
            e.printStackTrace();
            programbutton.setText("IOException.");
        }

    }
}
