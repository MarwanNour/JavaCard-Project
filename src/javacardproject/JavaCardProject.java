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
 * - https://github.com/OpenCryptoProject/JCMathLib/blob/master/JCMathLib/ext/java_card_kit-2_2_2-win/samples/src/com/sun/javacard/samples/sigMsgRec/SigMsgRecApplet.java
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
    public static final byte SIGN_MSG = (byte) 0x53;

    /******** Decl ********/
    // PIN decl
    OwnerPIN pin;
    // Balance decl
    short balance;
    
    // User info
    private static byte[] card_num;
    private static byte[] card_user_name;

    // Crypto
    DESKey m_desKey;
    Signature m_sessionCBCMAC;
    Signature m_sign;

    RSAPrivateKey m_privateKey;
    RSAPublicKey m_publicKey;
    

    // RSA Keypair data
    private static final byte[] RSA_PUB_KEY_EXP = {(byte)0x01, (byte)0x00, (byte)0x01};
    private static final byte[] RSA_PUB_PRIV_KEY_MOD = { (byte)0xbe, (byte)0xdf, 
        (byte)0xd3, (byte)0x7a, (byte)0x08, (byte)0xe2, (byte)0x9a, (byte)0x58, 
        (byte)0x27, (byte)0x54, (byte)0x2a, (byte)0x49, (byte)0x18, (byte)0xce, 
        (byte)0xe4, (byte)0x1a, (byte)0x60, (byte)0xdc, (byte)0x62, (byte)0x75, 
        (byte)0xbd, (byte)0xb0, (byte)0x8d, (byte)0x15, (byte)0xa3, (byte)0x65, 
        (byte)0xe6, (byte)0x7b, (byte)0xa9, (byte)0xdc, (byte)0x09, (byte)0x11, 
        (byte)0x5f, (byte)0x9f, (byte)0xbf, (byte)0x29, (byte)0xe6, (byte)0xc2, 
        (byte)0x82, (byte)0xc8, (byte)0x35, (byte)0x6b, (byte)0x0f, (byte)0x10, 
        (byte)0x9b, (byte)0x19, (byte)0x62, (byte)0xfd, (byte)0xbd, (byte)0x96, 
        (byte)0x49, (byte)0x21, (byte)0xe4, (byte)0x22, (byte)0x08, (byte)0x08, 
        (byte)0x80, (byte)0x6c, (byte)0xd1, (byte)0xde, (byte)0xa6, (byte)0xd3, 
        (byte)0xc3, (byte)0x8f};

    private static final byte[] RSA_PRIV_KEY_EXP = { (byte)0x84, (byte)0x21, 
        (byte)0xfe, (byte)0x0b, (byte)0xa4, (byte)0xca, (byte)0xf9, (byte)0x7d, 
        (byte)0xbc, (byte)0xfc, (byte)0x0e, (byte)0xa9, (byte)0xbb, (byte)0x7a, 
        (byte)0xbd, (byte)0x7d, (byte)0x65, (byte)0x40, (byte)0x2b, (byte)0x08, 
        (byte)0xc6, (byte)0xdf, (byte)0xc9, (byte)0x4b, (byte)0x09, (byte)0x6a, 
        (byte)0x29, (byte)0x3b, (byte)0xc2, (byte)0x42, (byte)0x88, (byte)0x23, 
        (byte)0x44, (byte)0xaf, (byte)0x08, (byte)0x82, (byte)0x4c, (byte)0xff, 
        (byte)0x42, (byte)0xa4, (byte)0xb8, (byte)0xd2, (byte)0xda, (byte)0xcc, 
        (byte)0xee, (byte)0xc5, (byte)0x34, (byte)0xed, (byte)0x71, (byte)0x01, 
        (byte)0xab, (byte)0x3b, (byte)0x76, (byte)0xde, (byte)0x6c, (byte)0xa2, 
        (byte)0xcb, (byte)0x7c, (byte)0x38, (byte)0xb6, (byte)0x9a, (byte)0x4b, 
        (byte)0x28, (byte)0x01};
        
    // 3DES key
    byte [] keydata  = {(byte)0x11,(byte)0x11,(byte)0x11,(byte)0x11,
                        (byte)0x11,(byte)0x11,(byte)0x11,(byte)0x11, 
                        (byte)0x11,(byte)0x11,(byte)0x11,(byte)0x11,
                        (byte)0x11,(byte)0x11,(byte)0x11,(byte)0x11, 
                        (byte)0x11,(byte)0x11,(byte)0x11,(byte)0x11,
                        (byte)0x11,(byte)0x11,(byte)0x11,(byte)0x11} ;

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

        // Create DES MAC signature object 
        // m_sessionCBCMAC = Signature.getInstance(Signature.ALG_DES_MAC4_NOPAD, false);
        
        // Build DES key object
        m_desKey = (DESKey)KeyBuilder.buildKey(KeyBuilder.TYPE_DES, KeyBuilder.LENGTH_DES3_3KEY, false);
        m_desKey.setKey(keydata,(short)0);

        // Create RSA keys and pair
        // m_privateKey = KeyBuilder.buildKey(KeyBuilder.TYPE_RSA_PRIVATE, KeyBuilder.LENGTH_RSA_1024, false);
        // m_publicKey = KeyBuilder.buildKey(KeyBuilder.TYPE_RSA_PUBLIC, KeyBuilder.LENGTH_RSA_1024, true);
        // m_keyPair = new KeyPair(KeyPair.ALG_RSA, (short) m_publicKey.getSize());

        m_privateKey = (RSAPrivateKey) KeyBuilder.buildKey(KeyBuilder.TYPE_RSA_PRIVATE, KeyBuilder.LENGTH_RSA_512, false);
        m_publicKey = (RSAPublicKey) KeyBuilder.buildKey(KeyBuilder.TYPE_RSA_PUBLIC, KeyBuilder.LENGTH_RSA_512, false);
        m_privateKey.setExponent(RSA_PRIV_KEY_EXP,(short)0,(short)RSA_PRIV_KEY_EXP.length);
        m_privateKey.setModulus(RSA_PUB_PRIV_KEY_MOD,(short)0,(short)RSA_PUB_PRIV_KEY_MOD.length);
        m_publicKey.setExponent(RSA_PUB_KEY_EXP,(short)0,(short)RSA_PUB_KEY_EXP.length);
        m_publicKey.setModulus(RSA_PUB_PRIV_KEY_MOD,(short)0,(short)RSA_PUB_PRIV_KEY_MOD.length);

        // // Generate keys
        // m_keyPair.genKeyPair();
        // // Get key references
        // m_publicKey = m_keyPair.getPublic();
        // m_privateKey = m_keyPair.getPrivate();

        // Create RSA signature object
        Signature m_sign = Signature.getInstance(Signature.ALG_RSA_SHA_PKCS1, false);
        m_sign.init(m_privateKey, Signature.MODE_SIGN);


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

    /**
     * 3DES Encryption (this is just a POC for testing purposes)
     * 
     * @param apdu
     */
    public void tripleDESEncrypt(APDU apdu){
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

    
    // TODO: Fix 
    /**
     * Sign transaction with RSA then encrypt with 3DES
     * 
     * @param apdu
     */
    public void signThenEncryptTransaction(APDU apdu){

        byte[] buffer = apdu.getBuffer();
        byte byteRead = (byte)(apdu.setIncomingAndReceive());

        byte[] output_sig = new byte [100];

        // sign buffer and store in output
        short sigLen = m_sign.sign(buffer, ISO7816.OFFSET_CDATA, byteRead, output_sig, (byte)0); 

        byte [] output_enc = new byte [100];
    
        // Create cipher object
        Cipher m_encryptCipher = Cipher.getInstance(Cipher.ALG_DES_ECB_ISO9797_M1, false);
        m_encryptCipher.init(m_desKey, Cipher.MODE_ENCRYPT);

        // encrypt signed output 
        short encLen = m_encryptCipher.doFinal(buffer, ISO7816.OFFSET_CDATA, sigLen, output_enc, (short)0);

        // // encrypt transaction
        // short tx_len = m_encryptCipher.doFinal(buffer, ISO7816.OFFSET_CDATA, sigLen, output_enc, (short)0);

        // copy to outgoing buffer
        Util.arrayCopyNonAtomic(output_enc, (short)0, buffer, ISO7816.OFFSET_CDATA, encLen);

        // send outgoing buffer
        apdu.setOutgoingAndSend(ISO7816.OFFSET_CDATA, byteRead);

    }

    // TODO: Fix
    /**
     * Sign message from incoming buffer with RSA
     * 
     * @param apdu
     */
    public void signMessage(APDU apdu){
        byte[] buffer = apdu.getBuffer();
        byte byteRead = (byte)(apdu.setIncomingAndReceive());
        
        byte[] output_sig = new byte [100];
        
        // sign buffer and store in output
        short sigLen = m_sign.sign(buffer, ISO7816.OFFSET_CDATA, byteRead, output_sig, (short)0); // TODO: Fix bug here 
        
        // copy to outgoing buffer
        Util.arrayCopyNonAtomic(output_sig, (short)0, buffer, ISO7816.OFFSET_CDATA, sigLen);

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

        // byte [] output = new byte[100];
        
        // // Create cipher object 
        // Cipher m_decryptCipher = Cipher.getInstance(Cipher.ALG_DES_ECB_ISO9797_M2, false);
        // m_decryptCipher.init(m_desKey, Cipher.MODE_DECRYPT);
        
        // // decrypt PIN from incoming buffer
        // m_decryptCipher.doFinal(buffer, ISO7816.OFFSET_CDATA, byteRead, output, (short)0);
    
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
                tripleDESEncrypt(apdu);
                break;
            
            // case SIGN_MSG:
            //     signMessage(apdu);
            //     break;

            default:
                ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
        }
    }



}
