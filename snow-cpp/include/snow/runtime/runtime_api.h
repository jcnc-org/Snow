#pragma once

extern "C" void snow_runtime_init();
extern "C" void snow_runtime_shutdown();
extern "C" void snow_runtime_drop(void* value);
extern "C" int snow_runtime_start(int (*user_main)());
