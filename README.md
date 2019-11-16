Howdy y'all,

Lately I've been playing around with the RF430FRL152H from Texas
Instruments, but preciously few tools were available for communicating
with it, except through custom hardware or by manually typing
commands.  GoodV is an attempt to remedy that, supporting the unique
manufacturer commands of this platform.

For now, it supports reading and writing of memory, and its wrapper
for Android's NfcV class might be handy in writing other RF430
applications.

It has partial support for the RF430TAL152H chip found in some medical
devices, written as part of a research project with Axelle Apvrille,
which we presented as [The Inner Guts of a Connected Glucose Sensor
for
Diabetes](https://github.com/cryptax/talks/blob/master/BlackAlps-2019/glucose-blackalps2019.pdf)
at BlackAlps 2019.

Cheers from Yverdon les Bains,

--Travis Goodspeed

## Related Projects

[GoodTag](https://github.com/travisgoodspeed/goodtag) is an open
hardware reference design for the RF430FRL152H, as well as a firmware
development kit for the platform.

[The GoodTag Wiki](https://github.com/travisgoodspeed/goodtag/wiki)
contains plenty of documentation about the RF430FRL152H and related
chips.
