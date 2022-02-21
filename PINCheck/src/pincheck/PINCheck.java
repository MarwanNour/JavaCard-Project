package pincheck;

import java.util.prefs.BackingStoreException;

import javacard.framework.APDU;
import javacard.framework.Applet;
import javacard.framework.ISO7816;
import javacard.framework.ISOException;
import javacard.framework.Util;
import javacard.framework.JCSystem;
import javacard.framework.OwnerPIN;

/** 
 * Author: Marwan Nour | marwan.nour@polytechnique.edu
 *  
 * Sources:
 * - https://www.oracle.com/java/technologies/java-card/writing-javacard-applet.html
 * - https://docs.oracle.com/javacard/3.0.5/api/javacard/framework/OwnerPIN.html
 * - https://www.oracle.com/java/technologies/java-card/writing-javacard-applet2.html
 * - https://docs.oracle.com/en/java/javacard/3.1/jc_api_srvc/api_classic/javacard/framework/ISO7816.html
 * 
 **/

public class PINCheck extends Applet {
    public static final byte CLA_MONAPPLET = (byte) 0xB0;

    /******** Operations (INS) ********/
    public static final byte VERIFY = (byte) 0x20;
    public static final byte DEPOSIT = (byte) 0x30;
    public static final byte DEBIT = (byte) 0x40;
    public static final byte GET_BALANCE = (byte) 0x50;
    
    /******** Decl ********/
    // PIN decl
    OwnerPIN pin;
    // Balance decl
    short balance;
    
    /******** PIN specs ********/
    // PIN length
    public static final byte MAX_PIN_SIZE = (byte) 4;
    // Max number of incorrect try before PIN is blocked
    public static final byte PIN_TRY_LIMIT = (byte) 5;
    
    /******** Returned SW ********/
    // PIN verification failed SW
    public static final short SW_VERIFICATION_FAILED = 0x6300; 
    // PIN verification required SW
    public static final short SW_PIN_VERIFICATION_REQUIRED = 0x6301;
    // Invalid amount (amount < 0  || amount > MAX_TRANSACTION_AMOUNT)
    public static final short SW_INVALID_AMOUNT = 0x6A83;
    // // Balance exceeded
    // static final short SW_EXCEED_MAX_BALANCE = 0x6A84;
    // Balance negative
    static final short SW_NEGATIVE_BALANCE = 0x6A85;
    

    public static final byte INS_OUTPUT_MESS1 = 0x00;
    public static final byte INS_OUTPUT_MESS2 = 0x01;

    private static final byte[] MESS_1 = { 'H', 'e', 'l', 'l', 'o', ' ', 'm', 'y', ' ', 'n', 'a', 'm', 'e', ' ', 'i',
            's', ' ', 'L', 'e', 'o', 'n' };
    private static final byte[] MESS_2 = { 'T', 'h', 'e', ' ', 'v', 'e', 'r', 's', 'i', 'o', 'n', ' ', 'o', 'f', ' ',
            'm', 'y', ' ', 'J', 'a', 'v', 'a', 'C', 'a', 'r', 'd', ' ', 'A', 'P', 'I', ' ', 'i', 's', ':', ' ' };




    /* Constructor */
    private PINCheck(byte bArray[], short bOffset, byte bLength) {
    // private PINCheck() {
        // Create PIN
        pin = new OwnerPIN(PIN_TRY_LIMIT, MAX_PIN_SIZE);
        // Init PIN
        pin.update(bArray, bOffset, bLength);
        
        register(); // Mandatory to register
    }

    public static void install(byte bArray[], short bOffset, byte bLength) throws ISOException {
        // new PINCheck();
        byte arr[] = {1,2,3,4};
        new PINCheck(arr, (short) 0, (byte) 0x04);
    }


    public boolean select(){
        // Applet declines to be selected if pin is blocked
        if(pin.getTriesRemaining() == 0){
            return false;
        }
        return true;
    }

    public void deselect(){
        // reset pin value
        pin.reset();
    }

    public void deposit(APDU apdu){

        byte[] buffer = apdu.getBuffer(); // To parse the apdu

        // check if pin is verified
        if(!pin.isValidated()){
            ISOException.throwIt(SW_PIN_VERIFICATION_REQUIRED);
        }
        
        // Lc
        byte numBytes = buffer[ISO7816.OFFSET_LC];
        byte byteRead = (byte)(apdu.setIncomingAndReceive());
        
        if((numBytes != 1) || (byteRead != 1)){
            ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
        }

        // get credit from apdu buffer
        byte creditAmount = buffer[ISO7816.OFFSET_CDATA];

        // check credit amount
        if(creditAmount < 0){
            ISOException.throwIt(SW_INVALID_AMOUNT);
        }

        // credit the amount
        balance = (short)(balance + creditAmount);

    }

    public void debit(APDU apdu){

        byte[] buffer = apdu.getBuffer(); // To parse the apdu

        // check if pin is verified
        if(!pin.isValidated()){
            ISOException.throwIt(SW_PIN_VERIFICATION_REQUIRED);
        }
        // Lc
        byte numBytes = buffer[ISO7816.OFFSET_LC];
        byte byteRead = (byte)(apdu.setIncomingAndReceive());
        
        if((numBytes != 1) || (byteRead != 1)){
            ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
        }

        // get debit from apdu buffer
        byte debitAmount = buffer[ISO7816.OFFSET_CDATA];

        // check debit amount
        if(debitAmount < 0){
            ISOException.throwIt(SW_INVALID_AMOUNT);
        }

        if((short)(balance - debitAmount) < (short) 0){
            ISOException.throwIt(SW_NEGATIVE_BALANCE);
        }

        // debit the amount
        balance = (short)(balance - debitAmount);
    }

    public void getBalance(APDU apdu){
        byte[] buffer = apdu.getBuffer(); // To parse the apdu

        // check if pin is verified
        if(!pin.isValidated()){
            ISOException.throwIt(SW_PIN_VERIFICATION_REQUIRED);
        }   

        short Le = apdu.setOutgoing();
        
        if(Le < 2){
            ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
        }

        // length of bytes returned
        apdu.setOutgoingLength((byte) 2);

        // move balance data into buffer
        Util.setShort(buffer, (short) 0, balance);
        
        // send 2-byte balance at offset 0 of the buffer
        apdu.sendBytes((short) 0, (short)2);

    }

    public void verify(APDU apdu){
        byte[] buffer = apdu.getBuffer(); // To parse the apdu
        
        // get pin data
        byte byteRead = (byte)(apdu.setIncomingAndReceive());

        if(pin.check(buffer, ISO7816.OFFSET_CDATA, byteRead) == false){
            ISOException.throwIt(SW_VERIFICATION_FAILED);
        }

    }

    public void process(APDU apdu) throws ISOException {

        if (this.selectingApplet())
            return; // If you are selecting this applet.

        byte[] buffer = apdu.getBuffer(); // To parse the apdu

        if (buffer[ISO7816.OFFSET_CLA] != CLA_MONAPPLET) {
            ISOException.throwIt(ISO7816.SW_CLA_NOT_SUPPORTED);
        }

        switch (buffer[ISO7816.OFFSET_INS]) {

            case INS_OUTPUT_MESS1:

                Util.arrayCopyNonAtomic(MESS_1,
                        (short) 0,
                        buffer,
                        (short) 0,
                        (short) MESS_1.length);

                apdu.setOutgoingAndSend((short) 0, (short) MESS_1.length);

                break;

            case INS_OUTPUT_MESS2:

                Util.arrayCopyNonAtomic(MESS_2,
                        (short) 0,
                        buffer,
                        (short) 0,
                        (short) MESS_2.length);

                short ver = JCSystem.getVersion();

                buffer[(short) MESS_2.length] = (byte) ver;
                buffer[(short) (MESS_2.length + 1)] = (byte) (ver >> 8);

                apdu.setOutgoingAndSend((short) 0, (short) (MESS_2.length + 2));

                break;

            case GET_BALANCE:
                getBalance(apdu);
                break;

            case DEPOSIT:
                deposit(apdu);
                break;

            case DEBIT:
                debit(apdu);
                break;

            case VERIFY:
                verify(apdu);
                break;

            default:
                ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
        }
    }



}
