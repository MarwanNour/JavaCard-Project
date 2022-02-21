# INF648 - Javacard Project
# clientPINCheck.py
# Author: Marwan Nour | marwan.nour@polytechnique.edu

from smartcard.System import readers
r=readers()
connection=r[0].createConnection()
connection.connect()
# Selection AID
SELECT = [0x00,0xA4,0x04,0x00,0x08]
AID = [0x02,0x02,0x03,0x04,0x05,0x06,0x07,0x07]
apdu = SELECT + AID
data, sw1, sw2 = connection.transmit(apdu)

print("Selection:")
print(hex(sw1),hex(sw2), data)
print()

# #Printing first message
# data, sw1, sw2 = connection.transmit([0xB0,0x00,0x00,0x00,0x00])
# mess1 = ''
# for e in data:
#     mess1 += chr(e)

# print(data)
# print(hex(sw1), hex(sw2))
# print(mess1)

# #Printing second message
# data, sw1, sw2 = connection.transmit([0xB0,0x01,0x00,0x00,0x00])
# mess2 = ''
# for i in range(len(data)-2): 
#     mess2 += chr(data[i])

# print(data)
# print(hex(sw1), hex(sw2))
# mess2 = mess2 + str(data[len(data)-2]) + '.' + str(data[len(data)-1])
# print(mess2)

try:
    invalid_count = 0

    while True:
        supported_str = """"
        Supported Operations:
        1 - Verify PIN
        2 - Deposit
        3 - Debit
        4 - Check Balance
        9 - Exit
        """
        print(supported_str)
        choice = input("Please input the type of operation:\n")
        
        if choice == "1":
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

                print(f"DATA = {DATA}")
                data, sw1, sw2 = connection.transmit([CLA, INS, P1, P2, Lc]+ DATA)
                print("Verify:")
                print(hex(sw1),hex(sw2), data)

                if(sw1 == 0x90 and sw2 == 00):
                    print("PIN Valid")
                
                else:
                    invalid_count += 1
                    print("PIN Invalid")
                    print(f"invalid count = {invalid_count}")

        elif choice == "2":
            ####  Deposit ####
            CLA   = 0xB0
            INS   = 0x30
            P1    = 0x00
            P2    = 0x00
            Lc    = 0x01
            DATA  = [0x04]
            # Le    = Not applicable
            print(f"DATA = {DATA}")
            data, sw1, sw2 = connection.transmit([CLA, INS, P1, P2, Lc]+ DATA)
            print("Deposit:")
            print(hex(sw1),hex(sw2), data)
        
        elif choice == "3":
            ####  Debit ####
            CLA   = 0xB0
            INS   = 0x40
            P1    = 0x00
            P2    = 0x00
            Lc    = 0x04
            DATA  = [0x01, 0x02, 0x03, 0x04]
            # Le    = Not applicable
            data, sw1, sw2 = connection.transmit([CLA, INS, P1, P2, Lc]+ DATA)
            print("Debit:")
            print(hex(sw1),hex(sw2), data)

        elif choice == "4":
            ####  Get Balance ####
            CLA   = 0xB0
            INS   = 0x50
            P1    = 0x00
            P2    = 0x00
            Lc    = 0x00
            # DATA  = Not applicable
            Le    = 0x00
            data, sw1, sw2 = connection.transmit([CLA, INS, P1, P2, Lc, Le])
            print("Balance:")
            print(hex(sw1),hex(sw2), data)
        
        elif choice == "9":
            print("Thank you for using our ATM, enjoy the festival.")
            break

        else:
            print("Wrong choice, try again")

    #Disconnect the reader
    connection.disconnect()

except KeyboardInterrupt:
    #Disconnect the reader
    connection.disconnect()