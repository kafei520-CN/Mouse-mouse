#define WIN32_LEAN_AND_MEAN
#define NOMINMAX
#define _CRT_SECURE_NO_WARNINGS
#include <windows.h>
#include <winsock2.h>
#include <hidsdi.h>
#include <setupapi.h>
#include <devguid.h>
#include <cfgmgr32.h>

#include <atomic>
#include <algorithm>
#include <cctype>
#include <cstdio>
#include <cstring>
#include <map>
#include <mutex>
#include <string>
#include <thread>
#include <vector>

#pragma comment(lib, "ws2_32.lib")
#pragma comment(lib, "hid.lib")
#pragma comment(lib, "user32.lib")
#pragma comment(lib, "setupapi.lib")
#pragma comment(lib, "cfgmgr32.lib")

#define PORT 19091
#define WM_SPLITTER_READY (WM_APP + 1)

struct ClientInfo {
    SOCKET socket;
    bool active;
};

static std::map<int, ClientInfo> clients;
static std::mutex clientsMutex;
static SOCKET listenSocket = INVALID_SOCKET;
static std::atomic_bool running = true;

static std::map<HANDLE, int> deviceIds;
static std::mutex deviceIdsMutex;
static int nextDeviceId = 1;

struct SetupApiDeviceInfo {
    std::string friendlyName;
    std::string manufacturer;
};

static std::vector<std::string> collectDeviceListLines();

static void sendToDevice(int deviceId, const char* data, int len) {
    std::lock_guard<std::mutex> lock(clientsMutex);
    auto it = clients.find(deviceId);
    if (it == clients.end() || !it->second.active) return;
    int sent = send(it->second.socket, data, len, 0);
    if (sent == SOCKET_ERROR) {
        it->second.active = false;
        closesocket(it->second.socket);
    }
}

static std::string recvLine(SOCKET s) {
    std::string line;
    char c;
    while (recv(s, &c, 1, 0) == 1) {
        if (c == '\n') break;
        if (c != '\r') line += c;
    }
    return line;
}

static void sendTextLine(SOCKET socket, const std::string& line) {
    std::string payload = line + "\n";
    send(socket, payload.c_str(), static_cast<int>(payload.size()), 0);
}

static void handleClient(SOCKET clientSocket) {
    std::string msg = recvLine(clientSocket);
    if (msg == "LIST") {
        std::vector<std::string> lines = collectDeviceListLines();
        for (const std::string& line : lines) {
            sendTextLine(clientSocket, line);
        }
        sendTextLine(clientSocket, "END");
        closesocket(clientSocket);
        return;
    }

    if (msg.find("CLAIM:") != 0) {
        closesocket(clientSocket);
        return;
    }

    int deviceId = 0;
    try {
        deviceId = std::stoi(msg.substr(6));
    } catch (...) {
        closesocket(clientSocket);
        return;
    }

    {
        std::lock_guard<std::mutex> lock(clientsMutex);
        if (clients.count(deviceId) && clients[deviceId].active) {
            std::string rej = "REJECTED\n";
            send(clientSocket, rej.c_str(), (int)rej.size(), 0);
            closesocket(clientSocket);
            return;
        }
        clients[deviceId] = {clientSocket, true};
    }

    std::string ack = "ACK:" + std::to_string(deviceId) + "\n";
    send(clientSocket, ack.c_str(), (int)ack.size(), 0);

    char buf[1];
    while (running && recv(clientSocket, buf, 1, 0) > 0) {}

    {
        std::lock_guard<std::mutex> lock(clientsMutex);
        auto it = clients.find(deviceId);
        if (it != clients.end() && it->second.socket == clientSocket) {
            it->second.active = false;
        }
    }
    closesocket(clientSocket);
}

static void acceptLoop() {
    while (running) {
        SOCKET s = accept(listenSocket, nullptr, nullptr);
        if (s == INVALID_SOCKET) {
            if (running) Sleep(50);
            continue;
        }
        std::thread(handleClient, s).detach();
    }
}

static std::string wideToUtf8(const std::wstring& value) {
    if (value.empty()) return "";

    int size = WideCharToMultiByte(CP_UTF8, 0, value.c_str(), -1, nullptr, 0, nullptr, nullptr);
    if (size <= 1) return "";

    std::string result(size - 1, '\0');
    WideCharToMultiByte(CP_UTF8, 0, value.c_str(), -1, result.data(), size, nullptr, nullptr);
    return result;
}

static std::string trim(std::string value) {
    size_t start = value.find_first_not_of(" \t\r\n");
    if (start == std::string::npos) return "";

    size_t end = value.find_last_not_of(" \t\r\n");
    return value.substr(start, end - start + 1);
}

static std::string toUpperCopy(std::string value) {
    std::transform(value.begin(), value.end(), value.begin(), [](unsigned char c) {
        return static_cast<char>(std::toupper(c));
    });
    return value;
}

static bool containsIgnoreCase(const std::string& haystack, const std::string& needle) {
    if (needle.empty()) return true;
    return toUpperCopy(haystack).find(toUpperCopy(needle)) != std::string::npos;
}

static bool isGenericDeviceName(const std::string& value) {
    std::string upper = toUpperCopy(trim(value));
    if (upper.empty() || upper == "UNKNOWN DEVICE") return true;

    static const char* genericNames[] = {
        "USB INPUT DEVICE",
        "HID DEVICE",
        "HID KEYBOARD DEVICE",
        "HID MOUSE DEVICE",
        "HID-COMPLIANT DEVICE",
        "HID-COMPLIANT KEYBOARD",
        "HID-COMPLIANT KEYBOARD DEVICE",
        "HID-COMPLIANT MOUSE",
        "HID-COMPLIANT MOUSE DEVICE",
        "KEYBOARD DEVICE",
        "MOUSE DEVICE"
    };

    for (const char* genericName : genericNames) {
        if (upper == genericName) return true;
    }
    return false;
}

static std::string combineManufacturerAndProduct(const std::string& manufacturer, const std::string& product) {
    std::string cleanManufacturer = trim(manufacturer);
    std::string cleanProduct = trim(product);

    if (cleanManufacturer.empty()) return cleanProduct;
    if (cleanProduct.empty()) return cleanManufacturer;
    if (containsIgnoreCase(cleanProduct, cleanManufacturer)) return cleanProduct;
    return cleanManufacturer + " " + cleanProduct;
}

static std::string normalizeInstancePath(std::string rawPath) {
    if (rawPath.rfind("\\\\?\\", 0) == 0) rawPath.erase(0, 4);
    if (rawPath.rfind("\\??\\", 0) == 0) rawPath.erase(0, 4);

    size_t guidPos = rawPath.find("#{");
    if (guidPos != std::string::npos) rawPath.erase(guidPos);

    std::replace(rawPath.begin(), rawPath.end(), '#', '\\');
    std::transform(rawPath.begin(), rawPath.end(), rawPath.begin(), [](unsigned char c) {
        return static_cast<char>(std::toupper(c));
    });
    return rawPath;
}

using HidStringReader = BOOLEAN(__stdcall*)(HANDLE, PVOID, ULONG);

static std::string readHidString(const std::string& rawPath, HidStringReader reader) {
    HANDLE deviceHandle = CreateFileA(rawPath.c_str(), 0, FILE_SHARE_READ | FILE_SHARE_WRITE,
                                      nullptr, OPEN_EXISTING, 0, nullptr);
    if (deviceHandle == INVALID_HANDLE_VALUE) return "";

    wchar_t buffer[256] = {};
    bool ok = reader(deviceHandle, buffer, sizeof(buffer)) == TRUE;
    CloseHandle(deviceHandle);
    if (!ok || buffer[0] == L'\0') return "";

    return wideToUtf8(buffer);
}

static std::string readProductString(const std::string& rawPath) {
    return readHidString(rawPath, HidD_GetProductString);
}

static std::string readManufacturerString(const std::string& rawPath) {
    return readHidString(rawPath, HidD_GetManufacturerString);
}

static SetupApiDeviceInfo querySetupApiInfo(const std::string& normalizedPath) {
    SetupApiDeviceInfo info;
    HDEVINFO devInfoSet = SetupDiGetClassDevsW(nullptr, nullptr, nullptr, DIGCF_ALLCLASSES | DIGCF_PRESENT);
    if (devInfoSet == INVALID_HANDLE_VALUE) return info;

    SP_DEVINFO_DATA devInfoData = {};
    devInfoData.cbSize = sizeof(SP_DEVINFO_DATA);

    for (DWORD index = 0; SetupDiEnumDeviceInfo(devInfoSet, index, &devInfoData); index++) {
        wchar_t instanceId[MAX_DEVICE_ID_LEN] = {};
        if (CM_Get_Device_IDW(devInfoData.DevInst, instanceId, MAX_DEVICE_ID_LEN, 0) != CR_SUCCESS) continue;

        std::string normalizedId = toUpperCopy(wideToUtf8(instanceId));

        if (normalizedId != normalizedPath) continue;

        wchar_t buffer[256] = {};
        if (SetupDiGetDeviceRegistryPropertyW(devInfoSet, &devInfoData, SPDRP_FRIENDLYNAME,
                                              nullptr, reinterpret_cast<PBYTE>(buffer), sizeof(buffer), nullptr)
            || SetupDiGetDeviceRegistryPropertyW(devInfoSet, &devInfoData, SPDRP_DEVICEDESC,
                                                 nullptr, reinterpret_cast<PBYTE>(buffer), sizeof(buffer), nullptr)) {
            info.friendlyName = wideToUtf8(buffer);
        }

        std::memset(buffer, 0, sizeof(buffer));
        if (SetupDiGetDeviceRegistryPropertyW(devInfoSet, &devInfoData, SPDRP_MFG,
                                              nullptr, reinterpret_cast<PBYTE>(buffer), sizeof(buffer), nullptr)) {
            info.manufacturer = wideToUtf8(buffer);
        }
        break;
    }

    SetupDiDestroyDeviceInfoList(devInfoSet);
    return info;
}

static std::string extractHexToken(const std::string& rawPath, const char* token) {
    std::string upperPath = toUpperCopy(rawPath);
    size_t pos = upperPath.find(token);
    if (pos == std::string::npos) return "";

    pos += std::strlen(token);
    size_t end = pos;
    while (end < upperPath.size() && std::isxdigit(static_cast<unsigned char>(upperPath[end]))) {
        end++;
    }
    return end > pos ? upperPath.substr(pos, end - pos) : "";
}

static std::string formatVidPidSuffix(const std::string& vid, const std::string& pid) {
    if (vid.empty() && pid.empty()) return "";
    if (vid.empty()) return " (PID:" + pid + ")";
    if (pid.empty()) return " (VID:" + vid + ")";
    return " (VID:" + vid + " PID:" + pid + ")";
}

static std::string buildDisplayName(const std::string& rawPath,
                                    const std::string& hidManufacturer,
                                    const std::string& hidProduct,
                                    const SetupApiDeviceInfo& setupInfo) {
    std::string hidName = combineManufacturerAndProduct(hidManufacturer, hidProduct);
    std::string setupName = trim(setupInfo.friendlyName);
    if (!setupInfo.manufacturer.empty() && !containsIgnoreCase(setupName, setupInfo.manufacturer)) {
        setupName = combineManufacturerAndProduct(setupInfo.manufacturer, setupName);
    }

    if (!isGenericDeviceName(hidName)) {
        return hidName;
    }
    if (!isGenericDeviceName(setupName)) {
        return setupName;
    }

    std::string productWithSetupManufacturer = combineManufacturerAndProduct(setupInfo.manufacturer, hidProduct);
    if (!isGenericDeviceName(productWithSetupManufacturer)) {
        return productWithSetupManufacturer;
    }

    std::string manufacturerOnly = trim(setupInfo.manufacturer.empty() ? hidManufacturer : setupInfo.manufacturer);
    if (!manufacturerOnly.empty()) {
        return manufacturerOnly;
    }

    std::string vid = extractHexToken(rawPath, "VID_");
    std::string pid = extractHexToken(rawPath, "PID_");
    if (!vid.empty() || !pid.empty()) {
        return "Unknown Device";
    }
    return rawPath;
}

// Query a readable device name instead of the raw device path.
static std::string getDeviceFriendlyName(HANDLE rawDevice) {
    UINT size = 0;
    if (GetRawInputDeviceInfoA(rawDevice, RIDI_DEVICENAME, nullptr, &size) != 0 || size == 0) {
        return "Unknown Device";
    }

    std::string rawPath(size, '\0');
    UINT result = GetRawInputDeviceInfoA(rawDevice, RIDI_DEVICENAME, rawPath.data(), &size);
    if (result == static_cast<UINT>(-1)) return "Unknown Device";
    if (!rawPath.empty() && rawPath.back() == '\0') rawPath.pop_back();

    std::string hidManufacturer = readManufacturerString(rawPath);
    std::string hidProduct = readProductString(rawPath);
    SetupApiDeviceInfo setupInfo = querySetupApiInfo(normalizeInstancePath(rawPath));
    std::string vid = extractHexToken(rawPath, "VID_");
    std::string pid = extractHexToken(rawPath, "PID_");

    std::string displayName = buildDisplayName(rawPath, hidManufacturer, hidProduct, setupInfo);
    if (containsIgnoreCase(displayName, "(VID:")) {
        return displayName;
    }
    return displayName + formatVidPidSuffix(vid, pid);
}

// Stable device id for Java CLAIM mapping.
static int getOrCreateDeviceId(HANDLE rawDevice) {
    if (rawDevice == nullptr) return 0;
    std::lock_guard<std::mutex> lock(deviceIdsMutex);
    auto it = deviceIds.find(rawDevice);
    if (it != deviceIds.end()) return it->second;

    int id = nextDeviceId++;
    deviceIds[rawDevice] = id;
    return id;
}

static const char* deviceTypeName(DWORD type) {
    if (type == RIM_TYPEKEYBOARD) return "KB";
    if (type == RIM_TYPEMOUSE) return "Mouse";
    return "Other";
}

static std::vector<std::string> collectDeviceListLines() {
    std::vector<std::string> lines;
    UINT count = 0;
    UINT listSize = sizeof(RAWINPUTDEVICELIST);
    if (GetRawInputDeviceList(nullptr, &count, listSize) != 0 || count == 0) return lines;

    std::vector<RAWINPUTDEVICELIST> devices(count);
    if (GetRawInputDeviceList(devices.data(), &count, listSize) == static_cast<UINT>(-1)) return lines;

    for (const RAWINPUTDEVICELIST& dev : devices) {
        if (dev.dwType != RIM_TYPEKEYBOARD && dev.dwType != RIM_TYPEMOUSE) continue;
        int id = getOrCreateDeviceId(dev.hDevice);
        // Show a friendly name instead of the raw path.
        std::string name = getDeviceFriendlyName(dev.hDevice);
        lines.push_back("Device " + std::to_string(id) + " [" + deviceTypeName(dev.dwType) + "]: " + name);
    }
    return lines;
}

// Register keyboard and mouse Raw Input for the hidden window.
static bool registerRawInput(HWND hwnd) {
    RAWINPUTDEVICE devices[2] = {};

    devices[0].usUsagePage = 0x01;
    devices[0].usUsage = 0x06;
    devices[0].dwFlags = RIDEV_INPUTSINK | RIDEV_DEVNOTIFY;
    devices[0].hwndTarget = hwnd;

    devices[1].usUsagePage = 0x01;
    devices[1].usUsage = 0x02;
    devices[1].dwFlags = RIDEV_INPUTSINK | RIDEV_DEVNOTIFY;
    devices[1].hwndTarget = hwnd;

    return RegisterRawInputDevices(devices, 2, sizeof(RAWINPUTDEVICE)) == TRUE;
}

static void sendKeyboardPacket(int deviceId, const RAWKEYBOARD& keyboard) {
    if (keyboard.VKey == 0 || keyboard.VKey == 0xFF) return;

    unsigned char state = (keyboard.Flags & RI_KEY_BREAK) ? 0x01 : 0x00;
    char packet[4] = {};
    packet[0] = 0x01;
    packet[1] = static_cast<char>(keyboard.VKey & 0xFF);
    packet[2] = static_cast<char>(state);
    packet[3] = static_cast<char>(keyboard.Flags & 0xFF);
    sendToDevice(deviceId, packet, sizeof(packet));
}

static void sendMousePacket(int deviceId, const RAWMOUSE& mouse) {
    short state = 0;
    short rolling = 0;

    if (mouse.usButtonFlags & RI_MOUSE_LEFT_BUTTON_DOWN) state |= 0x0001;
    if (mouse.usButtonFlags & RI_MOUSE_LEFT_BUTTON_UP) state |= 0x0002;
    if (mouse.usButtonFlags & RI_MOUSE_RIGHT_BUTTON_DOWN) state |= 0x0004;
    if (mouse.usButtonFlags & RI_MOUSE_RIGHT_BUTTON_UP) state |= 0x0008;
    if (mouse.usButtonFlags & RI_MOUSE_MIDDLE_BUTTON_DOWN) state |= 0x0010;
    if (mouse.usButtonFlags & RI_MOUSE_MIDDLE_BUTTON_UP) state |= 0x0020;
    if (mouse.usButtonFlags & RI_MOUSE_WHEEL) {
        rolling = static_cast<short>(mouse.usButtonData);
    }

    short flags = static_cast<short>(mouse.usFlags);
    int dx = mouse.lLastX;
    int dy = mouse.lLastY;

    if (state == 0 && rolling == 0 && dx == 0 && dy == 0) return;

    char packet[15] = {};
    packet[0] = 0x02;
    std::memcpy(packet + 1, &state, sizeof(state));
    std::memcpy(packet + 3, &flags, sizeof(flags));
    std::memcpy(packet + 5, &rolling, sizeof(rolling));
    std::memcpy(packet + 7, &dx, sizeof(dx));
    std::memcpy(packet + 11, &dy, sizeof(dy));
    sendToDevice(deviceId, packet, 15);
}

static void handleRawInput(LPARAM lParam) {
    UINT size = 0;
    if (GetRawInputData(reinterpret_cast<HRAWINPUT>(lParam), RID_INPUT, nullptr, &size,
                        sizeof(RAWINPUTHEADER)) != 0 || size == 0) {
        return;
    }

    std::vector<BYTE> buffer(size);
    UINT copied = GetRawInputData(reinterpret_cast<HRAWINPUT>(lParam), RID_INPUT, buffer.data(), &size,
                                  sizeof(RAWINPUTHEADER));
    if (copied != size) return;

    RAWINPUT* raw = reinterpret_cast<RAWINPUT*>(buffer.data());
    int deviceId = getOrCreateDeviceId(raw->header.hDevice);
    if (deviceId == 0) return;

    if (raw->header.dwType == RIM_TYPEKEYBOARD) {
        sendKeyboardPacket(deviceId, raw->data.keyboard);
    } else if (raw->header.dwType == RIM_TYPEMOUSE) {
        sendMousePacket(deviceId, raw->data.mouse);
    }
}

static LRESULT CALLBACK WndProc(HWND hwnd, UINT msg, WPARAM wParam, LPARAM lParam) {
    switch (msg) {
        case WM_CREATE:
            if (!registerRawInput(hwnd)) {
                std::printf("Failed to register Raw Input devices: %lu\n", GetLastError());
                PostQuitMessage(1);
                return 0;
            }
            PostMessage(hwnd, WM_SPLITTER_READY, 0, 0);
            return 0;

        case WM_INPUT:
            handleRawInput(lParam);
            return 0;

        case WM_INPUT_DEVICE_CHANGE:
            return 0;

        case WM_SPLITTER_READY:
            std::printf("Raw Input splitter ready on port %d\n", PORT);
            return 0;

        case WM_DESTROY:
            running = false;
            PostQuitMessage(0);
            return 0;
    }

    return DefWindowProc(hwnd, msg, wParam, lParam);
}

static bool startServer() {
    WSADATA wsaData = {};
    if (WSAStartup(MAKEWORD(2, 2), &wsaData) != 0) return false;

    listenSocket = socket(AF_INET, SOCK_STREAM, IPPROTO_TCP);
    if (listenSocket == INVALID_SOCKET) return false;

    int opt = 1;
    setsockopt(listenSocket, SOL_SOCKET, SO_REUSEADDR, reinterpret_cast<char*>(&opt), sizeof(opt));

    sockaddr_in addr = {};
    addr.sin_family = AF_INET;
    addr.sin_addr.s_addr = htonl(INADDR_LOOPBACK);
    addr.sin_port = htons(PORT);

    if (bind(listenSocket, reinterpret_cast<SOCKADDR*>(&addr), sizeof(addr)) == SOCKET_ERROR) return false;
    if (listen(listenSocket, 4) == SOCKET_ERROR) return false;

    std::thread(acceptLoop).detach();
    return true;
}

int main() {
    if (!startServer()) {
        std::printf("Failed to start IPC server: %d\n", WSAGetLastError());
        if (listenSocket != INVALID_SOCKET) closesocket(listenSocket);
        WSACleanup();
        return 1;
    }

    WNDCLASSEXA wc = {};
    wc.cbSize = sizeof(WNDCLASSEXA);
    wc.lpfnWndProc = WndProc;
    wc.hInstance = GetModuleHandleA(nullptr);
    wc.lpszClassName = "MouseRawInputSplitter";

    if (!RegisterClassExA(&wc)) {
        std::printf("Failed to register window class: %lu\n", GetLastError());
        running = false;
        closesocket(listenSocket);
        WSACleanup();
        return 1;
    }

    HWND hwnd = CreateWindowExA(0, wc.lpszClassName, "Mouse Raw Input Splitter",
                                0, 0, 0, 0, 0, nullptr, nullptr, wc.hInstance, nullptr);
    if (hwnd == nullptr) {
        std::printf("Failed to create Raw Input window: %lu\n", GetLastError());
        running = false;
        closesocket(listenSocket);
        WSACleanup();
        return 1;
    }

    MSG msg = {};
    while (GetMessage(&msg, nullptr, 0, 0) > 0) {
        TranslateMessage(&msg);
        DispatchMessage(&msg);
    }

    running = false;
    if (listenSocket != INVALID_SOCKET) closesocket(listenSocket);
    WSACleanup();
    return 0;
}
