#ifndef WIN32_LEAN_AND_MEAN
#define WIN32_LEAN_AND_MEAN
#endif

#include <windows.h>

#include <algorithm>
#include <atomic>
#include <cctype>
#include <cstdint>
#include <cstdio>
#include <cstdlib>
#include <memory>
#include <mutex>
#include <sstream>
#include <stdexcept>
#include <string>
#include <vector>

extern "C" {
typedef struct mpv_handle mpv_handle;

typedef enum mpv_format {
    MPV_FORMAT_NONE = 0,
    MPV_FORMAT_STRING = 1,
    MPV_FORMAT_OSD_STRING = 2,
    MPV_FORMAT_FLAG = 3,
    MPV_FORMAT_INT64 = 4,
    MPV_FORMAT_DOUBLE = 5,
} mpv_format;
}

namespace {

HMODULE gModule = nullptr;
constexpr const wchar_t *kWindowClass = L"NuvioPlayerBridgeVideoWindow";

std::wstring toWide(const std::string &value) {
    if (value.empty()) return std::wstring();
    int size = MultiByteToWideChar(CP_UTF8, 0, value.data(), (int)value.size(), nullptr, 0);
    if (size <= 0) return std::wstring();
    std::wstring result((size_t)size, L'\0');
    MultiByteToWideChar(CP_UTF8, 0, value.data(), (int)value.size(), result.data(), size);
    return result;
}

std::string toUtf8(const std::wstring &value) {
    if (value.empty()) return std::string();
    int size = WideCharToMultiByte(CP_UTF8, 0, value.data(), (int)value.size(), nullptr, 0, nullptr, nullptr);
    if (size <= 0) return std::string();
    std::string result((size_t)size, '\0');
    WideCharToMultiByte(CP_UTF8, 0, value.data(), (int)value.size(), result.data(), size, nullptr, nullptr);
    return result;
}

std::wstring moduleDirectory() {
    wchar_t buffer[MAX_PATH] = {};
    DWORD length = GetModuleFileNameW(gModule, buffer, MAX_PATH);
    if (length == 0 || length >= MAX_PATH) return std::wstring();
    std::wstring path(buffer, buffer + length);
    size_t separator = path.find_last_of(L"\\/");
    if (separator == std::wstring::npos) return std::wstring();
    return path.substr(0, separator);
}

std::string trim(std::string value) {
    auto isSpace = [](unsigned char ch) { return std::isspace(ch) != 0; };
    value.erase(value.begin(), std::find_if(value.begin(), value.end(), [&](char ch) { return !isSpace((unsigned char)ch); }));
    value.erase(std::find_if(value.rbegin(), value.rend(), [&](char ch) { return !isSpace((unsigned char)ch); }).base(), value.end());
    return value;
}

std::string lowerCopy(std::string value) {
    std::transform(value.begin(), value.end(), value.begin(), [](unsigned char ch) {
        return (char)std::tolower(ch);
    });
    return value;
}

struct MpvApi {
    using mpv_create_fn = mpv_handle *(*)();
    using mpv_initialize_fn = int (*)(mpv_handle *);
    using mpv_terminate_destroy_fn = void (*)(mpv_handle *);
    using mpv_set_option_fn = int (*)(mpv_handle *, const char *, mpv_format, void *);
    using mpv_set_option_string_fn = int (*)(mpv_handle *, const char *, const char *);
    using mpv_set_property_fn = int (*)(mpv_handle *, const char *, mpv_format, void *);
    using mpv_set_property_string_fn = int (*)(mpv_handle *, const char *, const char *);
    using mpv_get_property_fn = int (*)(mpv_handle *, const char *, mpv_format, void *);
    using mpv_command_fn = int (*)(mpv_handle *, const char **);
    using mpv_error_string_fn = const char *(*)(int);
    using mpv_free_fn = void (*)(void *);

    HMODULE library = nullptr;
    std::string loadFailure;
    mpv_create_fn create = nullptr;
    mpv_initialize_fn initialize = nullptr;
    mpv_terminate_destroy_fn terminateDestroy = nullptr;
    mpv_set_option_fn setOption = nullptr;
    mpv_set_option_string_fn setOptionString = nullptr;
    mpv_set_property_fn setProperty = nullptr;
    mpv_set_property_string_fn setPropertyString = nullptr;
    mpv_get_property_fn getProperty = nullptr;
    mpv_command_fn command = nullptr;
    mpv_error_string_fn errorString = nullptr;
    mpv_free_fn freeValue = nullptr;

    template <typename T>
    T loadSymbol(const char *name) {
        FARPROC symbol = library ? GetProcAddress(library, name) : nullptr;
        if (!symbol) {
            loadFailure = std::string("libmpv-2.dll is missing symbol ") + name;
            return nullptr;
        }
        return reinterpret_cast<T>(symbol);
    }

    void load() {
        if (library) return;

        std::vector<std::wstring> candidates;
        wchar_t envPath[32768] = {};
        DWORD envCapacity = (DWORD)(sizeof(envPath) / sizeof(envPath[0]));
        DWORD envLength = GetEnvironmentVariableW(L"NUVIO_LIBMPV_PATH", envPath, envCapacity);
        if (envLength > 0 && envLength < envCapacity) {
            candidates.emplace_back(envPath, envPath + envLength);
        }

        std::wstring moduleDir = moduleDirectory();
        if (!moduleDir.empty()) {
            candidates.push_back(moduleDir + L"\\libmpv-2.dll");
            candidates.push_back(moduleDir + L"\\..\\native\\libmpv-2.dll");
        }
        candidates.push_back(L"libmpv-2.dll");
        candidates.push_back(L"C:\\Program Files (x86)\\Nuvio\\app\\native\\libmpv-2.dll");

        for (const auto &candidate : candidates) {
            if (candidate.find(L'\\') != std::wstring::npos || candidate.find(L'/') != std::wstring::npos) {
                library = LoadLibraryExW(candidate.c_str(), nullptr, LOAD_WITH_ALTERED_SEARCH_PATH);
            } else {
                library = LoadLibraryW(candidate.c_str());
            }
            if (library) break;
        }

        if (!library) {
            loadFailure = "Unable to load libmpv-2.dll. Bundle it beside player_bridge.dll or set NUVIO_LIBMPV_PATH.";
            return;
        }

        create = loadSymbol<mpv_create_fn>("mpv_create");
        initialize = loadSymbol<mpv_initialize_fn>("mpv_initialize");
        terminateDestroy = loadSymbol<mpv_terminate_destroy_fn>("mpv_terminate_destroy");
        setOption = loadSymbol<mpv_set_option_fn>("mpv_set_option");
        setOptionString = loadSymbol<mpv_set_option_string_fn>("mpv_set_option_string");
        setProperty = loadSymbol<mpv_set_property_fn>("mpv_set_property");
        setPropertyString = loadSymbol<mpv_set_property_string_fn>("mpv_set_property_string");
        getProperty = loadSymbol<mpv_get_property_fn>("mpv_get_property");
        command = loadSymbol<mpv_command_fn>("mpv_command");
        errorString = loadSymbol<mpv_error_string_fn>("mpv_error_string");
        freeValue = loadSymbol<mpv_free_fn>("mpv_free");
    }

    void ensureLoaded() {
        load();
        if (!library || !create || !initialize || !terminateDestroy || !setOption || !setOptionString ||
            !setProperty || !setPropertyString || !getProperty || !command) {
            throw std::runtime_error(loadFailure.empty() ? "libmpv-2.dll failed to load." : loadFailure);
        }
    }

    std::string errorText(int code) const {
        const char *text = errorString ? errorString(code) : nullptr;
        return text ? text : "unknown";
    }
};

MpvApi &mpvApi() {
    static MpvApi api;
    return api;
}

LRESULT CALLBACK videoWindowProc(HWND hwnd, UINT message, WPARAM wParam, LPARAM lParam) {
    switch (message) {
        case WM_ERASEBKGND: {
            RECT rect;
            GetClientRect(hwnd, &rect);
            FillRect((HDC)wParam, &rect, (HBRUSH)GetStockObject(BLACK_BRUSH));
            return 1;
        }
        default:
            return DefWindowProcW(hwnd, message, wParam, lParam);
    }
}

void registerWindowClass() {
    static std::once_flag flag;
    std::call_once(flag, []() {
        WNDCLASSW wc = {};
        wc.lpfnWndProc = videoWindowProc;
        wc.hInstance = gModule;
        wc.hCursor = LoadCursorW(nullptr, IDC_ARROW);
        wc.hbrBackground = (HBRUSH)GetStockObject(BLACK_BRUSH);
        wc.lpszClassName = kWindowClass;
        RegisterClassW(&wc);
    });
}

std::vector<std::string> parseHeadersJson(const char *headersJson) {
    std::vector<std::string> headers;
    if (!headersJson || headersJson[0] == '\0') return headers;

    std::string json(headersJson);
    size_t pos = 0;
    while (true) {
        size_t keyStart = json.find('"', pos);
        if (keyStart == std::string::npos) break;
        size_t keyEnd = json.find('"', keyStart + 1);
        if (keyEnd == std::string::npos) break;
        size_t colon = json.find(':', keyEnd + 1);
        size_t valueStart = json.find('"', colon == std::string::npos ? keyEnd + 1 : colon + 1);
        size_t valueEnd = valueStart == std::string::npos ? std::string::npos : json.find('"', valueStart + 1);
        if (colon == std::string::npos || valueStart == std::string::npos || valueEnd == std::string::npos) break;
        std::string key = trim(json.substr(keyStart + 1, keyEnd - keyStart - 1));
        std::string value = trim(json.substr(valueStart + 1, valueEnd - valueStart - 1));
        if (!key.empty() && !value.empty()) headers.push_back(key + ": " + value);
        pos = valueEnd + 1;
    }
    return headers;
}

struct NativePlayer {
    std::mutex mutex;
    HWND parent = nullptr;
    HWND hwnd = nullptr;
    mpv_handle *mpv = nullptr;
    std::string pendingUrl;
    std::string pendingAudioUrl;
    std::string pendingHeadersJson;
    std::string error;
    std::atomic<bool> closed{false};
    bool playWhenReady = true;
    int resizeMode = 0;

    ~NativePlayer() {
        destroy();
    }

    void setError(const std::string &message) {
        error = message;
    }

    void ensureWindow() {
        if (hwnd && IsWindow(hwnd)) return;
        if (!parent || !IsWindow(parent)) return;
        registerWindowClass();
        hwnd = CreateWindowExW(
            0,
            kWindowClass,
            L"Nuvio Player",
            WS_CHILD | WS_VISIBLE | WS_CLIPSIBLINGS | WS_CLIPCHILDREN,
            0,
            0,
            1,
            1,
            parent,
            nullptr,
            gModule,
            nullptr
        );
    }

    void ensureMpv() {
        if (mpv) return;
        ensureWindow();
        if (!hwnd || !IsWindow(hwnd)) return;

        MpvApi &api = mpvApi();
        api.ensureLoaded();
        mpv = api.create();
        if (!mpv) {
            throw std::runtime_error("mpv_create returned null.");
        }

        int64_t wid = (int64_t)(intptr_t)hwnd;
        int flagNo = 0;
        api.setOption(mpv, "wid", MPV_FORMAT_INT64, &wid);
        api.setOption(mpv, "terminal", MPV_FORMAT_FLAG, &flagNo);
        api.setOptionString(mpv, "osc", "no");
        api.setOptionString(mpv, "input-default-bindings", "yes");
        api.setOptionString(mpv, "input-vo-keyboard", "no");
        api.setOptionString(mpv, "keep-open", "no");
        api.setOptionString(mpv, "force-window", "immediate");
        api.setOptionString(mpv, "vo", "gpu");
        api.setOptionString(mpv, "hwdec", "auto-safe");

        int result = api.initialize(mpv);
        if (result < 0) {
            std::string message = "mpv_initialize failed: " + api.errorText(result);
            api.terminateDestroy(mpv);
            mpv = nullptr;
            throw std::runtime_error(message);
        }
    }

    void loadPending() {
        if (pendingUrl.empty()) return;
        ensureMpv();
        if (!mpv) return;

        MpvApi &api = mpvApi();
        std::vector<std::string> headers = parseHeadersJson(pendingHeadersJson.c_str());
        if (!headers.empty()) {
            std::string joined;
            for (size_t i = 0; i < headers.size(); i++) {
                if (i > 0) joined += ",";
                joined += headers[i];
            }
            api.setPropertyString(mpv, "http-header-fields", joined.c_str());
        }

        const char *cmd[] = {"loadfile", pendingUrl.c_str(), "replace", nullptr};
        int result = api.command(mpv, cmd);
        if (result < 0) {
            throw std::runtime_error("mpv loadfile failed: " + api.errorText(result));
        }
        int paused = playWhenReady ? 0 : 1;
        api.setProperty(mpv, "pause", MPV_FORMAT_FLAG, &paused);
    }

    void destroy() {
        std::lock_guard<std::mutex> lock(mutex);
        closed = true;
        if (mpv) {
            mpvApi().terminateDestroy(mpv);
            mpv = nullptr;
        }
        if (hwnd && IsWindow(hwnd)) {
            DestroyWindow(hwnd);
        }
        hwnd = nullptr;
        parent = nullptr;
    }
};

NativePlayer *asPlayer(void *handle) {
    return reinterpret_cast<NativePlayer *>(handle);
}

template <typename T>
T getProperty(NativePlayer *player, const char *name, mpv_format format, T fallback) {
    if (!player || !player->mpv) return fallback;
    T value = fallback;
    if (mpvApi().getProperty(player->mpv, name, format, &value) < 0) return fallback;
    return value;
}

std::string getStringProperty(NativePlayer *player, const char *name) {
    if (!player || !player->mpv) return std::string();
    char *raw = nullptr;
    if (mpvApi().getProperty(player->mpv, name, MPV_FORMAT_STRING, &raw) < 0 || !raw) return std::string();
    std::string value(raw);
    if (mpvApi().freeValue) mpvApi().freeValue(raw);
    return value;
}

int64_t trackCount(NativePlayer *player) {
    return getProperty<int64_t>(player, "track-list/count", MPV_FORMAT_INT64, 0);
}

std::string trackPropertyName(int64_t index, const char *property) {
    char buffer[128];
    std::snprintf(buffer, sizeof(buffer), "track-list/%lld/%s", (long long)index, property);
    return std::string(buffer);
}

int64_t resolveTrackListIndex(NativePlayer *player, const char *type, int logicalIndex) {
    int current = 0;
    int64_t count = trackCount(player);
    for (int64_t i = 0; i < count; i++) {
        std::string trackType = lowerCopy(getStringProperty(player, trackPropertyName(i, "type").c_str()));
        if (trackType == type) {
            if (current == logicalIndex) return i;
            current++;
        }
    }
    return -1;
}

int countTracksByType(NativePlayer *player, const char *type) {
    int result = 0;
    int64_t count = trackCount(player);
    for (int64_t i = 0; i < count; i++) {
        if (lowerCopy(getStringProperty(player, trackPropertyName(i, "type").c_str())) == type) result++;
    }
    return result;
}

int trackIdAt(NativePlayer *player, const char *type, int logicalIndex) {
    int64_t index = resolveTrackListIndex(player, type, logicalIndex);
    if (index < 0) return -1;
    return (int)getProperty<int64_t>(player, trackPropertyName(index, "id").c_str(), MPV_FORMAT_INT64, -1);
}

const char *trackTextAt(NativePlayer *player, const char *type, int logicalIndex, const char *property) {
    thread_local std::string text;
    int64_t index = resolveTrackListIndex(player, type, logicalIndex);
    if (index < 0) {
        text.clear();
        return nullptr;
    }
    text = getStringProperty(player, trackPropertyName(index, property).c_str());
    if (text.empty() && std::string(property) == "title") {
        text = getStringProperty(player, trackPropertyName(index, "lang").c_str());
    }
    return text.empty() ? nullptr : text.c_str();
}

int isTrackSelected(NativePlayer *player, const char *type, int logicalIndex) {
    int64_t index = resolveTrackListIndex(player, type, logicalIndex);
    if (index < 0) return 0;
    return getProperty<int>(player, trackPropertyName(index, "selected").c_str(), MPV_FORMAT_FLAG, 0);
}

void commandNoThrow(NativePlayer *player, const char **cmd) {
    if (!player || !player->mpv) return;
    (void)mpvApi().command(player->mpv, cmd);
}

} // namespace

BOOL APIENTRY DllMain(HMODULE module, DWORD reason, LPVOID) {
    if (reason == DLL_PROCESS_ATTACH) {
        gModule = module;
        DisableThreadLibraryCalls(module);
    }
    return TRUE;
}

#define NUVIO_EXPORT extern "C" __declspec(dllexport)

NUVIO_EXPORT void *nuvio_player_create() {
    return new NativePlayer();
}

NUVIO_EXPORT void nuvio_player_destroy(void *player) {
    delete asPlayer(player);
}

NUVIO_EXPORT void nuvio_player_show(void *handle, long long hwnd) {
    NativePlayer *player = asPlayer(handle);
    if (!player || player->closed) return;
    std::lock_guard<std::mutex> lock(player->mutex);
    player->parent = (HWND)(intptr_t)hwnd;
    try {
        player->ensureWindow();
        player->ensureMpv();
        player->loadPending();
    } catch (const std::exception &error) {
        player->setError(error.what());
    }
}

NUVIO_EXPORT void nuvio_player_set_bounds(void *handle, int x, int y, int width, int height) {
    NativePlayer *player = asPlayer(handle);
    if (!player || !player->hwnd || !IsWindow(player->hwnd)) return;
    SetWindowPos(player->hwnd, nullptr, x, y, width > 0 ? width : 1, height > 0 ? height : 1, SWP_NOZORDER | SWP_SHOWWINDOW);
}

NUVIO_EXPORT void nuvio_player_load_file(void *handle, const char *url, const char *audioUrl, const char *headersJson) {
    NativePlayer *player = asPlayer(handle);
    if (!player || player->closed) return;
    std::lock_guard<std::mutex> lock(player->mutex);
    player->pendingUrl = url ? url : "";
    player->pendingAudioUrl = audioUrl ? audioUrl : "";
    player->pendingHeadersJson = headersJson ? headersJson : "";
    player->error.clear();
    try {
        player->loadPending();
    } catch (const std::exception &error) {
        player->setError(error.what());
    }
}

NUVIO_EXPORT void nuvio_player_play(void *handle) {
    NativePlayer *player = asPlayer(handle);
    if (!player) return;
    player->playWhenReady = true;
    int paused = 0;
    if (player->mpv) mpvApi().setProperty(player->mpv, "pause", MPV_FORMAT_FLAG, &paused);
}

NUVIO_EXPORT void nuvio_player_pause(void *handle) {
    NativePlayer *player = asPlayer(handle);
    if (!player) return;
    player->playWhenReady = false;
    int paused = 1;
    if (player->mpv) mpvApi().setProperty(player->mpv, "pause", MPV_FORMAT_FLAG, &paused);
}

NUVIO_EXPORT void nuvio_player_seek_to(void *handle, long long positionMs) {
    NativePlayer *player = asPlayer(handle);
    if (!player || !player->mpv) return;
    double seconds = (double)positionMs / 1000.0;
    mpvApi().setProperty(player->mpv, "time-pos", MPV_FORMAT_DOUBLE, &seconds);
}

NUVIO_EXPORT void nuvio_player_seek_by(void *handle, long long offsetMs) {
    NativePlayer *player = asPlayer(handle);
    if (!player || !player->mpv) return;
    std::string offset = std::to_string((double)offsetMs / 1000.0);
    const char *cmd[] = {"seek", offset.c_str(), "relative", "exact", nullptr};
    commandNoThrow(player, cmd);
}

NUVIO_EXPORT void nuvio_player_set_speed(void *handle, float speed) {
    NativePlayer *player = asPlayer(handle);
    if (!player || !player->mpv) return;
    double value = speed;
    mpvApi().setProperty(player->mpv, "speed", MPV_FORMAT_DOUBLE, &value);
}

NUVIO_EXPORT void nuvio_player_set_resize_mode(void *handle, int mode) {
    NativePlayer *player = asPlayer(handle);
    if (!player) return;
    player->resizeMode = mode;
    if (!player->mpv) return;
    const char *keepAspect = mode == 1 ? "no" : "yes";
    mpvApi().setPropertyString(player->mpv, "keepaspect", keepAspect);
    mpvApi().setPropertyString(player->mpv, "video-unscaled", mode == 2 ? "downscale-big" : "no");
}

NUVIO_EXPORT void nuvio_player_retry(void *handle) {
    NativePlayer *player = asPlayer(handle);
    if (!player) return;
    std::lock_guard<std::mutex> lock(player->mutex);
    try {
        player->loadPending();
    } catch (const std::exception &error) {
        player->setError(error.what());
    }
}

NUVIO_EXPORT void nuvio_player_refresh_state(void *) {}
NUVIO_EXPORT int nuvio_player_is_loading(void *handle) {
    NativePlayer *player = asPlayer(handle);
    if (!player || !player->mpv) return player && !player->pendingUrl.empty() ? 1 : 0;
    return getProperty<int>(player, "core-idle", MPV_FORMAT_FLAG, 0);
}
NUVIO_EXPORT int nuvio_player_is_playing(void *handle) {
    NativePlayer *player = asPlayer(handle);
    if (!player || !player->mpv) return 0;
    int paused = getProperty<int>(player, "pause", MPV_FORMAT_FLAG, 1);
    int idle = getProperty<int>(player, "core-idle", MPV_FORMAT_FLAG, 0);
    int ended = getProperty<int>(player, "eof-reached", MPV_FORMAT_FLAG, 0);
    return !paused && !idle && !ended;
}
NUVIO_EXPORT int nuvio_player_is_ended(void *handle) {
    return getProperty<int>(asPlayer(handle), "eof-reached", MPV_FORMAT_FLAG, 0);
}
NUVIO_EXPORT long long nuvio_player_get_position_ms(void *handle) {
    return (long long)(getProperty<double>(asPlayer(handle), "time-pos", MPV_FORMAT_DOUBLE, 0.0) * 1000.0);
}
NUVIO_EXPORT long long nuvio_player_get_duration_ms(void *handle) {
    return (long long)(getProperty<double>(asPlayer(handle), "duration", MPV_FORMAT_DOUBLE, 0.0) * 1000.0);
}
NUVIO_EXPORT long long nuvio_player_get_buffered_ms(void *handle) {
    NativePlayer *player = asPlayer(handle);
    double duration = getProperty<double>(player, "duration", MPV_FORMAT_DOUBLE, 0.0);
    double percent = getProperty<double>(player, "cache-buffering-state", MPV_FORMAT_DOUBLE, 0.0);
    return (long long)(duration * (percent / 100.0) * 1000.0);
}
NUVIO_EXPORT float nuvio_player_get_speed(void *handle) {
    return (float)getProperty<double>(asPlayer(handle), "speed", MPV_FORMAT_DOUBLE, 1.0);
}
NUVIO_EXPORT const char *nuvio_player_get_error(void *handle) {
    NativePlayer *player = asPlayer(handle);
    if (!player || player->error.empty()) return nullptr;
    return player->error.c_str();
}

NUVIO_EXPORT int nuvio_player_get_audio_track_count(void *handle) { return countTracksByType(asPlayer(handle), "audio"); }
NUVIO_EXPORT int nuvio_player_get_audio_track_id(void *handle, int index) { return trackIdAt(asPlayer(handle), "audio", index); }
NUVIO_EXPORT const char *nuvio_player_get_audio_track_label(void *handle, int index) { return trackTextAt(asPlayer(handle), "audio", index, "title"); }
NUVIO_EXPORT const char *nuvio_player_get_audio_track_lang(void *handle, int index) { return trackTextAt(asPlayer(handle), "audio", index, "lang"); }
NUVIO_EXPORT int nuvio_player_is_audio_track_selected(void *handle, int index) { return isTrackSelected(asPlayer(handle), "audio", index); }
NUVIO_EXPORT void nuvio_player_select_audio_track(void *handle, int trackId) {
    NativePlayer *player = asPlayer(handle);
    if (!player || !player->mpv) return;
    std::string value = trackId < 0 ? "no" : std::to_string(trackId);
    mpvApi().setPropertyString(player->mpv, "aid", value.c_str());
}

NUVIO_EXPORT int nuvio_player_get_subtitle_track_count(void *handle) { return countTracksByType(asPlayer(handle), "sub"); }
NUVIO_EXPORT int nuvio_player_get_subtitle_track_id(void *handle, int index) { return trackIdAt(asPlayer(handle), "sub", index); }
NUVIO_EXPORT const char *nuvio_player_get_subtitle_track_label(void *handle, int index) { return trackTextAt(asPlayer(handle), "sub", index, "title"); }
NUVIO_EXPORT const char *nuvio_player_get_subtitle_track_lang(void *handle, int index) { return trackTextAt(asPlayer(handle), "sub", index, "lang"); }
NUVIO_EXPORT int nuvio_player_is_subtitle_track_selected(void *handle, int index) { return isTrackSelected(asPlayer(handle), "sub", index); }
NUVIO_EXPORT void nuvio_player_select_subtitle_track(void *handle, int trackId) {
    NativePlayer *player = asPlayer(handle);
    if (!player || !player->mpv) return;
    std::string value = trackId < 0 ? "no" : std::to_string(trackId);
    mpvApi().setPropertyString(player->mpv, "sid", value.c_str());
}
NUVIO_EXPORT void nuvio_player_set_subtitle_url(void *handle, const char *url) {
    NativePlayer *player = asPlayer(handle);
    if (!player || !player->mpv || !url || url[0] == '\0') return;
    const char *cmd[] = {"sub-add", url, "select", nullptr};
    commandNoThrow(player, cmd);
}
NUVIO_EXPORT void nuvio_player_clear_external_subtitle(void *handle) {
    NativePlayer *player = asPlayer(handle);
    if (!player || !player->mpv) return;
    const char *cmd[] = {"sub-remove", nullptr};
    commandNoThrow(player, cmd);
}
NUVIO_EXPORT void nuvio_player_clear_external_subtitle_and_select(void *handle, int trackId) {
    nuvio_player_clear_external_subtitle(handle);
    nuvio_player_select_subtitle_track(handle, trackId);
}
NUVIO_EXPORT void nuvio_player_apply_subtitle_style(void *handle, const char *textColor, float outlineSize, float fontSize, int subPos) {
    NativePlayer *player = asPlayer(handle);
    if (!player || !player->mpv) return;
    if (textColor) mpvApi().setPropertyString(player->mpv, "sub-color", textColor);
    mpvApi().setPropertyString(player->mpv, "sub-border-size", std::to_string(outlineSize).c_str());
    mpvApi().setPropertyString(player->mpv, "sub-font-size", std::to_string(fontSize).c_str());
    mpvApi().setPropertyString(player->mpv, "sub-pos", std::to_string(subPos).c_str());
}

NUVIO_EXPORT int nuvio_player_is_closed(void *handle) {
    NativePlayer *player = asPlayer(handle);
    return !player || player->closed ? 1 : 0;
}

#define NUVIO_NOOP_VOID(name, signature) NUVIO_EXPORT void name signature {}
#define NUVIO_NOOP_INT(name, signature, value) NUVIO_EXPORT int name signature { return value; }
#define NUVIO_NOOP_STR(name, signature) NUVIO_EXPORT const char *name signature { return nullptr; }

NUVIO_NOOP_VOID(nuvio_player_set_metadata, (void *, const char *, const char *, const char *, int, int, const char *, const char *, const char *))
NUVIO_NOOP_VOID(nuvio_player_set_has_video_id, (void *, int))
NUVIO_NOOP_VOID(nuvio_player_set_is_series, (void *, int))
NUVIO_NOOP_VOID(nuvio_player_show_skip_button, (void *, const char *, long long))
NUVIO_NOOP_VOID(nuvio_player_hide_skip_button, (void *))
NUVIO_NOOP_VOID(nuvio_player_show_next_episode, (void *, int, int, const char *, const char *, int))
NUVIO_NOOP_VOID(nuvio_player_hide_next_episode, (void *))
NUVIO_NOOP_INT(nuvio_player_pop_next_episode_pressed, (void *), 0)
NUVIO_NOOP_INT(nuvio_player_is_addon_subtitles_fetch_requested, (void *), 0)
NUVIO_NOOP_VOID(nuvio_player_set_addon_subtitles_loading, (void *, int))
NUVIO_NOOP_VOID(nuvio_player_clear_addon_subtitles, (void *))
NUVIO_NOOP_VOID(nuvio_player_add_addon_subtitle, (void *, const char *, const char *, const char *, const char *))
NUVIO_NOOP_INT(nuvio_player_pop_subtitle_style_changed, (void *), 0)
NUVIO_NOOP_INT(nuvio_player_get_subtitle_style_color_index, (void *), 0)
NUVIO_NOOP_INT(nuvio_player_get_subtitle_style_font_size, (void *), 16)
NUVIO_NOOP_INT(nuvio_player_get_subtitle_style_outline_enabled, (void *), 1)
NUVIO_NOOP_INT(nuvio_player_get_subtitle_style_bottom_offset, (void *), 15)
NUVIO_NOOP_INT(nuvio_player_pop_sources_open_requested, (void *), 0)
NUVIO_NOOP_INT(nuvio_player_pop_episodes_open_requested, (void *), 0)
NUVIO_NOOP_STR(nuvio_player_pop_source_stream_selected, (void *))
NUVIO_NOOP_INT(nuvio_player_pop_source_filter_changed, (void *), 0)
NUVIO_NOOP_STR(nuvio_player_get_source_filter_value, (void *))
NUVIO_NOOP_INT(nuvio_player_pop_source_reload, (void *), 0)
NUVIO_NOOP_STR(nuvio_player_pop_episode_selected, (void *))
NUVIO_NOOP_STR(nuvio_player_pop_episode_stream_selected, (void *))
NUVIO_NOOP_INT(nuvio_player_pop_episode_filter_changed, (void *), 0)
NUVIO_NOOP_STR(nuvio_player_get_episode_filter_value, (void *))
NUVIO_NOOP_INT(nuvio_player_pop_episode_reload, (void *), 0)
NUVIO_NOOP_INT(nuvio_player_pop_episode_back, (void *), 0)
NUVIO_NOOP_VOID(nuvio_player_set_sources_loading, (void *, int))
NUVIO_NOOP_VOID(nuvio_player_clear_source_streams, (void *))
NUVIO_NOOP_VOID(nuvio_player_add_source_stream, (void *, const char *, const char *, const char *, const char *, const char *, const char *, int))
NUVIO_NOOP_VOID(nuvio_player_clear_source_addon_groups, (void *))
NUVIO_NOOP_VOID(nuvio_player_add_source_addon_group, (void *, const char *, const char *, const char *, int, int))
NUVIO_NOOP_VOID(nuvio_player_set_source_selected_filter, (void *, const char *))
NUVIO_NOOP_VOID(nuvio_player_clear_episodes, (void *))
NUVIO_NOOP_VOID(nuvio_player_add_episode, (void *, const char *, const char *, const char *, const char *, int, int))
NUVIO_NOOP_VOID(nuvio_player_set_episode_streams_loading, (void *, int))
NUVIO_NOOP_VOID(nuvio_player_clear_episode_streams, (void *))
NUVIO_NOOP_VOID(nuvio_player_add_episode_stream, (void *, const char *, const char *, const char *, const char *, const char *, const char *, int))
NUVIO_NOOP_VOID(nuvio_player_clear_episode_addon_groups, (void *))
NUVIO_NOOP_VOID(nuvio_player_add_episode_addon_group, (void *, const char *, const char *, const char *, int, int))
NUVIO_NOOP_VOID(nuvio_player_set_episode_selected_filter, (void *, const char *))
NUVIO_NOOP_VOID(nuvio_player_show_episode_streams, (void *, int, int, const char *))

#undef NUVIO_NOOP_STR
#undef NUVIO_NOOP_INT
#undef NUVIO_NOOP_VOID
#undef NUVIO_EXPORT
