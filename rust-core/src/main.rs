mod capability_probe;
mod desktop_isolation;
mod ipc_server;

use std::env;

fn main() {
    let args: Vec<String> = env::args().collect();

    // Safe default run: do nothing dangerous.
    if args.len() <= 1 || args[1] == "--help" {
        println!("Rust Lockdown Core Safe PoC");
        println!("Usage:");
        println!("  --probe                Run capability probe (SAFE)");
        println!("  --desktop-demo-safe    Run CreateDesktopW/SwitchDesktop demo (VM-ONLY SAFE)");
        println!("  --ipc-diagnostic       Run IPC diagnostic (SAFE)");
        return;
    }

    let command = &args[1];

    match command.as_str() {
        "--probe" => {
            let result = capability_probe::run_probe();
            println!("{}", ipc_server::format_json(&result));
        }
        "--desktop-demo-safe" => {
            let result = desktop_isolation::run_demo();
            println!("{}", ipc_server::format_json(&result));
        }
        "--ipc-diagnostic" => {
            println!("{{\"diagnostic\":\"ok\"}}");
        }
        _ => {
            println!("Unknown command: {}", command);
            println!("Run with --help for usage.");
        }
    }
}
