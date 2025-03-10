package com.phearme.btscanselector;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import androidx.databinding.BaseObservable;
import androidx.databinding.Bindable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static android.bluetooth.BluetoothDevice.EXTRA_RSSI;


public class BTScanSelectorViewModel extends BaseObservable {
    private List<BTScanResultItem> devices;
    private boolean scanning;
    private final ComparatorByRssi comparatorByRssi = new ComparatorByRssi();

    private IBTScanSelectorEvents callbacks;
    private IBTScanDataEvents dataEvents;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null) {
                switch (action) {
                    case BluetoothDevice.ACTION_FOUND:
                        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                        int rssi = intent.getShortExtra(EXTRA_RSSI, (short)100);
                        BTScanResultItem deviceItem = new BTScanResultItem(device, rssi);
                        if (devices != null && callbacks.onDeviceFound(deviceItem.getBluetoothDevice())) {
                            int devicePosition = findDeviceByAddress(deviceItem.getBluetoothDevice().getAddress());
                            if (devicePosition == -1) {
                                devices.add(deviceItem);
                            } else {
                                devices.get(devicePosition).setRssi(rssi);
                            }
                            Collections.sort(devices, comparatorByRssi);
                            notifyPropertyChanged(com.phearme.btscanselector.BR.devices);
                            dataEvents.onDataChange();
                        }
                        break;
                    case BluetoothAdapter.ACTION_DISCOVERY_STARTED:
                        setScanning(true);
                        dataEvents.onScanToggled(scanning);
                        break;
                    case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                        setScanning(false);
                        dataEvents.onScanToggled(scanning);
                        break;
                }
            }
        }
    };

    BTScanSelectorViewModel(IBTScanSelectorEvents callbacks, IBTScanDataEvents dataEvents,  Context context) {
        this.devices = new ArrayList<>();
        this.scanning = false;
        this.callbacks = callbacks;
        this.dataEvents = dataEvents;
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        context.registerReceiver(mReceiver, filter);
        if (BluetoothAdapter.getDefaultAdapter() != null) {
            BluetoothAdapter.getDefaultAdapter().startDiscovery();
        }
    }

    @Bindable
    public List<BTScanResultItem> getDevices() {
        return devices;
    }

    public void setDevices(List<BTScanResultItem> devices) {
        this.devices = devices;
        notifyPropertyChanged(com.phearme.btscanselector.BR.devices);
    }

    @Bindable
    public boolean isScanning() {
        return scanning;
    }

    public void setScanning(boolean scanning) {
        this.scanning = scanning;
        notifyPropertyChanged(com.phearme.btscanselector.BR.scanning);
    }

    public void onItemResultClick(BTScanResultItem device) {
        if (this.callbacks != null) {
            this.callbacks.onDeviceSelected(device.getBluetoothDevice());
        }
    }

    void terminate(Context context) {
        try {
            if (BluetoothAdapter.getDefaultAdapter() != null) {
                BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
            }
            if (mReceiver != null) {
                context.unregisterReceiver(mReceiver);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class ComparatorByRssi implements Comparator<BTScanResultItem> {
        @Override
        public int compare(BTScanResultItem btScanResultItem, BTScanResultItem t1) {
            return t1.getRssi() - btScanResultItem.getRssi();
        }
    }

    void refresh() {
        if (!isScanning()) {
            devices = new ArrayList<>();
            if (BluetoothAdapter.getDefaultAdapter() != null) {
                BluetoothAdapter.getDefaultAdapter().startDiscovery();
            }
        }
    }

    private int findDeviceByAddress(String address) {
        for (int i = 0; i < devices.size(); i++) {
            if (devices.get(i).getBluetoothDevice().getAddress().equals(address)) {
                return i;
            }
        }
        return -1;
    }


}
