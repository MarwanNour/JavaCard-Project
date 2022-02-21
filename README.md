# JavaCard Project

Marwan Nour | marwan.nour@polytechnique.edu

JavaCard Project for the Embedded Systems Security course at École Polytechnique.

## Usage
1. Go to a project directory (same directory as an `build.xml` file) and run `ant` to compile the `.java` files into `.cap` files.
2. Install the `.cap` file into your card with `gp --install <applet.cap>`
3. Run the Python client files corresponding to the project  


## PINCheck

Files:
- `PINCheck.java`
- `clientPINCheck.py` 

Example:
```

    ==========================
    ------ Festival ATM ------

    Supported Operations:
    1 - Verify PIN
    2 - Deposit
    3 - Debit
    4 - Check Balance
    9 - Exit
    ==========================
    

Please input the type of operation:1
Please enter your pin:1234
PIN Valid

Please input the type of operation:4
Operation successful
Balance = 	348

Please input the type of operation:2
Please input the amount you wish to deposit: 25
Operation successful

Please input the type of operation:4
Operation successful
Balance = 	373

Please input the type of operation:3
Please input the amount you wish to debit: 14
Operation successful

Please input the type of operation:4
Operation successful
Balance = 	359

Please input the type of operation:9
Thank you for using our ATM, enjoy the festival.
```