# Samsung TV control

This is a simple Java library/API for controlling a Samsung Smart TV that listens on port 55000.
Samsung TV's built in 2014 and later use encryption and can therefore **not** be controlled by this library!

The protocol information has been gathered from here: http://sc0ty.pl/2012/02/samsung-tv-network-remote-control-protocol/

## Javadoc

Javadoc location: https://mhvis.github.io/samsung-tv-control/javadoc/

## Builds

The latest build can be found here: https://github.com/mhvis/samsung-tv-control/releases/latest

## Usage

See [javadoc](https://mhvis.github.io/samsung-tv-control/javadoc/), short example:

```java
try {
  SamsungRemote remote = new SamsungRemote(String address);
  TVReply reply = remote.authenticate("Toaster"); // Argument is the device name (displayed on television).
  if (reply == TVReply.ALLOWED) {
    remote.keycode("KEY_INFO");
  }
  remote.close();
} catch (IOException e) {
  System.err.println(e.getMessage());
}
```
