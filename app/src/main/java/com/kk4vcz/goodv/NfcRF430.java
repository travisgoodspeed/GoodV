package com.kk4vcz.goodv;

import android.nfc.Tag;
import android.nfc.TagLostException;
import android.nfc.tech.NfcV;
import android.nfc.tech.TagTechnology;
import android.util.Log;

import java.io.IOException;
import java.util.Arrays;
import java.util.StringTokenizer;
import java.util.stream.Stream;

/* This is a convenient wrapper for the NfcV class that adds support for commands found in the
   mask ROM of the RF430FRL152H from Texas Instruments.  My goal is to provide some higher-level
   abstractions than individual requests, while keeping things sufficiently low level that they
   can be traced and understood.

   Previously, this was the only class, but I've been refactoring it out into separate handler
   classes for the different chips.  If you see any janky if(){} clauses, that's why.

   --Travis
 */

public abstract class NfcRF430 implements TagTechnology {
    Tag tag;
    NfcV nfcv;

    byte[] tagid;
    int blocklen = 8; //8 bytes is default, but 4 can be supported by tag firmware.
    int page = 0; //in 4-byte mode, there are two pages.
    int baseadr = 0xF868; //F868 on stock ROM, F860 on CGM ROM.
    int blockcount = 0xF3; //0xF3 blocks on stock, 0xF4 on GCM.
    String variant;

    public NfcRF430(Tag tag) {
        this.tag = tag;
        this.tagid = tag.getId();
        this.nfcv = NfcV.get(this.tag);
    }

    /* Use this function to get a non-abstract class that's right for your tag. */
    public static NfcRF430 get(Tag tag) {
        byte[] tagid = tag.getId();
        switch (tagid[6]) {
            case 0x07: //Texas Instruments is our primary target.
                switch (tagid[5]) {
                    case 0: //Tag-IT
                        Log.d("GoodV", "TAG-IT");
                        return new NfcRF430TAGIT(tag);
                    case (byte) 0xA2: //Stock ROM of RF430FRL152H.
                        Log.d("GoodV", "RF430FRL");
                        return new NfcRF430FRL(tag);
                    case (byte) 0xA0: //GCM ROM of the RF430TAL152H.
                        Log.d("GoodV", "RF430TAL");
                        return new NfcRF430TAL(tag);
                }

            case 0x04: //NXP tags are the most common.
                switch (tagid[5]) {
                    case 01: //Stand Label IC
                        return new NfcRF430NXPIcodeSli(tag);
                }
        }

        //Unknown tag, so we hope for the best.
        return new NfcRF430Generic(tag);
    }

    @Override
    public Tag getTag() {
        return this.tag;
    }

    @Override
    public void connect() throws IOException {
        nfcv.connect();
    }

    //! Reads the block length, among other things.
    public void readF867() throws IOException{
        byte f867 = read(0xf867, 1)[0];

        //RF430FRL152H tags can have either 8 byte or 4 byte blocks.
        if ((f867 & 1) == 1) {
            blocklen = 8;
            page = 0;
        } else {
            blocklen = 4;
            //Page bit is inverted.
            page = (((f867 >> 1) & 1) == 1) ? 0 : 1;
        }
    }

    @Override
    public void close() throws IOException {
        //Close on page 0 for 4-byte devices.
        if(blocklen==4 && variant.equals("FRL"))
            setPage(0);
        nfcv.close();
    }

    @Override
    public boolean isConnected() {
        return nfcv.isConnected();
    }

    /* These functions are the lowest level, directly communicating with the tag. */

    public byte[] transceive(byte[] data) throws IOException {
        return nfcv.transceive(data);
    }


    //! Converts an address to a 16-bit block.
    public int adr2block(int adr) {
        /* Different RF430 devices have different mappings of FRAM to blocks.
           In the stock firmware, blocks run [F868,FFF8] as blocks [0,F2].  GCM tags
           run a different range, from [F860,FFF8] as blocks [0,F3].

           The special address F867 is available on stock ROM, but not GCM ROM.
         */

        if (adr >= 0x1C00 && adr < 0x2C00) { ///SRAM
            /* Only available on stock rom by the Cx commands.*/
            return ((adr - 0x1C00) / blocklen) + 0x600;
        } else if (adr >= baseadr && adr < 0x10000) {
            // RF430FRL152H devices wrap into the next page after 0xF2,
            // so 0xFC34 is fetched from page 1, block 0.

            //Don't forget to set the page number!
            int block = ((adr - baseadr) / blocklen);
            if (block >= blockcount)
                block -= blockcount;
            return block;
        } else { //No block number.
            Log.e("GoodV", "No block number for address: " + adr);
            return -1;
        }
    }



    /* These functions are a bit higher level, implementing one command apiece. */


    //! Reads the system info.
    public byte[] getRawInfo() throws IOException {
        byte[] res = transceive(new byte[]{
                0x02,         // Flags
                (byte) 0x2b,  // Standard Read Command
        });

        /* Example result:
             OK ?? Serial Number    ????
             00 04 477a010000a207e0 f207
         */

        //Return the result.
        return res;
    }

    //! Reads the serial number.
    public byte[] getSerialNumber() throws IOException {
        /* Example result:
             OK ?? Serial Number    ????
             00 04 477a010000a207e0 f207
                  /This is backward\
         */

        //Return the result.
        byte[] revserial = Arrays.copyOfRange(getRawInfo(), 2, 2 + 8);


        //Easiest to just hardcode the reversal.
        byte[] serial = new byte[]{
                revserial[7],
                revserial[6],
                revserial[5],
                revserial[4],
                revserial[3],
                revserial[2],
                revserial[1],
                revserial[0]
        };

        return serial;
    }


    //! Reads a block with an 8-bit address.  Standard NFC-V command.
    public byte[] readBlock8(byte block) throws IOException {
        Log.d("GoodV", "readBlock8(): NFC Read command for block 0x" + String.format("%02X", block));
        byte[] res = transceive(new byte[]{
                0x02,         // Flags
                (byte) 0x20,  // Standard Read Command
                block         // 8-bit block number
        });

        //Return just the bytes on success, or nothing on failure.
        if (res[0] == 0)
            return Arrays.copyOfRange(res, 1, res.length);
        else
            return new byte[]{};
    }

    //! Writes a block with an 8-bit address.  Standard NFC-V command.
    public boolean writeBlock8(byte block, byte[] data) throws IOException {
        byte[] res = new byte[]{1}; //Failure by default.


        if (blocklen == 8)
            res=transceive(new byte[]{
                    0x02,         // Flags.  (Docs say option must be set, but that fails for me.)
                    (byte) 0x21,  // Standard Write Command
                    block,         // 8-bit block number
                    data[0],
                    data[1],
                    data[2],
                    data[3],
                    data[4],
                    data[5],
                    data[6],
                    data[7]
            });
        else if (blocklen == 4)
            res=transceive(new byte[]{
                    0x02,         // Flags.  (Docs say option must be set, but that fails for me.)
                    (byte) 0x21,  // Standard Write Command
                    block,         // 8-bit block number
                    data[0],
                    data[1],
                    data[2],
                    data[3]
            });


        if (res[0] == 0)
            return true;

        Log.e("GoodV", String.format("Error writing block8 0x%02x", block));
        return false;
    }

    //! Writes a block with a 16-bit address.  Custom vendor command.
    public boolean writeBlock16(int block, byte[] data) throws IOException {
        if (block < 0x100)
            return writeBlock8((byte) block, data);


        byte[] res = new byte[]{1};

        if (blocklen == 8)
            res=transceive(new byte[]{
                0x02,         // Flags
                (byte) 0xC1,  // MFG Raw Write Command
                0x07,         // MFG Code
                (byte) (block & 0xFF), (byte) (block >> 8), //16-bit block number, little endian.
                data[0],
                data[1],
                data[2],
                data[3],
                data[4],
                data[5],
                data[6],
                data[7]
            });
        else if (blocklen == 4)
            res=transceive(new byte[]{
                    0x02,         // Flags
                    (byte) 0xC1,  // MFG Raw Write Command
                    0x07,         // MFG Code
                    (byte) (block & 0xFF), (byte) (block >> 8), //16-bit block number, little endian.
                    data[0],
                    data[1],
                    data[2],
                    data[3]
            });

        if (res[0] == 0) {
            return true;
        }

        Log.e("GoodV", String.format("Error writing block16 0x%04x", block));
        return false;
    }

    //! Reads a block with a 16-bit address.  Custom vendor command.
    public byte[] readBlock16(int block) throws IOException {
        Log.v("GoodV", String.format("readBlock16(): Fetching block 0x%04x.", block));

        //For compatibility with custom tags, we default to readBlock8
        //where compatible.
        if (block < 0x100)
            return readBlock8((byte) block);


        byte[] res = transceive(new byte[]{
                0x02,         // Flags
                (byte) 0xC0,  // MFG Raw Read Command
                0x07,         // MFG Code
                (byte) (block & 0xFF), (byte) (block >> 8) //16-bit block number, little endian.
        });

        //Return just the bytes on success, or nothing on failure.
        if (res[0] == 0)
            return Arrays.copyOfRange(res, 1, res.length);
        else
            return new byte[]{};
    }

    /* Finally, we want some high level functions, so that we don't need to keep
       every little detail of the hardware in mind all the time.  These will, of course,
       call the lower level functions above.
     */


    //! Reads data from a native address.
    public byte[] read(int adr, int len) throws IOException {
        Log.d("GoodV", "read(): address=@" + String.format("%04x", adr) + " len=" + len);

        //Special addresses are handled differently.
        if (adr == 0xf867) {
            // This is the Firmware System Control Register, written by backdoor command.
            //Log.v("GoodV", "read(): firmware system control register");
            byte[] early = readBlock8((byte) 0xFF);
            early=Arrays.copyOf(early, len);

            //When they only want one byte, we're already done.
            if(len==1)
                return early;

            //Concatenate in the rest.
            byte[] late = read(adr+1, len-1);
            System.arraycopy(late, 0, early, 1, late.length);

            return early;
        }

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
            }catch(ArrayIndexOutOfBoundsException e){
                Log.e("GoodV", "Array index failure.  Hoping for the best.");
                return new byte[]{};
            }
        }


        //Native block size is the easiest.
        if (len == blocklen) {
            //Log.v("GoodV", "<read(): native block size - calling readBlock16()");

            //Automatically falls to 8-bit blocks when needed.
            int block = adr2block(adr);
            if (block == -1) //Illegal adr.
                return new byte[]{};

            //Return valid adr.
            return readBlock16(adr2block(adr));
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
                Log.e("GoodV", "read(): failed to read block address=@" + String.format("%04x", adr + i) + " len=" + blocklen);
                return new byte[]{};
            }

            //Copy up to blocklen or buffer length bytes over.
            for (int j = 0; j < blocklen && i < len; i++, j++) {
                res[i] = chunk[j];
            }
        }

        return res;
    }

    //! Writes data to a native address.
    public boolean write(int adr, byte[] data) throws IOException {
        Log.v("GoodV", String.format("Writing %d bytes to 0x%04X.", data.length, adr));


        //Special addresses are handled differently.
        if (adr == 0xf867) {
            // This is the Firmware System Control Register, written by backdoor command.
            boolean res=writeBlock8((byte) 0xFF,
                    new byte[]{(byte) 0x95, data[0], 0x00, 0x00,
                            0x00, 0x00, 0x00, 0x00
                    });

            // Read back the new blocklength.
            readF867();

            //Return result on failure or if done.
            if(!res || data.length==1){
                Log.e("GoodV", "Returning from write with res="+res);
                return res;
            }

            //Write the rest of the data.
            return write(adr+1, Arrays.copyOfRange(data, 1, data.length));
        }

        //This does nothing if page doesn't need to be changed.
        setPageForAdr(adr);

        //Unaligned writes work by incorporating a read of the prior block.
        if (adr % blocklen != 0) {
            //First we grab the block before our data.
            int earlyblockadr = adr - (adr % blocklen);
            byte[] earlydata = read(earlyblockadr, adr % blocklen);
            byte[] totaldata = Arrays.copyOf(earlydata, data.length + (adr % blocklen));
            System.arraycopy(data, 0, totaldata, adr % blocklen, data.length);


            return write(earlyblockadr, totaldata);
        }


        for (int i = 0; i < data.length; i += blocklen) {
            int blockadr = adr2block(adr + i);

            if(blockadr==-1){
                Log.e("GoodV", "Illegal block adr.");
                return false;
            }

            if (data.length - i >= blocklen) {
                byte[] blockdata = Arrays.copyOfRange(data, i, i + blocklen);
                if (!writeBlock16(blockadr, blockdata)) {
                    Log.e("GoodV", String.format("Error writing %d bytes to 0x%04x: %s",
                            blockdata.length, blockadr,
                            GoodVUtil.byteArrayToHex(blockdata)
                    ));
                    return false;
                }
            } else { //Not enough data for the full block, so we'll have to copy it in.
                byte[] blockdata = readBlock16(blockadr);
                System.arraycopy(data, i, blockdata, 0, data.length % blocklen);


                if (!writeBlock16(blockadr, blockdata)) {
                    Log.e("GoodV", String.format("Error writing %d bytes to 0x%04x: %s (unfilled)",
                            blockdata.length, blockadr,
                            GoodVUtil.byteArrayToHex(blockdata)
                    ));
                    return false;
                }
            }
        }

        return true;
    }

    public byte[] exec(int adr) throws IOException {
        Log.e("GoodV", "This tag type doesn't yet support shellcode.");
        throw new IOException("Unable to execute shellcode.");
    }

    //! Returns true if JTAG is locked.
    public boolean isJTAGLocked() throws IOException {
        byte[] lockstring = this.read(0xFFD0, 4);
        Log.v("GoodV", "JTAG Lock String: " + GoodVUtil.byteArrayToHex(lockstring));


        //FF's and 00's are unlocked.
        return !Arrays.equals(lockstring, new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF})
                && !Arrays.equals(lockstring, new byte[]{0x00, 0x00, 0x00, 0x00});//Any other value is locked.
    }

    //! Locks or unlocks JTAG.
    public void setJTAGLocked(boolean lockstate) throws IOException {
        if(lockstate)
            write(0xFFD0, new byte[]{0x55, 0x55, 0x55, 0x55});  //Locked
        else
            write(0xFFD0, new byte[]{0x00, 0x00, 0x00, 0x00});  //Unlocked
    }


    //! Sets the page number, iff it needs to be changed.
    public void setPage(int newpage) throws IOException {
        //No pages in 8-byte mode.
        if (blocklen == 8) return;
        //Don't fix it if it ain't broke.
        if (page == newpage) return;
        //Can't set page except in stock.
        if (!variant.equals("FRL")) return;

        //Flip the bit and write it to memory.  Confusingly it's the inverse of the page number.
        byte f867 = read(0xf867, 1)[0];

        if (newpage == 0)
            f867 |= 2;
        else
            f867 &= ~2;

        //Write it back.
        write(0xf867, new byte[]{f867});

        //Update our internal page number.
        page = newpage;
    }

    //! Sets the page number to that of an address.
    public void setPageForAdr(int adr) throws IOException {
        //Pages don't matter in 8-byte mode.
        if (blocklen == 8)
            return;

        /* Paging only affects Flash memory, where everything before FC34 is in page 0. */
        if (adr < 0xFC34)
            setPage(0);
        else
            setPage(1);

        Log.v("GoodV", String.format("Page %d for adr %04x.", page, adr));
    }

    /* And some very high level functions, that probably ought to be done outside of this class. */

    //! Dumps a region of memory as an TI TXT format, popular with the MSP430.
    public String readTITXT(int adr, int len) throws IOException {
        StringBuilder dump = new StringBuilder();
        dump.append(String.format("@%04x", adr));

        Log.d("GoodV", "readTITXT: reading address=" + String.format("@%04x", adr) + " len=" + len);
        byte[] data = read(adr, len);

        if (data.length != len) {
            Log.e("GoodV", String.format("Requested %d bytes but got %d.", len, data.length));
            return ""; //Empty string for unreadable regions.
        }

        for (int i = 0; i < len; i++) {
            if (i % 16 == 0)
                dump.append(String.format("\n%02x", data[i]));
            else
                dump.append(String.format(" %02x", data[i]));
        }


        return dump.toString();
    }

    //! Writes a TITXT file to memory.
    public boolean writeTITXT(String txt) throws IOException {
        /* TI's TXT format is rather poorly defined, but in general each line should be either
           (1) an address preceded by @, (2) a line of up to 16 bytes, or (3) the litter q, which
           ends the document.

           Parsing this, we can simply forget about linebreaks and treat each item as a word.
           Words beginning with @ set the address, words of just two bytes are data bytes in hex,
           and the letter q ends the transaction.

           As a safety check, we should probably overwrite the RESET vector with 0xFFFF first
           if the total write will touch the IVT, so that a failed write will repair the IVT
           rather than leave stale pointers.
         */
        StringTokenizer tokens = new StringTokenizer(txt);

        //Target address, length, and data.
        int adr = 0;
        int len = 0;
        byte[] data = new byte[0x10000];

        while (tokens.hasMoreTokens()) {
            //Grab the next word.
            String word = tokens.nextToken();

            if (word.length() == 2) {
                //Load new byte into the buffer.
                data[len++] = (byte) Integer.parseInt(word, 16);
            } else if (word.equalsIgnoreCase("x")) {
                // Ending on the 'x' token, so we Execute the last batch of code.
                exec(adr);
            } else {
                //Not a data byte, so we if we have data, we need to flush it.
                if (len > 0) {
                    if (!write(adr, Arrays.copyOf(data, len)))
                        return false;
                }
                adr = len = 0;
            }

            if (word.equalsIgnoreCase("q")) {
                // Ending on the 'q' token, so we're done.
                return true;
            } else if (word.indexOf('@') == 0) {
                // This is an address.
                adr = Integer.parseInt(word.substring(1), 16);
            }
        }

        Log.e("GoodV", "TI-TXT file doesn't end with q.");
        return false;
    }

    //! Erases the tag.
    public abstract void erase() throws IOException;

    //! Dumps the tag as a string.
    public String dumpTITXT() throws IOException {
        return readTITXT(baseadr, blockcount * blocklen) + "\nq";
    }

    //! Gets the tags info as a user-readable string.
    public String getInfo() throws IOException {
        String info =
                "" +
                        "INFO:     " + GoodVUtil.byteArrayToHex(getRawInfo()) + "\n\n" +
                        "SERIAL:   " + GoodVUtil.byteArrayToHex(getSerialNumber()) + "\n" +
                        "VARIANT:  " + variant + "\n" +
                        "BLOCKLEN: " + blocklen + "\n" +
                        "PAGE:     " + page + "\n";

        return info;
    }
}
