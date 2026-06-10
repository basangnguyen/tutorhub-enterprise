
        function showToast(message, duration = 3000) {
            const container = document.getElementById('toast-container');
            const toast = document.createElement('div');
            toast.innerHTML = message;
            toast.style.background = 'rgba(0, 0, 0, 0.8)';
            toast.style.color = 'white';
            toast.style.padding = '10px 20px';
            toast.style.borderRadius = '5px';
            toast.style.boxShadow = '0 2px 10px rgba(0,0,0,0.3)';
            toast.style.fontSize = '14px';
            toast.style.transition = 'opacity 0.3s ease';
            toast.style.opacity = '0';
            toast.style.pointerEvents = 'auto';
            
            container.appendChild(toast);
            
            // Trigger animation
            setTimeout(() => toast.style.opacity = '1', 10);
            
            setTimeout(() => {
                toast.style.opacity = '0';
                setTimeout(() => toast.remove(), 300);
            }, duration);
        }

        // Tích hợp phím tắt Tldraw
    