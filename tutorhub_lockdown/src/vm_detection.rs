use crate::error::LockdownError;
use std::path::Path;
use windows::Win32::NetworkManagement::IpHelper::{
    GetAdaptersAddresses, GET_ADAPTERS_ADDRESSES_FLAGS, IP_ADAPTER_ADDRESSES_LH,
};

pub fn check() -> Result<(), LockdownError> {
    // Layer 1: CPUID
    if check_cpuid_hypervisor() {
        // Warning only, do not return Err
        #[cfg(feature = "debug_mode")]
        println!("WARNING: CPUID indicates running under a hypervisor.");
    }

    // Layer 2: MAC Prefixes
    if let Ok(macs) = get_mac_addresses() {
        for mac in macs {
            if is_vm_mac(&mac) {
                return Err(LockdownError::VmDetected(format!(
                    "VM MAC Address detected: {:02X}:{:02X}:{:02X}:{:02X}:{:02X}:{:02X}",
                    mac[0], mac[1], mac[2], mac[3], mac[4], mac[5]
                )));
            }
        }
    }

    // Layer 3: VM Drivers / Files
    if let Some(indicator) = check_vm_files() {
        return Err(LockdownError::VmDetected(format!(
            "VM Guest tools detected: {}",
            indicator
        )));
    }

    Ok(())
}

fn check_cpuid_hypervisor() -> bool {
    #[cfg(any(target_arch = "x86", target_arch = "x86_64"))]
    {
        #[cfg(target_arch = "x86")]
        use std::arch::x86::__cpuid;
        #[cfg(target_arch = "x86_64")]
        use std::arch::x86_64::__cpuid;

        let cpuid = __cpuid(1);
        (cpuid.ecx & (1 << 31)) != 0
    }
    #[cfg(not(any(target_arch = "x86", target_arch = "x86_64")))]
    {
        false
    }
}

fn get_mac_addresses() -> Result<Vec<[u8; 6]>, LockdownError> {
    let mut size = 0;
    // Call once to get size. Family = 0 (AF_UNSPEC)
    unsafe {
        let _ = GetAdaptersAddresses(0, GET_ADAPTERS_ADDRESSES_FLAGS(0), None, None, &mut size);
    }

    if size == 0 {
        return Ok(Vec::new());
    }

    let mut buf: Vec<u8> = vec![0; size as usize];
    let mut macs = Vec::new();

    let res = unsafe {
        GetAdaptersAddresses(
            0,
            GET_ADAPTERS_ADDRESSES_FLAGS(0),
            None,
            Some(buf.as_mut_ptr() as *mut IP_ADAPTER_ADDRESSES_LH),
            &mut size,
        )
    };

    if res == 0 {
        let mut curr = buf.as_ptr() as *const IP_ADAPTER_ADDRESSES_LH;
        while !curr.is_null() {
            let adapter = unsafe { &*curr };
            if adapter.PhysicalAddressLength == 6 {
                let mut mac = [0u8; 6];
                mac.copy_from_slice(&adapter.PhysicalAddress[..6]);
                // Ignore all 0s
                if mac != [0, 0, 0, 0, 0, 0] {
                    macs.push(mac);
                }
            }
            curr = adapter.Next;
        }
    }

    Ok(macs)
}

fn is_vm_mac(mac: &[u8; 6]) -> bool {
    let prefix = [mac[0], mac[1], mac[2]];
    matches!(
        prefix,
        // VMware
        [0x00, 0x05, 0x69] | [0x00, 0x0C, 0x29] | [0x00, 0x1C, 0x14] | [0x00, 0x50, 0x56] |
        // VirtualBox
        [0x08, 0x00, 0x27] |
        // Parallels
        [0x00, 0x1C, 0x42] |
        // QEMU/KVM
        [0x52, 0x54, 0x00] |
        // Xen
        [0x00, 0x16, 0x3E]
    )
}

fn check_vm_files() -> Option<&'static str> {
    let paths = [
        // VirtualBox
        (
            "C:\\Windows\\System32\\drivers\\VBoxMouse.sys",
            "VirtualBox",
        ),
        (
            "C:\\Windows\\System32\\drivers\\VBoxGuest.sys",
            "VirtualBox",
        ),
        ("C:\\Windows\\System32\\drivers\\VBoxSF.sys", "VirtualBox"),
        // VMware
        ("C:\\Windows\\System32\\drivers\\vmmouse.sys", "VMware"),
        ("C:\\Windows\\System32\\drivers\\vm3dmp.sys", "VMware"),
        ("C:\\Windows\\System32\\drivers\\vmci.sys", "VMware"),
        // QEMU
        ("C:\\Windows\\System32\\drivers\\qemu-ga.exe", "QEMU"),
    ];

    for (path, name) in paths.iter() {
        if Path::new(path).exists() {
            return Some(*name);
        }
    }

    None
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_is_vm_mac() {
        let vmware_mac = [0x00, 0x0C, 0x29, 0x11, 0x22, 0x33];
        assert!(is_vm_mac(&vmware_mac));

        let vbox_mac = [0x08, 0x00, 0x27, 0x44, 0x55, 0x66];
        assert!(is_vm_mac(&vbox_mac));

        let real_mac = [0x11, 0x22, 0x33, 0x44, 0x55, 0x66];
        assert!(!is_vm_mac(&real_mac));
    }

    #[test]
    fn test_check_vm_files_does_not_panic() {
        let _ = check_vm_files();
    }
}
