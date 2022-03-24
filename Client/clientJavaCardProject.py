#!/usr/bin/python3


#### INF648 - Javacard Project
# clientJavaCardProject.py
# Author: Marwan Nour | marwan.nour@polytechnique.edu
# 
# References:
# - https://github.com/LudovicRousseau/pyscard/tree/master/smartcard/Examples/
# - 

import sys
import time

from pip import main
from smartcard.System import readers
from smartcard.util import toHexString
from smartcard.CardMonitoring import CardMonitor, CardObserver
from smartcard.Exceptions import NoCardException
from des import DesKey

# Debug mode
DEBUG = False
if len(sys.argv) == 2:
    if sys.argv[1] == "-d" or sys.argv[1] == "--debug":
        DEBUG = True
        print("Debug mode: ON")
elif len(sys.argv) > 2:
    print("Too many arguments provided. Exiting...")
    sys.exit(1)


# Globals
inserted = False
inserted_count = 0


# a simple card observer that prints inserted/removed cards
class transmitObserver(CardObserver):
    """A simple card observer that is notified
    when cards are inserted/removed from the system and
    prints the list of cards
    """

    def update(self, observable, actions):
        (addedcards, removedcards) = actions

        global inserted 
        global inserted_count

        for card in addedcards:
            print("+Inserted: ", toHexString(card.atr))
            inserted = True
            inserted_count += 1

        for card in removedcards:
            print("-Removed: ", toHexString(card.atr))

            inserted = False


if __name__ == "__main__":

    # bArr =  bytes.fromhex("11 11 11 11 11 11 11 11")
    bArr =  bytes.fromhex("11 11 11 11 11 11 11 11 11 11 11 11 11 11 11 11 11 11 11 11 11 11 11 11")
    
    des_key = DesKey(bArr)



    # Connect reader
    r=readers()

    try: 
        connection=r[0].createConnection()
        connection.connect()
    except NoCardException:
        print("Please insert a card")
        sys.exit(1)


    # Selection AID
    SELECT = [0x00,0xA4,0x04,0x00,0x08]
    AID = [0x02,0x02,0x03,0x04,0x05,0x06,0x07,0x07]
    apdu = SELECT + AID
    data, sw1, sw2 = connection.transmit(apdu)

    if DEBUG:
        print("Selection:")
        print(hex(sw1),hex(sw2), data)
        print()

    try:
        banner = """
        ==========================
        ------ Festival ATM ------

        Supported Operations:
        0 - Get card info
        1 - Verify PIN
        2 - Deposit
        3 - Debit
        4 - Check Balance
        5 - Send to another card
        6 - Encrypt data
        9 - Exit
        ==========================
        """
        print(banner)
        

        invalid_count = 0
        pin_valid = False

        def getStatusCode(sw1, sw2):
            status_ok = False

            # Check PIN
            if(sw1 == 0x63 and sw2 == 0x01):
                print("Please validated your PIN first")
            elif(sw1 == 0x6A and sw2 == 0x83):
                print("Transaction value exceeded. Please input a number < 50.")
            elif(sw1 == 0x6A and sw2 == 0x84):
                print("Maximum balance (1000) reached.")
            elif(sw1 == 0x6A and sw2 == 0x85):
                print("Negative balance error. Please input a number <= current balance.")
            elif(sw1 == 0x90 and sw2 == 0x00):
                print("Operation successful")
                status_ok = True

            return status_ok

        def verifyPIN(connection):
            global pin_valid
            if pin_valid:
                print("PIN Already validated")
                return
            ####  Verify ####
            CLA   = 0xB0
            INS   = 0x20
            P1    = 0x00
            P2    = 0x00
            Lc    = 0x04
            # Le    = Not applicable

            # Prompt for PIN
            PIN_num = input("Please enter your pin:")

            if len(PIN_num) != 4:
                print("Invalid PIN length. Exiting...")
                return
            else:
                # print(PIN_num)
                DATA = []
                for i in range(len(PIN_num)):
                    num = PIN_num[i]
                    try:
                        num_int = int(num)
                        DATA.append(num_int)
                    except ValueError:
                        return

                data, sw1, sw2 = connection.transmit([CLA, INS, P1, P2, Lc]+ DATA)
                
                if DEBUG:
                    print(f"DATA = {DATA}")
                    print("Verify:")
                    print(hex(sw1), hex(sw2), data, "\n")

                if(sw1 == 0x90 and sw2 == 0x00):
                    pin_valid = True
                    print("PIN Valid")
                
                elif(sw1 == 0x63 and sw2 == 0x00):
                    global invalid_count
                    invalid_count += 1
                    print("PIN Invalid")
                    if DEBUG:
                        print(f"invalid count (current sesion) = {invalid_count}")

        def deposit(connection, tx_amount):
            ####  Deposit ####
            CLA   = 0xB0
            INS   = 0x30
            P1    = 0x00
            P2    = 0x00
            Lc    = 0x01
            DATA  = [tx_amount]
            # Le    = Not applicable

            data, sw1, sw2 = connection.transmit([CLA, INS, P1, P2, Lc]+ DATA)
            
            if DEBUG:
                print(f"DATA = {DATA}")
                print("Deposit:")
                print(hex(sw1), hex(sw2), data, "\n")

            # Print status code
            getStatusCode(sw1, sw2)
        
        def debit(connection, tx_amount):
            ####  Debit ####
            CLA   = 0xB0
            INS   = 0x40
            P1    = 0x00
            P2    = 0x00
            Lc    = 0x01
            DATA  = [tx_amount] 
            # Le    = Not applicable

            data, sw1, sw2 = connection.transmit([CLA, INS, P1, P2, Lc]+ DATA)
            
            if DEBUG:
                print(f"DATA = {DATA}")
                print("Debit:")
                print(hex(sw1), hex(sw2), data, "\n")

            # Print status code
            tx_status = getStatusCode(sw1, sw2)
            return tx_status

        # Loop until exit or failure
        while True:
            choice = input("\nPlease input the type of operation:")
            
            if choice == "0":
                ####  Get card info ####
                CLA   = 0xB0
                INS   = 0x51
                P1    = 0x00
                P2    = 0x00
                Lc    = 0x00
                Le    = 0x00 

                data, sw1, sw2 = connection.transmit([CLA, INS, P1, P2, Le])
                
                if DEBUG:
                    print("Card Info:")
                    print(hex(sw1), hex(sw2), data, "\n")  

                if(sw1 == 0x90 and sw2 == 0x00):
                    card_num = ""
                    for e in data[0:4]:
                        # print(e)
                        card_num += str(e)
                    
                    card_user_name = ""
                    for e in data[4::] :
                        # print(e)
                        card_user_name += chr(e)

                    print(f""" 
                    Card Number: {card_num} \t\t Card User Name: {card_user_name}
                    """)
            
                else:
                    print("Card info Error")

            elif choice == "1":               # PIN Verification
                verifyPIN(connection)

            elif choice == "2":             # Deposit
                tx_amount_str = input("Please input the amount you wish to deposit: ")
                try:
                    tx_amount = int(tx_amount_str)
                except ValueError:
                    break 

                deposit(connection, tx_amount)
                
            elif choice == "3":             # Debit

                tx_amount_str = input("Please input the amount you wish to debit: ")
                try:
                    tx_amount = int(tx_amount_str)
                except ValueError:
                    break 

                debit(connection, tx_amount)

            elif choice == "4":             # Balance
                ####  Get Balance ####
                CLA   = 0xB0
                INS   = 0x50
                P1    = 0x00
                P2    = 0x00
                Lc    = 0x00
                # DATA  = Not applicable
                Le    = 0x02
                data, sw1, sw2 = connection.transmit([CLA, INS, P1, P2, Lc, Le])
                
                if DEBUG:
                    print("Balance:")
                    print(hex(sw1), hex(sw2), data, "\n")

                # Check PIN
                if(sw1 == 0x63 and sw2 == 0x01):
                    print("Please validated your PIN first")
                elif(sw1 == 0x90 and sw2 == 0x00):
                    print("Operation successful")
                    # Format balance data
                    data_merged = (data[0] << 8)  + data[1]
                    print(f"Balance = \t{data_merged}")
            
            elif choice == "5":             # Send to card
                #### Send to another Card ####

                if DEBUG:
                    print(f"inserted : {inserted}, inserted_count {inserted_count}")

                tx_amount_str = input("Please input the amount you wish to transfer: ")
                try:
                    tx_amount = int(tx_amount_str)
                except ValueError:
                    break 

                # Debit from current card
                tx_status_ok = debit(connection, tx_amount)
                
                # Check status before proceeding (to avoid creating money)
                if tx_status_ok == False:
                    print("Transaction failed... Exiting")
                    continue
                
                print("Insert or remove a smartcard in the system.")
                print("Waiting for 10 seconds")
                print("")
                cardmonitor = CardMonitor()
                cardobserver = transmitObserver()
                cardmonitor.addObserver(cardobserver)

                time.sleep(10)

                cardmonitor.deleteObserver(cardobserver)

                # Check if card was removed and re-inserted
                if inserted and inserted_count == 2:
                    
                    if DEBUG:
                        print(f"inserted : {inserted}, inserted_count {inserted_count}")
                    
                    # reset pin valid value 
                    pin_valid = False

                    # Disconnect and Connect
                    print("Card swapped... Reconnecting")
                    connection.disconnect()
                    
                    try: 
                        connection=r[0].createConnection()
                        connection.connect()
                    except NoCardException:
                        print("Please insert a card")
                        sys.exit(1)

                    # Selection AID
                    SELECT = [0x00,0xA4,0x04,0x00,0x08]
                    AID = [0x02,0x02,0x03,0x04,0x05,0x06,0x07,0x07]
                    apdu = SELECT + AID
                    data, sw1, sw2 = connection.transmit(apdu)

                    # Ask for PIN
                    verifyPIN(connection)

                elif not inserted: # no card inside reader
                    print("Card removed... Aborting")
                    break


                # Deposit to other card (same tx amount)
                deposit(connection, tx_amount)

                # reset inserted and inserted_count
                inserted = False
                inserted_count = 0
    
                if DEBUG:
                    print(f"inserted : {inserted}, inserted_count {inserted_count}")

            elif choice == "6":
                #### Encrypt ####
                print("enc")
                CLA     = 0xB0
                INS     = 0x52
                P1      = 0x00
                P2      = 0x00
                DATA    = [0x42, 0x41, 0x42, 0x41, 0x41, 0x42, 0x41, 0x42] # Test: BOB. TODO Change it
                Lc      = len(DATA)

                print(f"Message to encrypt: {DATA}")
                enc_msg = des_key.encrypt(bytes(DATA))
                print(f"Encrypted message: {enc_msg}")

                if DEBUG:
                    # Dec test
                    dec_msg = des_key.decrypt(enc_msg)
                    print(f"Decrypted msg (local) : {dec_msg}")

                data, sw1, sw2 = connection.transmit([CLA, INS, P1, P2, Lc] + DATA)
                if DEBUG:
                    print("Card Info:")
                    print(hex(sw1), hex(sw2), data, "\n")

                if(sw1 == 0x90 and sw2 == 0x00):
                    msg = ""
                    for e in data:
                        msg += str(hex(e)) + " "
                    
                    msg = msg[0:len(msg) - 1]
                    print(msg)

                    values = bytearray(data)
                    
                    dec_msg = des_key.decrypt(bytes(values))
                    print(f"Decrypted msg from card: {dec_msg}")

            elif choice == "9":
                print("Thank you for using our ATM, enjoy the festival.")
                break

            else:
                print("Wrong choice, try again")
                print(banner)


        #Disconnect the reader
        connection.disconnect()

    except KeyboardInterrupt:
        #Disconnect the reader
        connection.disconnect()