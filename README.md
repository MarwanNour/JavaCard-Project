# JavaCard Project

Marwan Nour | marwan.nour@polytechnique.edu

JavaCard Project for the Embedded Systems Security course at École Polytechnique.

## Usage
1. Go to the project directory (same directory as an `build.xml` file) and run `ant` to compile the `.java` files into `.cap` files.
2. Install the `.cap` file into your card with `gp --install <applet.cap> --params <pin_in_hex>`
3. Run the Python client in `Client/` directory


Files:
- `JavaCardProject.java`
- `clientJavaCardProject.py` 

Example:
```
        ==========================
        ------ Festival ATM ------

        Supported Operations:
        1 - Verify PIN
        2 - Deposit
        3 - Debit
        4 - Check Balance
        5 - Send to another card
        9 - Exit
        ==========================
        

Please input the type of operation:1
Please enter your pin:1234
PIN Valid

Please input the type of operation:4
Operation successful
Balance = 	364

Please input the type of operation:2
Please input the amount you wish to deposit: 16
Operation successful

Please input the type of operation:4
Operation successful
Balance = 	380

Please input the type of operation:3
Please input the amount you wish to debit: 10
Operation successful

Please input the type of operation:4
Operation successful
Balance = 	370

Please input the type of operation:5
Please input the amount you wish to transfer: 20
Operation successful
Insert or remove a smartcard in the system.
Waiting for 10 seconds

+Inserted:  3B FD 13 00 00 81 31 FE 45 54 3D 31 4A 32 31 33 36 4B 56 32 33 31 DC
-Removed:  3B FD 13 00 00 81 31 FE 45 54 3D 31 4A 32 31 33 36 4B 56 32 33 31 DC
+Inserted:  3B FD 13 00 00 81 31 FE 45 54 3D 31 4A 32 31 33 36 4B 56 32 33 31 DC
Card swapped... Reconnecting
Please enter your pin:1234
PIN Valid
Operation successful

Please input the type of operation:4
Operation successful
Balance = 	60

Please input the type of operation:9
Thank you for using our ATM, enjoy the festival.
```