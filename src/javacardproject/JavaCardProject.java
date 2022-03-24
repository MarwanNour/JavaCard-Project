package javacardproject;

import java.util.prefs.BackingStoreException;

import javacard.framework.APDU;
import javacard.framework.Applet;
import javacard.framework.ISO7816;
import javacard.framework.ISOException;
import javacard.framework.Util;
import javacard.framework.JCSystem;
import javacard.framework.OwnerPIN;
import javacard.security.*;
import javacardx.crypto.Cipher; 


/** 
 * JavaCard Project for the Embedded Security course at École Polytechnique.
 * 
 * @author Marwan Nour | marwan.nour@polytechnique.edu 
 * 
 * References:
 * - https://www.oracle.com/java/technologies/java-card/writing-javacard-applet.html
 * - https://docs.oracle.com/javacard/3.0.5/api/javacard/framework/OwnerPIN.html
 * - https://www.oracle.com/java/technologies/java-card/writing-javacard-applet2.html
 * - https://docs.oracle.com/en/java/javacard/3.1/jc_api_srvc/api_classic/javacard/framework/ISO7816.html
 * - https://github.com/OpenCryptoProject/JCMathLib/blob/master/JCMathLib/ext/java_card_kit-2_2_2-win/samples/src/com/sun/javacard/samples/wallet/Wallet.java
 * 
 **/

public class JavaCardProject extends Applet {
    public static final byte CLA_MONAPPLET = (byte) 0xB0;

    /******** Operations (INS) ********/
    public static final byte VERIFY = (byte) 0x20;
    public static final byte DEPOSIT = (byte) 0x30;
    public static final byte DEBIT = (byte) 0x40;
    public static final byte GET_BALANCE = (byte) 0x50;

    public static final byte GET_INFO = (byte) 0x51;
    public static final byte DES_ENCRYPT = (byte) 0x52;

    /******** Decl ********/
    // PIN decl
    OwnerPIN pin;
    // Balance decl
    short balance;
    
    // User info
    private static byte[] card_num;
    private static byte[] card_user_name;

    DESKey m_desKey;

    // 3DES key
    byte [] keydata  = {(byte)0x11,(byte)0x11,(byte)0x11,(byte)0x11,(byte)0x11,(byte)0x11,(byte)0x11,(byte)0x11, (byte)0x11,(byte)0x11,(byte)0x11,(byte)0x11,(byte)0x11,(byte)0x11,(byte)0x11,(byte)0x11, (byte)0x11,(byte)0x11,(byte)0x11,(byte)0x11,(byte)0x11,(byte)0x11,(byte)0x11,(byte)0x11} ;





    /******** PIN specs ********/
    // PIN length
    public static final byte MAX_PIN_SIZE = (byte) 4;
    // Max number of incorrect try before PIN is blocked
    public static final byte PIN_TRY_LIMIT = (byte) 5;
    
    /******** Tx specs ********/
    // Max balance
    public static final short MAX_BALANCE = 1000;
    // Max tx amount 
    public static final short MAX_TX_AMOUNT = 50;

    /******** Returned SW ********/
    // PIN verification failed SW
    public static final short SW_VERIFICATION_FAILED = 0x6300; 
    // PIN verification required SW
    public static final short SW_PIN_VERIFICATION_REQUIRED = 0x6301;
    // Invalid amount (amount < 0  || amount > MAX_TX_AMOUNT)
    public static final short SW_INVALID_TX_AMOUNT = 0x6A83;
    // Balance exceeded
    static final short SW_EXCEED_MAX_BALANCE = 0x6A84;
    // Balance negative
    static final short SW_NEGATIVE_BALANCE = 0x6A85;

    // DES data input bad length 
    public static final short SW_CIPHER_DATA_LENGTH_BAD = 0x6A86;
    

    /**
     * Constructor
     * 
     * @param bArray
     * @param bOffset
     * @param bLength
     */
    private JavaCardProject(byte bArray[], short bOffset, byte bLength) {
        // Create PIN
        pin = new OwnerPIN(PIN_TRY_LIMIT,   MAX_PIN_SIZE);
        
        byte aidLength = bArray[bOffset];           // aid length
        bOffset = (short) (bOffset+aidLength+1);
        byte controlLength = bArray[bOffset];       // control info length
        bOffset = (short) (bOffset+controlLength+1);
        byte dataLength = bArray[bOffset];          // applet data length
        
        // The installation parameters contain the PIN
        // initialization value
        pin.update(bArray, (short)(bOffset+1), (byte) 0x04);


        // Build DES key object
        m_desKey = (DESKey)KeyBuilder.buildKey(KeyBuilder.TYPE_DES, KeyBuilder.LENGTH_DES3_3KEY, false);
        m_desKey.setKey(keydata,(short)0);

        // For testing: setting up DES key through installation (need to fix offsets)
        // Set DES key value
        // bOffset = (short) (bOffset + aLen + 1);
        // desKey.setKey(bArray, (short) (bOffset + 5));

        card_num = new byte[(short)4];          // fixed card number size (4 bytes long) 
        bOffset = (short) (bOffset + 0x04 + 1);     // offset + pin size
        Util.arrayCopy(bArray, bOffset, card_num, (short)0, (byte) card_num.length);


        card_user_name = new byte[(short) 15];  // fixed card user's name (15 bytes long)
        bOffset = (short) (bOffset + 4);        // offset + card num size
        Util.arrayCopy(bArray, bOffset, card_user_name, (short) 0, (byte) card_user_name.length);
        
        register();

    }

    public void des_encrypt(APDU apdu){
        byte[] buffer = apdu.getBuffer(); // To parse the apdu

        byte byteRead = (byte)(apdu.setIncomingAndReceive());

        if((byteRead % 8) != 0){
            ISOException.throwIt(SW_CIPHER_DATA_LENGTH_BAD);
        }

        // byte [] input = {(byte)0x22,(byte)0x22,(byte)0x22}; // For testing only
        byte [] output = new byte [100];

        // Create cipher object
        Cipher m_encryptCipher = Cipher.getInstance(Cipher.ALG_DES_ECB_ISO9797_M1, false);
        m_encryptCipher.init(m_desKey, Cipher.MODE_ENCRYPT);
        
        // encrypt incoming buffer
        m_encryptCipher.doFinal(buffer, ISO7816.OFFSET_CDATA, byteRead, output, (short)0);

        // copy to outgoing buffer
        Util.arrayCopyNonAtomic(output, (short)0, buffer, ISO7816.OFFSET_CDATA, byteRead);

        // send outgoing buffer
        apdu.setOutgoingAndSend(ISO7816.OFFSET_CDATA, byteRead);
    }

    /***
     * Gets card info (card number and card user name)
     * 
     * @param apdu
     */
    public void getInfo(APDU apdu){
        byte[] buffer = apdu.getBuffer(); // To parse the apdu
            
        Util.arrayCopyNonAtomic(card_num, 
                                (short) 0,
                                buffer, 
                                (short) 0, 
                                (short) card_num.length);
        
        Util.arrayCopyNonAtomic(card_user_name,
                                (short) 0,
                                buffer,
                                (short) card_num.length,
                                (short) card_user_name.length);


        apdu.setOutgoingAndSend((short) 0, (short) (card_num.length + card_user_name.length));
    }

    public static void install(byte bArray[], short bOffset, byte bLength) throws ISOException {
        new JavaCardProject(bArray, bOffset, bLength);
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

    /**
     * Deposit money into account
     * 
     * @param apdu
     */
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
        if(creditAmount < 0 || creditAmount > MAX_TX_AMOUNT){
            ISOException.throwIt(SW_INVALID_TX_AMOUNT);
        }

        // check new balance
        if((short)(balance + creditAmount) > MAX_BALANCE){
            ISOException.throwIt(SW_EXCEED_MAX_BALANCE);
        }

        // credit the amount
        balance = (short)(balance + creditAmount);

    }

    /**
     * Debit money from account
     * 
     * @param apdu
     */
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
        if(debitAmount < 0 || debitAmount > MAX_TX_AMOUNT){
            ISOException.throwIt(SW_INVALID_TX_AMOUNT);
        }

        if((short)(balance - debitAmount) < (short) 0){
            ISOException.throwIt(SW_NEGATIVE_BALANCE);
        }

        // debit the amount
        balance = (short)(balance - debitAmount);
    }

    /**
     * Retrieve balance from account
     * 
     * @param apdu
     */
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

    /**
     * Verify PIN. Required before any other operations (debit, deposit, getBalance)
     * 
     * @param apdu
     */
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

            case GET_INFO:
                getInfo(apdu);
                break;

            case DES_ENCRYPT:
                des_encrypt(apdu);
                break;

            default:
                ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
        }
    }



}
