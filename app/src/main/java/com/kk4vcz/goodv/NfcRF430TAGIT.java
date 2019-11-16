package com.kk4vcz.goodv;

import android.nfc.Tag;
import android.util.Log;

import java.io.IOException;

public class NfcRF430TAGIT extends NfcRF430 {
    public NfcRF430TAGIT(Tag tag) {
        super(tag);

        variant = "TAGIT";
        blocklen = 4;
        baseadr = 0;
        blockcount = 0x40;
    }

    @Override
    public void erase() throws IOException {
        Log.v("GoodV", String.format("Eeeek, how do I erase a TagIT tag?"));
    }
}
