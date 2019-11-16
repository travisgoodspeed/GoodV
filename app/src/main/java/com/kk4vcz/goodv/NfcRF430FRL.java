package com.kk4vcz.goodv;

import android.nfc.Tag;
import android.util.Log;

import java.io.IOException;

public class NfcRF430FRL extends NfcRF430 {
    public NfcRF430FRL(Tag tag) {
        super(tag);

        //Things specific to this tag.
        variant = "FRL";
    }



    @Override
    public void connect() throws IOException {
        super.connect();

        if (variant.equals("FRL")) {
            readF867();
        }
    }

    //! Erases the tag.
    public void erase() throws IOException {
        /* So, it turns out that erasing is a trickier problem than we might imagine.  The FRAM
           is used not just for firmware, but also as nonvolatile data memory memory.  So what we
           do here is invalidate the RESET vector, the patch table, and disable all sensor readings
           while leaving the NFCV stack intact.

           A TXT file of a blank image might be cleaner than successive write() calls.
         */


        //Invalidate the RESET vector.
        write(0xFFFE, new byte[]{(byte) 0xFF, (byte) 0xFF});
        //Invalidate the patch table.
        write(0xFFCE, new byte[]{(byte) 0xFF, (byte) 0xFF});
        //8 byte pages, NFCV stack but no sensors.
        write(0xF867, new byte[]{(byte) 0x7F});

        Log.v("GoodV", "Erase complete.");
    }

    @Override
    public String dumpTITXT() throws IOException {
        //Read what we can, if we can.
        String f867 = readTITXT(0xf867, 1) + "\n";
        String fram = readTITXT(baseadr, blocklen * blockcount) + "\n";
        String sram = readTITXT(0x1C00, 0x1000) + "\n";

        return f867 + fram + sram + "q";
    }

    public byte[] exec(int adr) throws IOException {
        /* While we could overwrite the call stack, it is much easier to overwrite the
           function call table in early SRAM with a pointer to our function, because we
           can only perform writes of 4 or 8 bytes at a time, and the call stack within a
           write handler will be quite different from the one in a read handler.

           There are plenty of functions to choose from, and an ideal hook would be one that
           won't be missed by normal functions.  We'd also prefer to have continuation wherever
           possible, so that executing the code doesn't crash our target.

           The function pointer we'll overwrite is at 0x1C5C, pointing to rom_rf13_senderror() at
           0x4FF6.  For proper continuation, you can just write two bytes to RF13MTXF and return.
           Without proper continuation, an IOException will be thrown in the reply timeout.
           To unhook, write 0x4FF6 to 0x1C5C, restoring the original handler.

           As a handy side effect, we return the two bytes that need to be transmitted for
           continuation, so you can get a bit of data back from your shellcode.
         */

        Log.v("GoodV", String.format("Asked to call shellcode at %04x", adr));

        // First we replace the read error reply handler.
        write(0x1C5C, new byte[]{(byte) (adr & 0xFF), (byte) (adr >> 8)});

        // Then we read from an illegal address to trigger an error,
        // returning the two bytes of its handler.
        byte[] shellcodereturn = transceive(new byte[]{
                0x02,         // Flags
                (byte) 0xC0,  // MFG Raw Read Command
                0x07,         // MFG Code
                (byte) (0xbe), (byte) (0xba) //16-bit block number, little endian.
        });
        Log.v("GoodV", "Shellcode returned: " + GoodVUtil.byteArrayToHex(shellcodereturn));

        //And finally, we repair the original handler address, like nothing ever happened.
        write(0x1C5C, new byte[]{(byte) (0xf6), (byte) (0x4f)});

        return shellcodereturn;
    }

    //! Gets the tags info as a user-readable string.
    public String getInfo() throws IOException {
        String info = super.getInfo();
        info +=
                "JTAGLOCK: " + (isJTAGLocked() ? "LOCKED" : "UNLOCKED") + "\n" +
                        "RESET VEC:" + GoodVUtil.byteArrayToHex(read(0xFFFE, 2)) + "\n";
        return info;
    }
}
