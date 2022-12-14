# m8c for Android

![build](https://github.com/v3rm0n/m8c-android/actions/workflows/build.yml/badge.svg)

Android wrapper for the awesome [m8c](https://github.com/laamaa/m8c). 

Nothing gets close to the real thing so get yours from [here](https://dirtywave.com/products/m8-tracker).

## Installing

Check the [releases](https://github.com/v3rm0n/m8c-android/releases) page to install it manually (you need to be able to install from unknown sources).

## Features

- [x] Display
- [x] Game controller input
- [x] Touch screen input
- [x] Audio playback

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
