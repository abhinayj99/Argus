#include <BLEDevice.h>
#include <BLEUtils.h>
#include <BLEServer.h>
#include <BLE2902.h>
#include "WiFi.h"

#define SERVICE_UUID        ""
#define CHARACTERISTIC_UUID ""
#define DEVICE_NAME         "ARGUS_SCANNER"

BLEServer* pServer = NULL;
BLECharacteristic* pCharacteristic = NULL;
bool deviceConnected = false;

class MyServerCallbacks: public BLEServerCallbacks {
    void onConnect(BLEServer* pServer) { 
      deviceConnected = true; 
      Serial.println(">>> App Linked via BLE");
    }
    void onDisconnect(BLEServer* pServer) { 
      deviceConnected = false; 
      BLEDevice::startAdvertising();
      Serial.println(">>> App Disconnected");
    }
};

void setup() {
  Serial.begin(115200);
  
  // Initialize Wi-Fi in Station Mode for sniffing
  WiFi.mode(WIFI_STA);
  WiFi.disconnect();
  
  // Initialize BLE to talk to the Phone
  BLEDevice::init(DEVICE_NAME);
  pServer = BLEDevice::createServer();
  pServer->setCallbacks(new MyServerCallbacks());

  BLEService *pService = pServer->createService(SERVICE_UUID);
  pCharacteristic = pService->createCharacteristic(
                      CHARACTERISTIC_UUID,
                      BLECharacteristic::PROPERTY_READ | BLECharacteristic::PROPERTY_NOTIFY
                    );
  pCharacteristic->addDescriptor(new BLE2902());
  pService->start();

  BLEDevice::startAdvertising();
  Serial.println("Argus WiFi Sniffer Ready...");
}

void loop() {
  if (deviceConnected) {
    Serial.println("Scanning WiFi Spectrum...");
    
    // Scan WiFi (async = false, show_hidden = true, passive = true)
    int n = WiFi.scanNetworks(false, true, true);
    int strongestWiFiRSSI = -100;

    if (n > 0) {
      for (int i = 0; i < n; ++i) {
        if (WiFi.RSSI(i) > strongestWiFiRSSI) {
          strongestWiFiRSSI = WiFi.RSSI(i);
        }
      }
    }
    
    // Clean up WiFi scan results to save memory
    WiFi.scanDelete();

    // Send the strongest Wi-Fi signal found to the HUD
    String payload = String(strongestWiFiRSSI);
    pCharacteristic->setValue(payload.c_str());
    pCharacteristic->notify();
    
    Serial.print("Strongest WiFi Threat: ");
    Serial.println(payload);
    
    delay(1000); // Wait 1 second before next sweep
  }
}