# m8c for Android

![build](https://github.com/v3rm0n/m8c-android/actions/workflows/release.yml/badge.svg)

Android wrapper for the awesome [m8c](https://github.com/laamaa/m8c). 

Nothing gets close to the real thing so get yours from [here](https://dirtywave.com/products/m8-tracker).

## Installing

Check the [releases](https://github.com/v3rm0n/m8c-android/releases) page to install it manually (you need to be able to install from unknown sources).

## Features

- [x] Display
- [x] Game controller input
- [x] Touch screen input
- [x] Audio playback

## Buttons

The buttons have two special functions added to them:

- Touch left and right at the same time: reset screen (useful if you have artefacts on the screen)
- Touch up and down at the same time: screen view is closed and you go back to settings (not very useful, disconnecting and closing the app is recommended)

**NB!** When onscreen buttons are hidden then in the landscape mode you can still use the margins on either side of screen as buttons. Layout is the same. Try it out and you'll understand.

## Building

### Prerequisites

- Android SDK/NDK

### Usage

- Run `git submodule update --init` to download all dependencies (libusb, m8c, SDL2)
- Run `./gradlew installDebug` to build and install it to your device

### Example

![Example](/img/m8_android.jpg)

### Links

- [Dirtywave M8 Tracker](https://dirtywave.com/products/m8-tracker)
- [m8c](https://github.com/laamaa/m8c)
- [M8 Headless Firmware](https://github.com/Dirtywave/M8HeadlessFirmware) 
- [usbaudio-android-demo](https://github.com/shenki/usbaudio-android-demo)
