use crate::error::LockdownError;
use chrono::Local;
use log::{LevelFilter, Metadata, Record};
use std::fs::{File, OpenOptions};
use std::io::{BufWriter, Write};
use std::sync::Mutex;

struct LockdownLogger {
    file: Mutex<BufWriter<File>>,
}

impl log::Log for LockdownLogger {
    fn enabled(&self, _metadata: &Metadata) -> bool {
        true
    }

    fn log(&self, record: &Record) {
        if self.enabled(record.metadata()) {
            let timestamp = Local::now().format("%Y-%m-%d %H:%M:%S%.3f");
            let thread_id = std::thread::current().id();
            let log_line = format!(
                "[{}] [{:?}] [{}] {}\n",
                timestamp,
                thread_id,
                record.level(),
                record.args()
            );

            eprint!("{}", log_line);

            if let Ok(mut file_guard) = self.file.lock() {
                let _ = file_guard.write_all(log_line.as_bytes());
                let _ = file_guard.flush();
            }
        }
    }

    fn flush(&self) {
        if let Ok(mut file_guard) = self.file.lock() {
            let _ = file_guard.flush();
        }
    }
}

pub fn init(session_id: &str) -> Result<(), LockdownError> {
    let mut temp_dir = std::env::temp_dir();
    temp_dir.push("tutorhub");

    if !temp_dir.exists() {
        std::fs::create_dir_all(&temp_dir)?;
    }

    let log_file_name = format!("lockdown_{}.log", session_id);
    let log_path = temp_dir.join(log_file_name);

    let file = OpenOptions::new()
        .create(true)
        .append(true)
        .open(&log_path)?;

    let logger = LockdownLogger {
        file: Mutex::new(BufWriter::new(file)),
    };

    let logger = Box::new(logger);

    // Ignore SetLoggerError if it was already set
    let _ = log::set_boxed_logger(logger);
    log::set_max_level(LevelFilter::Info);

    log::info!("Logger initialized at {:?}", log_path);
    Ok(())
}
