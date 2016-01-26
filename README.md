I2C Sample Application
======================

This example demonstrates the usage of the I2C API by accessing and 
controlling an external I2C EEPROM memory. Application can perform read, 
write and erase actions displaying results in an hexadecimal list view.

Demo requirements
-----------------

To run this example you need:

* One compatible device to the host application.
* A USB connection between the device and the host PC in order to transfer and
  launch the application.
* An external EEPROM memory connected to the I2C port of the development board.
  EEPROM family 24LCXX is recommended.

Demo setup
----------

Make sure the hardware is set up correctly:

1. The device is powered on.
2. The device is connected directly to the PC by the micro USB cable.
3. The EEPROM memory is correctly connected to the I2C connector of the
   development board.

Demo run
--------

The example is already configured, so all you need to do is to build and 
launch the project.

While it is running, the application displays a configuration page with the
following parameters:

* **I2C Interface**: I2C interface where EEPROM memory is connected to the
  module. By default interface 1.
* **Slave Address**: EEPROM Memory slave address within the I2C interface.
  Consult memory datasheet for further information.
  
The application allows you to open the I2C interface connection against the
EEPROM memory. Once connection is established, you can:

* Read EEPROM Data: Reads and displays first 256 bytes of the memory.
* Write EEPROM Data: Writes first 256 bytes of the memory with values from 0
  to 255.
* Erase EEPROM Data: Erases first 256 bytes of the memory by writing FF on them.

Compatible with
---------------

* ConnectCore 6 SBC
* ConnectCore 6 SBC v3

License
-------

Copyright (c) 2014-2016, Digi International Inc. <support@digi.com>

Permission to use, copy, modify, and/or distribute this software for any
purpose with or without fee is hereby granted, provided that the above
copyright notice and this permission notice appear in all copies.

THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.