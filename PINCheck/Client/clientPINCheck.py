#!/usr/bin/python3


#### INF648 - Javacard Project
# clientPINCheck.py
# Author: Marwan Nour | marwan.nour@polytechnique.edu

import sys
from smartcard.System import readers

# Debug mode
DEBUG = False
if len(sys.argv) == 2:
    if sys.argv[1] == "-d" or sys.argv[1] == "--debug":
        DEBUG = True
        print("Debug mode: ON")
elif len(sys.argv) > 2:
    print("Too many arguments provided. Exiting...")
    sys.exit(1)


# Connect reader
r=readers()
connection=r[0].createConnection()
connection.connect()
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
    1 - Verify PIN
    2 - Deposit
    3 - Debit
    4 - Check Balance
    9 - Exit
    ==========================
    """
    print(banner)
    

    invalid_count = 0
    pin_valid = False

    # Loop until exit or failure
    while True:
        choice = input("\nPlease input the type of operation:")
        
        if choice == "1":
            if pin_valid:
                print("PIN Already validated")
                continue
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
                break
            else:
                # print(PIN_num)
                DATA = []
                for i in range(len(PIN_num)):
                    num = PIN_num[i]
                    try:
                        num_int = int(num)
                        DATA.append(num_int)
                    except ValueError:
                        break

                data, sw1, sw2 = connection.transmit([CLA, INS, P1, P2, Lc]+ DATA)
                
                if DEBUG:
                    print(f"DATA = {DATA}")
                    print("Verify:")
                    print(hex(sw1), hex(sw2), data, "\n")

                if(sw1 == 0x90 and sw2 == 0x00):
                    pin_valid = True
                    print("PIN Valid")
                
                elif(sw1 == 0x63 and sw2 == 0x00):
                    invalid_count += 1
                    print("PIN Invalid")
                    if DEBUG:
                        print(f"invalid count (current sesion) = {invalid_count}")


        elif choice == "2":
            ####  Deposit ####
            CLA   = 0xB0
            INS   = 0x30
            P1    = 0x00
            P2    = 0x00
            Lc    = 0x01
            DATA  = []
            # Le    = Not applicable
            tx_amount = input("Please input the amount you wish to deposit: ")
            try:
                tx_amount_int = int(tx_amount)
                DATA.append(tx_amount_int)
            except ValueError:
                break

            data, sw1, sw2 = connection.transmit([CLA, INS, P1, P2, Lc]+ DATA)
            
            if DEBUG:
                print(f"DATA = {DATA}")
                print("Deposit:")
                print(hex(sw1), hex(sw2), data, "\n")

            # Check PIN
            if(sw1 == 0x63 and sw2 == 0x01):
                print("Please validated your PIN first")
            elif(sw1 == 0x6A and sw2 == 0x83):
                print("Transaction value exceeded. Please input a number < 50.")
            elif(sw1 == 0x6A and sw2 == 0x84):
                print("Maximum balance (1000) reached.")
            elif(sw1 == 0x90 and sw2 == 0x00):
                print("Operation successful")
            
        
        elif choice == "3":
            ####  Debit ####
            CLA   = 0xB0
            INS   = 0x40
            P1    = 0x00
            P2    = 0x00
            Lc    = 0x01
            DATA  = [] 
            # Le    = Not applicable
            tx_amount = input("Please input the amount you wish to debit: ")
            try:
                tx_amount_int = int(tx_amount)
                DATA.append(tx_amount_int)
            except ValueError:
                break

            data, sw1, sw2 = connection.transmit([CLA, INS, P1, P2, Lc]+ DATA)
            
            if DEBUG:
                print(f"DATA = {DATA}")
                print("Debit:")
                print(hex(sw1), hex(sw2), data, "\n")

            # Check PIN
            if(sw1 == 0x63 and sw2 == 0x01):
                print("Please validated your PIN first")
            elif(sw1 == 0x6A and sw2 == 0x83):
                print("Transaction value exceeded. Please input a number < 50.")
            elif(sw1 == 0x6A and sw2 == 0x85):
                print("Negative balance error. Please input a number <= current balance.")
            elif(sw1 == 0x90 and sw2 == 0x00):
                print("Operation successful")

        elif choice == "4":
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