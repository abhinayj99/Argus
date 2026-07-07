/*
 * ARGUS - Hardware Simulation (Advanced Mode)
 * * PURPOSE: Simulates Sensor Noise to validate Z-Score AI
 * * MAPPING:
 * - Potentiometer (PIN 34) --> RF Base Signal
 * - Potentiometer (PIN 35) --> Thermal Base Temp
 * - LED (PIN 2)            --> Warning Indicator
 */

#include <Arduino.h>

// --- PIN DEFINITIONS ---
const int PIN_RF_SENSOR = 34; 
const int PIN_THERMAL_SENSOR = 35; 
const int PIN_IR_LED = 2; 

// --- VARIABLES ---
int rf_raw = 0;
int thermal_raw = 0;
int rf_output = 0; 
float temp_output = 0.0;

// Simulation Variables
float noise_factor = 0.0;

void setup() {
  Serial.begin(115200);
  pinMode(PIN_RF_SENSOR, INPUT);
  pinMode(PIN_THERMAL_SENSOR, INPUT);
  pinMode(PIN_IR_LED, OUTPUT);
  
  // Random seed generation for realistic noise
  randomSeed(analogRead(0));
  
  Serial.println("ARGUS SYSTEM INITIALIZED... SENSOR SIMULATION ACTIVE");
}

void loop() {
  // -------------------------------------------------
  // 1. READ SENSORS (THE BASELINE)
  // -------------------------------------------------
  rf_raw = analogRead(PIN_RF_SENSOR);
  thermal_raw = analogRead(PIN_THERMAL_SENSOR);

  // -------------------------------------------------
  // 2. INJECT REALISM (THE UPGRADE)
  // -------------------------------------------------
  
  // A. Add "Jitter" to RF Signal (Real WiFi signals bounce around)
  // Maps 0-4095 to 0-100%, then adds random noise (-2% to +2%)
  int rf_noise = random(-2, 3); 
  rf_output = map(rf_raw, 0, 4095, 0, 100) + rf_noise;
  rf_output = constrain(rf_output, 0, 100); // Keep it within 0-100

  // B. Add "Micro-Fluctuations" 
  // Real sensors fluctuate by +/- 0.2 degrees naturally
  float temp_base = map(thermal_raw, 0, 4095, 200, 800) / 10.0; 
  float temp_jitter = random(-20, 20) / 100.0; // +/- 0.20 deg C
  temp_output = temp_base + temp_jitter;

  // -------------------------------------------------
  // 3. LED LOGIC (Fail-Safe)
  // -------------------------------------------------
  // Simple hardware trigger (The App handles the complex Z-Score trigger)
  bool warning_active = (rf_output > 85) || (temp_output > 40.0);

  if (warning_active) {
      digitalWrite(PIN_IR_LED, HIGH);
  } else {
      digitalWrite(PIN_IR_LED, LOW); 
  }

  // -------------------------------------------------
  // 4. OUTPUT DATA (JSON)
  // -------------------------------------------------
  Serial.print("{");
  Serial.print("\"rf_level\":");
  Serial.print(rf_output);
  Serial.print(", ");
  Serial.print("\"thermal_c\":");
  Serial.print(temp_output, 2); // 2 decimal places for precision
  Serial.println("}");

  delay(100); // Faster refresh rate (10Hz) to test App speed
}