package com.kk4vcz.goodv;

import android.nfc.Tag;
import android.util.Log;

import java.io.IOException;


/* This is generic class for interacting with NFC Type V tags that are not RF430 tags.
   It might be useful for implementing TagIT support, or for interacting with tags from
   other manufacturers.
 */

public class NfcRF430Generic extends NfcRF430 {
    public NfcRF430Generic(Tag tag) {
        super(tag);

        variant = String.format("Unknown%02x%02x", tagid[6], tagid[5]);

        //Guessing at the size.
        blocklen = 4;
        baseadr = 0;
        blockcount = 0x10;
    }

    @Override
    public void erase() throws IOException {
        Log.v("GoodV", String.format("Eeeek, how do I erase a generic tag?"));
    }
}
