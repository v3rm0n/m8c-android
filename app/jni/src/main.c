#include <SDL.h>

int android_main(int argc, char *argv[]) {
    SDL_SetHint(SDL_HINT_ORIENTATIONS, "LandscapeLeft LandscapeRight");
    // m8c/main.c
    return SDL_main(argc, argv);
}
