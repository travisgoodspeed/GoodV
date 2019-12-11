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

## Prebuilt Releases

Prebuilt APKs of GoodV for those wanting to use it with RF430 chips
are available in the
[Releases](https://github.com/travisgoodspeed/GoodV/releases) section
of the Github page.

## Building in Android Studio

GoodV is developed with [Android
Studio](https://developer.android.com/studio).  Begin by choosing
"Check out project from Version Control", then give
`https://github.com/travisgoodspeed/GoodV` as the URL.

With a little luck it will simply compile, but there might be a
mismatch of the Gradle version and the IDE's plugin.  If the Gradle
sync fails, either try the "Install missing platforms and sync
project" option or update the project's build target to something more
modern.  Sometimes it helps to jump forward in fewer target revisions,
rather than trying to go all the way to the latest major release.

## Building with the Gradle Wrapper

While Android Studio is damned handy as an IDE, some of us stubborn
ol' fogeys demand a way to compile code from the command line like a
proper gentleman would.  For that, use `./gradlew` on Unix or
`graldew.bat` in Windows.

You can compile with `./gradlew clean` and then `./gradlew assemble`,
then install with `./gradlew installDebug`.  For a full list of
targets run `./gradle tasks`, and for convenience, a `Makefile`
wrapper is also included.

## Related Projects

[GoodTag](https://github.com/travisgoodspeed/goodtag) is an open
hardware reference design for the RF430FRL152H, as well as a firmware
development kit for the platform.

[The GoodTag Wiki](https://github.com/travisgoodspeed/goodtag/wiki)
contains plenty of documentation about the RF430FRL152H and related
chips.
