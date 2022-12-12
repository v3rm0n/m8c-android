# m8c for Android

![build](https://github.com/v3rm0n/m8c-android/actions/workflows/build.yml/badge.svg)

Android wrapper for the awesome [m8c](https://github.com/laamaa/m8c). Nothing gets close to the real thing so get yours from [here](https://dirtywave.com/products/m8-tracker).

# Features

- [x] Display
- [x] Game controller input
- [x] Audio (partial) 
- [ ] Android touch screen input

## Prerequisites
- Android SDK
- Android NDK

## Usage

- Run `git submodule update --init` to download all dependencies (libusb, m8c, SDL2)
- Run `./gradlew installDebug` to build and install it to your device_

## Example

![Example](/img/m8_android.jpg)

## Links

- [Dirtywave M8 Tracker](https://dirtywave.com/products/m8-tracker)
- [m8c](https://github.com/laamaa/m8c)
- [M8 Headless](https://github.com/Dirtywave/M8HeadlessFirmware) 
