Java.perform(function() {
    console.log('[AutoMod] License bypass script loaded');
    
    // === PATTERN DETECTION ===
    var licensePatterns = [
        'isLicensed', 'verifyLicense', 'checkLicense', 'validateLicense',
        'hasValidLicense', 'isPurchased', 'isPremium', 'isPro', 'isUnlocked',
        'isCracked', 'isPirated', 'isModded', 'isPatched',
        'hasFullVersion', 'isUserPremium', 'isTrial', 'hasTrialExpired',
        'getLicenseStatus', 'getPurchaseState', 'getSubscriptionStatus',
        'verifyPurchase', 'verifyReceipt', 'verifySignature',
        'checkSignature', 'validateSignature', 'checkSigningCert',
        'getPackageSignature', 'getSigningCertificate'
    ];
    
    // === KNOWN LIBRARIES TO BYPASS ===
    var bypassLibraries = [
        'com.google.android.vending.licensing',
        'com.android.vending.licensing',
        'com.google.android.finsky',
        'com.android.billingclient',
        'com.revenuecat.purchases',
        'com.android.vending.billing',
        'com.anjlab.android.iab.v3',
        'org.onepf.openiab',
        'com.amazon.device.iap',
        'com.google.play.core',
        'com.google.firebase.appcheck',
        'com.scottyab.rootbeer',
        'com.detect.safety',
        'com.secure.checks'
    ];

    // === 1. BYPASS LICENSE CHECK METHODS ===
    function hookLicenseMethods(className) {
        try {
            var targetClass = Java.use(className);
            var methods = targetClass.class.getDeclaredMethods();
            
            methods.forEach(function(method) {
                var methodName = method.getName();
                var match = false;
                
                licensePatterns.forEach(function(pattern) {
                    if (methodName.toLowerCase().indexOf(pattern.toLowerCase()) !== -1) {
                        match = true;
                    }
                });
                
                if (match) {
                    console.log('[AutoMod] Found license method: ' + className + '.' + methodName);
                    
                    try {
                        var overloads = targetClass[methodName].overloads;
                        overloads.forEach(function(overload) {
                            overload.implementation = function() {
                                console.log('[AutoMod] Intercepted: ' + className + '.' + methodName);
                                
                                // Return values based on return type
                                var retType = overload.returnType.name;
                                if (retType === 'boolean' || retType === 'java.lang.Boolean') {
                                    return true;
                                } else if (retType === 'int' || retType === 'java.lang.Integer') {
                                    return 1;
                                } else if (retType === 'long' || retType === 'java.lang.Long') {
                                    return 1;
                                } else if (retType === 'java.lang.String') {
                                    return 'VALID_LICENSE';
                                } else if (retType === 'void') {
                                    return;
                                } else {
                                    return this[methodName].apply(this, arguments);
                                }
                            };
                        });
                    } catch(e) {}
                }
            });
        } catch(e) {}
    }

    // === 2. BYPASS KNOWN LIBRARIES ===
    bypassLibraries.forEach(function(lib) {
        try {
            hookLicenseMethods(lib);
        } catch(e) {}
    });

    // === 3. BYPASS SSL PINNING ===
    function bypassSSLPinning() {
        var SSLContext = Java.use('javax.net.ssl.SSLContext');
        SSLContext.init.overload(
            '[Ljavax.net.ssl.KeyManager;',
            '[Ljavax.net.ssl.TrustManager;',
            'java.security.SecureRandom'
        ).implementation = function(keyManager, trustManager, secureRandom) {
            console.log('[AutoMod] SSLContext.init intercepted - bypassing trust manager');
            var TrustAllManager = Java.registerClass({
                name: 'com.automod.TrustAllManager',
                implements: [Java.use('javax.net.ssl.X509TrustManager')],
                methods: {
                    checkClientTrusted: function(chain, authType) {},
                    checkServerTrusted: function(chain, authType) {},
                    getAcceptedIssuers: function() { return []; }
                }
            });
            this.init(keyManager, [TrustAllManager.$new()], secureRandom);
        };
        
        // Disable hostname verifier
        var HttpsURLConnection = Java.use('javax.net.ssl.HttpsURLConnection');
        HttpsURLConnection.setDefaultHostnameVerifier.implementation = function(v) {};
        
        // Bypass OkHttp
        try {
            var CertificatePinner = Java.use('okhttp3.CertificatePinner');
            CertificatePinner.check.overload('java.lang.String', 'java.util.List').implementation = function(h, p) {};
        } catch(e) {}
        
        // Bypass TrustKit
        try {
            var TrustKit = Java.use('com.datatheorem.android.trustkit.TrustKit');
            TrustKit.initializeWithNetworkSecurityConfiguration.implementation = function(c) {};
            TrustKit.isInitialized.implementation = function() { return false; };
        } catch(e) {}
        
        // Bypass OkHttp3
        try {
            var OkHttpClient = Java.use('okhttp3.OkHttpClient');
            OkHttpClient.newCall.implementation = function(request) {
                console.log('[AutoMod] OkHttp intercepted: ' + request.url());
                return this.newCall(request);
            };
        } catch(e) {}
    }

    // === 4. BYPASS ROOT DETECTION ===
    function bypassRootDetection() {
        // RootBeer
        try {
            var RootBeer = Java.use('com.scottyab.rootbeer.RootBeer');
            RootBeer.isRooted.implementation = function() { return false; };
            RootBeer.detectRootManagementApps.implementation = function() { return false; };
            RootBeer.detectPotentiallyDangerousApps.implementation = function() { return false; };
            RootBeer.detectTestKeys.implementation = function() { return false; };
            RootBeer.checkForBusyBoxBinary.implementation = function() { return false; };
            RootBeer.checkForSuBinary.implementation = function() { return false; };
            RootBeer.checkForMagiskBinary.implementation = function() { return false; };
            RootBeer.checkForNativeLibraryReadAccess.implementation = function() { return false; };
            RootBeer.detectDaemons.implementation = function() { return false; };
        } catch(e) {}
        
        // SafetyNet
        try {
            var SafetyNet = Java.use('com.google.android.gms.safetynet.SafetyNet');
            SafetyNet.isSupported.implementation = function() { return false; };
        } catch(e) {}
        
        // Generic root checks
        try {
            var Runtime = Java.use('java.lang.Runtime');
            Runtime.exec.overload('[Ljava.lang.String;').implementation = function(cmd) {
                var cmdStr = cmd.join(' ');
                if (cmdStr.indexOf('su') !== -1 || cmdStr.indexOf('busybox') !== -1 || 
                    cmdStr.indexOf('magisk') !== -1 || cmdStr.indexOf('supolicy') !== -1) {
                    console.log('[AutoMod] Blocked root check: ' + cmdStr);
                    return null;
                }
                return this.exec(cmd);
            };
        } catch(e) {}
        
        // File.exists check for root files
        try {
            var File = Java.use('java.io.File');
            File.exists.overload().implementation = function() {
                var path = this.getAbsolutePath();
                var blacklist = ['/su', '/magisk', '/supersu', '/xbin/su', '/system/bin/su',
                               '/system/xbin/su', '/sbin/su', '/data/local/xbin/su',
                               '/data/local/bin/su', '/data/local/su',
                               '/system/app/Superuser.apk', '/system/app/SuperSU.apk',
                               '/system/app/Magisk.apk'];
                for (var i = 0; i < blacklist.length; i++) {
                    if (path.indexOf(blacklist[i]) !== -1) return false;
                }
                return this.exists();
            };
        } catch(e) {}
    }

    // === 5. BYPASS EMULATOR DETECTION ===
    function bypassEmulatorDetection() {
        try {
            var Build = Java.use('android.os.Build');
            Build.FINGERPRINT.value = 'google/raven/raven:14/UP1A.231005.007/10747397:user/release-keys';
            Build.MODEL.value = 'Pixel 7 Pro';
            Build.MANUFACTURER.value = 'Google';
            Build.BRAND.value = 'google';
            Build.DEVICE.value = 'raven';
            Build.HARDWARE.value = 'raven';
            Build.PRODUCT.value = 'raven';
            Build.BOARD.value = 'taro';
            Build.BOOTLOADER.value = 'slider-0.1-7430341';
            Build.DISPLAY.value = 'UP1A.231005.007';
            Build.FINGERPRINT.value = 'google/raven/raven:14/UP1A.231005.007/10747397:user/release-keys';
            Build.HOST.value = 'wph5-fed-aaa-0060.google.com';
            Build.ID.value = 'UP1A.231005.007';
            Build.SERIAL.value = 'R5CN90C0Z1R';
            Build.TAGS.value = 'release-keys';
            Build.TYPE.value = 'user';
            Build.USER.value = 'android-build';
            
            // Telephony manager
            var TelephonyManager = Java.use('android.telephony.TelephonyManager');
            TelephonyManager.getDeviceId.implementation = function() { return '352756109123456'; };
            TelephonyManager.getImei.implementation = function() { return '352756109123456'; };
            TelephonyManager.getSubscriberId.implementation = function() { return '310150123456789'; };
            TelephonyManager.getSimSerialNumber.implementation = function() { return '89014103211118510720'; };
            TelephonyManager.getNetworkOperatorName.implementation = function() { return 'T-Mobile'; };
            TelephonyManager.getNetworkCountryIso.implementation = function() { return 'us'; };
            TelephonyManager.getSimOperatorName.implementation = function() { return 'T-Mobile'; };
            TelephonyManager.isNetworkRoaming.implementation = function() { return false; };
        } catch(e) {}
        
        // Network info
        try {
            var WifiInfo = Java.use('android.net.wifi.WifiInfo');
            WifiInfo.getMacAddress.implementation = function() { return '02:00:00:00:00:00'; };
            WifiInfo.getSSID.implementation = function() { return 'GoogleGuest'; };
            WifiInfo.getBSSID.implementation = function() { return '02:00:00:00:00:00'; };
            WifiInfo.getIpAddress.implementation = function() { return 16843009; }; // 1.2.3.1
        } catch(e) {}
    }

    // === 6. BYPASS ANTI-DEBUG ===
    function bypassAntiDebug() {
        // Debug flags
        try {
            var ActivityManager = Java.use('android.app.ActivityManager');
            ActivityManager.getRunningAppProcesses.implementation = function() {
                var result = this.getRunningAppProcesses();
                if (result != null) {
                    for (var i = 0; i < result.size(); i++) {
                        result.get(i).importance.value = 100; // IMPORTANCE_FOREGROUND
                    }
                }
                return result;
            };
        } catch(e) {}
        
        try {
            var Debug = Java.use('android.os.Debug');
            Debug.isDebuggerConnected.implementation = function() { return false; };
            Debug.waitingForDebugger.implementation = function() { return false; };
        } catch(e) {}
        
        // Native debugger detection
        try {
            var Process = Java.use('java.lang.Process');
            var InputStream = Java.use('java.io.InputStream');
        } catch(e) {}
    }

    // === 7. HOOK GAME ENGINES ===
    function hookGameEngines() {
        // Unity
        try {
            var unityPlayer = Java.use('com.unity3d.player.UnityPlayer');
            if (unityPlayer) console.log('[AutoMod] Unity engine detected');
        } catch(e) {}
        
        // Cocos2d
        try {
            var cocos2d = Java.use('org.cocos2dx.lib.Cocos2dxActivity');
            if (cocos2d) console.log('[AutoMod] Cocos2d engine detected');
        } catch(e) {}
        
        // libGDX
        try {
            var gdx = Java.use('com.badlogic.gdx.backends.android.AndroidApplication');
            if (gdx) console.log('[AutoMod] libGDX engine detected');
        } catch(e) {}
    }

    // === 8. NETWORK INTERCEPTION ===
    function hookNetwork() {
        // HttpURLConnection
        try {
            var HttpURLConnection = Java.use('java.net.HttpURLConnection');
            HttpURLConnection.getInputStream.implementation = function() {
                var url = this.getURL();
                console.log('[AutoMod] HTTP GET: ' + url);
                var response = this.getInputStream();
                return response;
            };
            HttpURLConnection.getOutputStream.implementation = function() {
                var url = this.getURL();
                console.log('[AutoMod] HTTP POST: ' + url);
                return this.getOutputStream();
            };
            HttpURLConnection.getResponseCode.implementation = function() {
                var code = this.getResponseCode();
                console.log('[AutoMod] HTTP Response Code: ' + code + ' for ' + this.getURL());
                return code;
            };
        } catch(e) {}
        
        // OkHttp
        try {
            var Callback = Java.use('okhttp3.Callback');
            Callback.onResponse.overload('okhttp3.Call', 'okhttp3.Response').implementation = function(call, response) {
                console.log('[AutoMod] OkHttp Response: ' + call.request().url() + ' -> ' + response.code());
                return this.onResponse(call, response);
            };
        } catch(e) {}
        
        // Socket
        try {
            var Socket = Java.use('java.net.Socket');
            Socket.connect.overload('java.net.SocketAddress', 'int').implementation = function(addr, timeout) {
                console.log('[AutoMod] Socket connect: ' + addr);
                return this.connect(addr, timeout);
            };
        } catch(e) {}
    }

    // === EXECUTE ===
    try {
        bypassSSLPinning();
        bypassRootDetection();
        bypassEmulatorDetection();
        bypassAntiDebug();
        hookNetwork();
        hookGameEngines();
        
        // Scan loaded classes
        Java.enumerateLoadedClasses({
            onMatch: function(className) {
                // Check if it matches license patterns
                for (var i = 0; i < licensePatterns.length; i++) {
                    if (className.indexOf(licensePatterns[i]) !== -1) {
                        console.log('[AutoMod] Suspicious class: ' + className);
                        hookLicenseMethods(className);
                    }
                }
            },
            onComplete: function() {
                console.log('[AutoMod] License scan complete');
            }
        });
        
        console.log('[AutoMod] All bypasses installed successfully');
    } catch(e) {
        console.log('[AutoMod] Error: ' + e);
    }
});
