use crate::error::LockdownError;
use std::sync::{mpsc, Arc, Mutex};
use std::thread;
use std::time::{Duration, Instant};

pub struct Watchdog {
    last_ping: Arc<Mutex<Instant>>,
    timeout: Duration,
    shutdown_tx: Option<mpsc::Sender<()>>,
    thread_handle: Option<thread::JoinHandle<()>>,
}

impl Watchdog {
    pub fn new(timeout_secs: u64) -> Self {
        Watchdog {
            last_ping: Arc::new(Mutex::new(Instant::now())),
            timeout: Duration::from_secs(timeout_secs),
            shutdown_tx: None,
            thread_handle: None,
        }
    }

    pub fn record_ping(&self) -> Result<(), LockdownError> {
        let mut last_ping = self
            .last_ping
            .lock()
            .map_err(|_| LockdownError::UnexpectedCommand)?;
        *last_ping = Instant::now();
        Ok(())
    }

    pub fn start<F>(&mut self, mut on_timeout: F) -> Result<(), LockdownError>
    where
        F: FnMut() + Send + 'static,
    {
        if self.thread_handle.is_some() {
            return Err(LockdownError::UnexpectedCommand);
        }

        let (tx, rx) = mpsc::channel();
        self.shutdown_tx = Some(tx);

        let last_ping = Arc::clone(&self.last_ping);
        let timeout = self.timeout;

        self.thread_handle = Some(thread::spawn(move || {
            loop {
                // Check if shutdown signal received immediately
                if let Ok(_) = rx.try_recv() {
                    break;
                }

                let elapsed = {
                    match last_ping.lock() {
                        Ok(lp) => lp.elapsed(),
                        Err(_) => break, // poisoned mutex, main thread probably died
                    }
                };

                if elapsed > timeout {
                    on_timeout();
                    break; // Stop checking after timeout is triggered
                }

                // Sleep up to 1 second but be responsive to shutdown signal
                match rx.recv_timeout(Duration::from_secs(1)) {
                    Ok(_) => break,                                     // Shutdown signal received
                    Err(mpsc::RecvTimeoutError::Timeout) => continue,   // 1s passed, loop again
                    Err(mpsc::RecvTimeoutError::Disconnected) => break, // Channel closed
                }
            }
        }));

        Ok(())
    }

    pub fn stop(&mut self) -> Result<(), LockdownError> {
        if let Some(tx) = self.shutdown_tx.take() {
            let _ = tx.send(());
        }

        if let Some(handle) = self.thread_handle.take() {
            let _ = handle.join();
        }

        Ok(())
    }
}

impl Drop for Watchdog {
    fn drop(&mut self) {
        let _ = self.stop();
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::sync::atomic::{AtomicBool, Ordering};

    #[test]
    fn test_watchdog_ping() {
        let mut wd = Watchdog::new(2);
        let timeout_called = Arc::new(AtomicBool::new(false));
        let tc_clone = Arc::clone(&timeout_called);

        wd.start(move || {
            tc_clone.store(true, Ordering::SeqCst);
        })
        .unwrap();

        // ping after 1 sec
        thread::sleep(Duration::from_millis(1000));
        wd.record_ping().unwrap();

        // ping after another 1 sec
        thread::sleep(Duration::from_millis(1000));
        wd.record_ping().unwrap();

        // total 2 seconds passed, but we pinged, so should not timeout
        assert!(!timeout_called.load(Ordering::SeqCst));

        wd.stop().unwrap();
    }

    #[test]
    fn test_watchdog_timeout() {
        let mut wd = Watchdog::new(1);
        let timeout_called = Arc::new(AtomicBool::new(false));
        let tc_clone = Arc::clone(&timeout_called);

        wd.start(move || {
            tc_clone.store(true, Ordering::SeqCst);
        })
        .unwrap();

        // wait for 2.1 seconds (timeout is 1 sec, plus some slack for thread wakeup)
        thread::sleep(Duration::from_millis(2100));

        // should have been called
        assert!(timeout_called.load(Ordering::SeqCst));

        wd.stop().unwrap();
    }
}
