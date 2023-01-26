#include <SDL.h>

int android_main(int argc, char *argv[]) {
    // m8c/main.c
    SDL_SetHint(SDL_HINT_ORIENTATIONS, "LandscapeLeft LandscapeRight");
    return SDL_main(argc, argv);
}
