package com.kk4vcz.goodv;

import android.nfc.Tag;
import android.util.Log;

import java.io.IOException;

public class NfcRF430NXPIcodeSli extends NfcRF430 {
    public NfcRF430NXPIcodeSli(Tag tag) {
        super(tag);


    }

    @Override
    public void connect() throws IOException {
        super.connect();

        byte[] rawinfo=getRawInfo();
        blockcount=rawinfo[12];
        baseadr=0;
        blocklen=4;
        variant="NXPICODESLI";
    }

    @Override
    public void erase() throws IOException {
        Log.v("GoodV", String.format("Eeeek, how do I erase an SLI tag?"));
    }


    //! Gets the tags info as a user-readable string.
    public String getInfo() throws IOException {
        String info = super.getInfo();

        info +=
                "BLOCKS: " + blockcount + "\n";

        return info;
    }
}
