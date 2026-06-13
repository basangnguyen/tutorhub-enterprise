package com.mycompany.tutorhub_enterprise.client.exam.ui;

import com.sun.jna.Native;
import com.sun.jna.Structure;
import com.sun.jna.win32.StdCallLibrary;

import java.util.Arrays;
import java.util.List;

public class TSEBatteryStatusProvider {

    public interface MyKernel32 extends StdCallLibrary {
        MyKernel32 INSTANCE = Native.load("kernel32", MyKernel32.class);

        class SYSTEM_POWER_STATUS extends Structure {
            public byte ACLineStatus;
            public byte BatteryFlag;
            public byte BatteryLifePercent;
            public byte SystemStatusFlag;
            public int BatteryLifeTime;
            public int BatteryFullLifeTime;

            @Override
            protected List<String> getFieldOrder() {
                return Arrays.asList("ACLineStatus", "BatteryFlag", "BatteryLifePercent",
                        "SystemStatusFlag", "BatteryLifeTime", "BatteryFullLifeTime");
            }
        }

        boolean GetSystemPowerStatus(SYSTEM_POWER_STATUS result);
    }

    public static class BatteryStatus {
        public boolean hasBattery;
        public boolean isCharging;
        public int percent;
        public String tooltip;
    }

    public static BatteryStatus getStatus() {
        BatteryStatus status = new BatteryStatus();
        try {
            MyKernel32.SYSTEM_POWER_STATUS sysPowerStatus = new MyKernel32.SYSTEM_POWER_STATUS();
            if (MyKernel32.INSTANCE.GetSystemPowerStatus(sysPowerStatus)) {
                // ACLineStatus: 0 = Offline, 1 = Online, 255 = Unknown
                int acStatus = sysPowerStatus.ACLineStatus & 0xFF;
                status.isCharging = (acStatus == 1);
                
                // BatteryFlag: 128 = No system battery, 255 = Unknown
                int flag = sysPowerStatus.BatteryFlag & 0xFF;
                
                if (flag == 128 || flag == 255) {
                    status.hasBattery = false;
                    status.percent = 100;
                    status.tooltip = "Đang cắm nguồn (Không có pin)";
                } else {
                    status.hasBattery = true;
                    status.percent = sysPowerStatus.BatteryLifePercent & 0xFF;
                    if (status.percent == 255) {
                        status.percent = -1; // Unknown
                    }
                    
                    if (status.percent != -1) {
                        if (status.isCharging) {
                            status.tooltip = "Đang sạc (" + status.percent + "%)";
                        } else {
                            status.tooltip = "Pin (" + status.percent + "%)";
                        }
                    } else {
                        status.tooltip = status.isCharging ? "Đang sạc (N/A)" : "Pin (N/A)";
                    }
                }
            } else {
                status.hasBattery = false;
                status.tooltip = "Không đọc được thông tin Nguồn";
            }
        } catch (Throwable t) {
            status.hasBattery = false;
            status.tooltip = "Trạng thái Pin (N/A)";
            System.err.println("[TSE_BATTERY] Failed to get system power status: " + t.getMessage());
        }
        return status;
    }
}
