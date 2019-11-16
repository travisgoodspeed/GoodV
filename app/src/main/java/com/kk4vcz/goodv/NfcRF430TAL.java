package com.kk4vcz.goodv;

import android.nfc.Tag;
import android.util.Log;

import java.io.IOException;
import java.util.Arrays;

/* This class handles the RF430TAL152H chip found in some commercial Glucose Monitor devices.  With
   the backdoor password, it is able to read from any address within those devices.  Without the
   password, it isn't very useful.

 */

public class NfcRF430TAL extends NfcRF430FRL {
    public NfcRF430TAL(Tag tag) {
        //Our super is the RF430FRL, so many functions we can inherit without modification.
        super(tag);

        //Then we change the details that differ from the FRL.
        baseadr = 0xF860; //Base adr is one block lower.
        blocklen = 8;     //Block length is always 8.
        variant = "GCM";
        blockcount = 0xF4;
    }

    private byte[] password = {
            //Secret 4-byte password to unlock the RF430TAL152H devices.
            //You can find this by SPI bus sniffing or by reversing the public apps.
            (byte) 0xde, (byte) 0xad, (byte) 0xbe, (byte) 0xef
    };

    //! Unlocks the tag with the A4 command.
    public boolean unlock() throws IOException {
        byte[] res = transceive(new byte[]{
                0x02,         // Flags
                (byte) 0xA4,  // MFG Unlock Command
                0x07,         // MFG Code

                        //Secret password.
                        password[0], password[1], password[2], password[3],
                }
        );
        return (res[0] == 0);
    }

    //! Locks the tag with the A2 command.
    public boolean lock() throws IOException {
        byte[] res = transceive(new byte[]{
                0x02,         // Flags
                (byte) 0xA2,  // MFG Lock Command
                0x07,         // MFG Code

                // Secret password.
                password[0], password[1], password[2], password[3],
        });
        return (res[0] == 0);
    }

    //! Initializes the sensor tag and begins a 1-hour calibration process with the A0 command.
    public boolean calibrate() throws IOException {
        byte[] res = transceive(new byte[]{
                0x02,         // Flags
                (byte) 0xA0,  // MFG Calibrate Command
                0x07,         // MFG Code

                //Secret password.
                password[0], password[1], password[2], password[3],
        });
        return (res[0] == 0);
    }

    //! Don't yet know what this does.
    public boolean vendor_e0() throws IOException {
        byte[] res = transceive(new byte[]{
                0x02,         // Flags
                (byte) 0xE0,  // MFG Initialize Command
                0x07,         // MFG Code
        });
        return (res[0] == 0);
    }

    //! Don't yet know what this does.
    public boolean vendor_e1() throws IOException {
        byte[] res = transceive(new byte[]{
                0x02,         // Flags
                (byte) 0xE1,  // MFG Initialize Command
                0x07,         // MFG Code
        });
        return (res[0] == 0);
    }

    //! Don't yet know what this does.
    public boolean vendor_e2() throws IOException {
        byte[] res = transceive(new byte[]{
                0x02,         // Flags
                (byte) 0xE2,  // MFG Initialize Command
                0x07,         // MFG Code
        });
        return (res[0] == 0);
    }

    //! Reads a block using the backdoor command of the CGM tags.
    public byte[] readA3(int adr) throws IOException {
        //Log.v("GoodV", String.format("readA3(): Fetching block at 0x%04x.", adr));

        byte[] res = transceive(new byte[]{
                0x02,         // Flags
                (byte) 0xA3,  // backdoor Raw Read Command
                0x07,         // MFG Code

                //Secret password.
                password[0], password[1], password[2], password[3],

                (byte) (adr & 0xFF), (byte) (adr >> 8), //16-bit address, little endian.
                0x04          // 4 16-bit words is one block.
        });
        if (res[0] == 0) {
            //Log.v("GoodV", String.format("readA3(): "+GoodVUtil.byteArrayToHex(res), adr));
            return Arrays.copyOfRange(res, 1, res.length);
        }

        return new byte[]{};
    }

    //! Erases the tag.
    public void erase() throws IOException {
        /* In the case of a TAL tag, an erase restores the tag to its factory setting,
           so that it can be initialized again.

           TODO This doesn't work yet.
         */

        //First we unlock flash memory.
        unlock();

        //Then we re-enable the E-series commands.
        writeTITXT("@FFB8 E0 00 q");

        /* We might overwrite the early bytes, but this doesn't seem to be enough.
           Maybe its detecting damage, or maybe we need to overwrite more variables?
          */
        /*
        writeTITXT("@F860 \n"+
                        "3d c7 88 13 01 00 00 00 \n"+
                        "00 00 00 00 00 00 00 00 \n"+
                        "00 00 00 00 00 00 00 00 \n"+
                        "62 c2 00 00 00 00 00 00 \n"+
                        "q"
        );*/


        /* We might also try the E series of commands.  E0 seems to reset the state to
           1, but it quickly falls back to the failure mode on my extracted chip. */

        /*
        vendor_e0();
        vendor_e1();
        vendor_e2();
        */

        //Then we lock back the device.
        lock();


        Log.v("GoodV", "Erase complete, now in stage " + getStageOfLife());
    }

    //! Executed shellcode.
    @Override
    public byte[] exec(int adr) throws IOException {
        Log.e("GoodV", "This tag type doesn't yet support shellcode.");
        throw new IOException("Unable to execute shellcode.");
    }

    @Override
    public String dumpTITXT() throws IOException {
        String fram = readTITXT(0xf800, 2048) + "\n";
        String rom = readTITXT(0x4400, 0x2000) + "\n";
        String sram = readTITXT(0x1C00, 0x1000) + "\n";
        //Serial number and calibration are here.
        String config = readTITXT(0x1a00, 64) + "\n";

        return config + fram + rom + sram + "q";
    }

    public String getStageOfLife() throws IOException {
        try {
            return String.format("%02x", read(0xf864, 1)[0]);
        }catch(ArrayIndexOutOfBoundsException e){
            return "FF";
        }
    }

    public String getA1Text() throws IOException {
        byte[] res = transceive(new byte[]{
                0x02,         // Flags
                (byte) 0xA1,  // MFG Initialize Command
                0x07          // MFG Code
        });
        return GoodVUtil.byteArrayToHex(res);
    }

    //! Gets the tags info as a user-readable string.
    public String getInfo() throws IOException {
        String info = super.getInfo();
        info += "STAGE:    " + getStageOfLife() + "\n";
        info += "STATE:    " + getA1Text() + "\n";

        return info;
    }


    //! Reads data from a native address.
    public byte[] read(int adr, int len) throws IOException {
        Log.d("GoodV", "read(): address=@" + String.format("%04x", adr) + " len=" + len);

        //F867 special address doesn't exist in the TAL chips.

        //This does nothing if page doesn't need to be changed.
        setPageForAdr(adr);

        //Unaligned reads are corrected early.
        if (adr % blocklen != 0) {
            //First we grab the block before our data.
            int earlyblockadr = adr - (adr % blocklen);
            int earlyblocklen = adr - earlyblockadr + len;

            byte[] data = read(earlyblockadr, earlyblocklen);

            //Then we return just the requested fragment.
            try {
                return Arrays.copyOfRange(data, adr - earlyblockadr, adr - earlyblockadr + len);
            } catch (ArrayIndexOutOfBoundsException e) {
                Log.e("GoodV", "Array index failure.  Hoping for the best.");
                return new byte[]{};
            }
        }


        //Native block size is the easiest.
        if (len == blocklen) {
            //Fancy backdoor command!
            return readA3(adr);
        }

        //Buffer to fill as we load grab blocks.
        byte[] res = new byte[len];

        //Grab blocklen chunks and shuttle them over.
        int i = 0;
        while (i < len) {
            //Grab the chunk.
            //Log.v("GoodV", "read(): recursive call with i="+i);
            byte[] chunk = read(adr + i, blocklen);
            if (chunk.length < blocklen) {
                Log.e("GoodV", "read(): failed to read block address=@"
                        + String.format("%04x", adr + i) + " len=" + blocklen);
                return new byte[]{};
            }

            //Copy up to blocklen or buffer length bytes over.
            for (int j = 0; j < blocklen && i < len; i++, j++) {
                res[i] = chunk[j];
            }
        }

        return res;
    }
}
