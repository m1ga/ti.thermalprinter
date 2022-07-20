# Ti.Thermalprinter - ESC/POS thermal printer library for Titanium (Android)

Appcelerator Titanium library that implements https://github.com/DantSu/ESCPOS-ThermalPrinter-Android to print with (Bluetooth, TCP, USB) ESC/POS thermal printer

## Installation

* `<module>ti.thermalprinter</module>` in tiapp.xml
* add this to your build.gradle:
```
repositories {
    maven { url 'https://jitpack.io' }
}
```
* `const ThermalPrinter = require("ti.thermalprinter")`

## Constants
* TYPE_BLUETOOTH
* TYPE_TCP
* TYPE_USB

## Methods

* `requestPermissions()`
* `print({connection, text, [ip, port, timeout, dpi, width, cpl});`
  * `connection`: TYPE_BLUETOOTH | TYPE_TCP | TYPE_USB
  * `text`: actual text. See https://github.com/DantSu/ESCPOS-ThermalPrinter-Android#formatted-text--syntax-guide for syntax<br/><br/>

  if `connection` is `TYPE_TCP` you have to set:
  * `ip` (String), `port`, `dpi`, `width` and `cpl` (characters per line)

## Example

```js
const ThermalPrinter = require("ti.thermalprinter");
ThermalPrinter.print({
  connection: ThermalPrinter.TYPE_BLUETOOTH,
  text: "[C]================================\n" +
        "[L]\n" +
        "[L]<font size='tall'>Customer :</font>\n" +
        "[L]User name\n" +
        "[L]Street No 7\n" +
        "[L]1234 place\n" +
        "[L]Tel : +123456\n" +
        "[L]\n" +
        "[C]<qrcode size='20'>http://www.migaweb.de/</qrcode>"
});
```

## TODO

* add support for images
* add "getList" to select a different printer. Currently it will use the first one


## Author

* Michael Gangolf (<a href="https://github.com/m1ga">@MichaelGangolf</a> / <a href="https://www.migaweb.de">Web</a>)

<span class="badge-buymeacoffee"><a href="https://www.buymeacoffee.com/miga" title="donate"><img src="https://img.shields.io/badge/buy%20me%20a%20coke-donate-orange.svg" alt="Buy Me A Coke donate button" /></a></span>
