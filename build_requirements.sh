#!/system/bin/sh

# AutoMod AI - Build Dependencies Script
# Requires: root, Android NDK 27+, Gradle 8.9+

export ANDROID_NDK_HOME=/data/data/com.termux/files/usr/lib/android-ndk
export PATH=$ANDROID_NDK_HOME/toolchains/llvm/prebuilt/linux-x86_64/bin:$PATH
export CC=aarch64-linux-android21-clang
export CXX=aarch64-linux-android21-clang++

echo "[*] Installing build dependencies..."

# Install required packages
pkg update -y
pkg install -y cmake ninja gradle openjdk-21 python3 git wget unzip

# Clone and build R2Frida
echo "[*] Building R2Frida..."
git clone https://github.com/wedikcz/r2-frida /tmp/r2frida
cd /tmp/r2frida
meson setup build --cross-file /tmp/android_cross.txt
ninja -C build
cp build/libr2frida.so /data/data/com.automod.ai/files/

# Clone and build R2Ghidra
echo "[*] Building R2Ghidra..."
git clone https://github.com/wedikcz/r2ghidra /tmp/r2ghidra
cd /tmp/r2ghidra
meson setup build --cross-file /tmp/android_cross.txt
ninja -C build
cp build/libr2ghidra.so /data/data/com.automod.ai/files/

# Build Frida gadget for aarch64
echo "[*] Building Frida gadget..."
git clone https://github.com/frida/frida-gadget /tmp/frida-gadget
cd /tmp/frida-gadget
make gadget-android-arm64
cp build/frida-gadget-android-arm64.so /data/data/com.automod.ai/files/libfrida-gadget.so

# Build APKTool
echo "[*] Installing APKTool..."
wget https://raw.githubusercontent.com/iBotPeaches/Apktool/master/scripts/linux/apktool
chmod +x apktool
mv apktool /data/data/com.termux/files/usr/bin/

# Install uber-apk-signer
echo "[*] Installing APK signer..."
wget https://github.com/patrickfav/uber-apk-signer/releases/download/v1.3.0/uber-apk-signer-1.3.0.jar
mv uber-apk-signer-1.3.0.jar /data/data/com.automod.ai/files/

echo "[+] All dependencies installed successfully"
