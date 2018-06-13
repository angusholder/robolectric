package org.robolectric.shadows;

import static android.os.Build.VERSION_CODES.LOLLIPOP;

import android.annotation.SuppressLint;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadow.api.Shadow;

/** Adds Robolectric support for BLE advertising. */
@Implements(value = BluetoothLeAdvertiser.class, minSdk = LOLLIPOP)
public class ShadowBluetoothLeAdvertiser {
  private static BluetoothLeAdvertiser bluetoothLeAdvertiser;

  static BluetoothLeAdvertiser getInstance() {
    if (bluetoothLeAdvertiser == null) {
      bluetoothLeAdvertiser = newInstance();
    }
    return bluetoothLeAdvertiser;
  }

  @SuppressLint("PrivateApi")
  @SuppressWarnings("unchecked")
  private static BluetoothLeAdvertiser newInstance() {
    try {
      Class<?> iBluetoothManagerClass =
          Shadow.class.getClassLoader().loadClass("android.bluetooth.IBluetoothManager");

      return Shadow.newInstance(
          BluetoothLeAdvertiser.class,
          new Class<?>[] {iBluetoothManagerClass},
          new Object[] {null});
    } catch (ClassNotFoundException e) {
      return null;
    }
  }

  @Implementation
  public void startAdvertising(
      AdvertiseSettings settings, AdvertiseData advertiseData, AdvertiseCallback callback) {
    callback.onStartSuccess(settings);
  }

  @Implementation
  public void startAdvertising(
      AdvertiseSettings settings,
      AdvertiseData advertiseData,
      AdvertiseData scanResponse,
      AdvertiseCallback callback) {
    callback.onStartSuccess(settings);
  }

  @Implementation
  public void stopAdvertising(AdvertiseCallback callback) {}
}
