        async function handleDocumentUpload(event) {
            const file = event.target.files[0];
            if (!file) return;
            
            showToast("Đang xử lý tài liệu...", 3000);
            
            if (file.type === "application/pdf") {
                try {
                    const arrayBuffer = await file.arrayBuffer();
                    const pdf = await pdfjsLib.getDocument(arrayBuffer).promise;
                    showToast(`Tìm thấy ${pdf.numPages} trang PDF. Đang tải lên...`, 3000);
                    
                    let startY = window.tldrawAPI ? window.tldrawAPI.getAppState().scrollY : 0;
                    
                    for (let pageNum = 1; pageNum <= pdf.numPages; pageNum++) {
                        const page = await pdf.getPage(pageNum);
                        const viewport = page.getViewport({ scale: 2.0 });
                        
                        const canvas = document.createElement("canvas");
                        const ctx = canvas.getContext("2d");
                        canvas.height = viewport.height;
                        canvas.width = viewport.width;
                        
                        await page.render({ canvasContext: ctx, viewport: viewport }).promise;
                        
                        canvas.toBlob(async (blob) => {
                            await uploadAndInsertImage(blob, "pdf-page-" + pageNum + ".png", startY + (pageNum - 1) * (viewport.height + 50));
                        }, "image/png");
                    }
                } catch (e) {
                    console.error("Lỗi xử lý PDF:", e);
                    showToast("Lỗi xử lý PDF: " + e.message);
                }
            } else if (file.type.startsWith("image/")) {
                await uploadAndInsertImage(file, file.name, window.tldrawAPI ? window.tldrawAPI.getAppState().scrollY : 0);
            }
            
            event.target.value = '';
        }
        
        async function uploadAndInsertImage(blobOrFile, fileName, yPos) {
            const formData = new FormData();
            formData.append('file', blobOrFile, fileName);
            
            try {
                const res = await fetch("https://hocbatrolai293-tutorhub-vscode.hf.space/upload-document", {
                    method: 'POST',
                    body: formData
                });
                const data = await res.json();
                
                if (data.success) {
                    const url = data.url;
                    insertImageToExcalidraw(url, yPos);
                    
                    if (window.currentRoom) {
                        const strData = JSON.stringify({ type: 'document_sync', url: url, yPos: yPos });
                        const encoder = new TextEncoder();
                        await window.currentRoom.localParticipant.publishData(encoder.encode(strData), LivekitClient.DataPacket_Kind.RELIABLE);
                    }
                } else {
                    showToast("Lỗi tải tài liệu: " + data.error);
                }
            } catch (e) {
                console.error("Lỗi upload tài liệu:", e);
                showToast("Lỗi kết nối Server: " + e.message);
            }
        }
        
        async function insertImageToExcalidraw(url, yPos = 0) {
            if (!window.tldrawAPI) return;
            
            try {
                const proxyUrl = "https://hocbatrolai293-tutorhub-vscode.hf.space/proxy-image?url=" + encodeURIComponent(url);
                const response = await fetch(proxyUrl);
                const blob = await response.blob();
                
                const reader = new FileReader();
                reader.onloadend = () => {
                    const dataURL = reader.result;
                    const fileId = "file-" + Date.now() + Math.random().toString(36).substring(2, 9);
                    
                    const img = new Image();
                    img.onload = () => {
                        window.tldrawAPI.addFiles([{
                            id: fileId,
                            dataURL: dataURL,
                            mimeType: blob.type,
                            created: Date.now()
                        }]);
                        
                        const elements = window.tldrawAPI.getSceneElements();
                        const newElement = {
                            id: "img-" + Date.now() + Math.random().toString(36).substring(2, 9),
                            type: "image",
                            fileId: fileId,
                            x: -window.tldrawAPI.getAppState().scrollX + 50,
                            y: -window.tldrawAPI.getAppState().scrollY + yPos,
                            width: img.width / 2,
                            height: img.height / 2,
                            angle: 0,
                            strokeColor: "transparent",
                            backgroundColor: "transparent",
                            fillStyle: "hachure",
                            strokeWidth: 1,
                            strokeStyle: "solid",
                            roughness: 1,
                            opacity: 100,
                            groupIds: [],
                            roundness: null,
                            isDeleted: false,
                            boundElements: null,
                            updated: Date.now(),
                            link: null,
                            locked: false
                        };
                        
                        window.tldrawAPI.updateScene({ elements: [...elements, newElement] });
                        
                        // Kích hoạt đồng bộ nét vẽ sau khi chèn
                        if (window.performDrawSync) {
                            setTimeout(() => window.performDrawSync(window.tldrawAPI.getSceneElements()), 100);
                        }
                    };
                    img.src = dataURL;
                };
                reader.readAsDataURL(blob);
            } catch (e) {
                console.error("Lỗi chèn ảnh vào bảng:", e);
            }
        }

        // ==========================================
        // TÍNH NĂNG VS CODE THẬT (HUGGING FACE CODE-SERVER)
        // ==========================================
    // ==========================================
    // TÍNH NĂNG CO-WATCH & EXPORT PDF
    // ==========================================

    async function exportToPDF() {
        if (!window.tldrawAPI) return;
        try {
            const elements = window.tldrawAPI.getSceneElements();
            const appState = window.tldrawAPI.getAppState();
            const files = window.tldrawAPI.getFiles();
            
            showToast("Đang tạo file PDF, vui lòng chờ...", 2000);
            
            const blob = await window.ExcalidrawLib.exportToBlob({
                elements,
                appState: { ...appState, exportWithDarkMode: false, viewBackgroundColor: "#ffffff" },
                files,
                mimeType: "image/png"
            });
            
            const imgUrl = URL.createObjectURL(blob);
            const img = new Image();
            img.onload = () => {
                const { jsPDF } = window.jspdf;
                const orientation = img.width > img.height ? "l" : "p";
                const pdf = new jsPDF({ orientation: orientation, unit: "px", format: [img.width, img.height] });
                pdf.addImage(img, 'PNG', 0, 0, img.width, img.height);
                pdf.save("TutorHub_Board.pdf");
                showToast("Đã lưu PDF thành công!", 3000);
            };
            img.src = imgUrl;
        } catch (e) {
            console.error("Lỗi xuất PDF", e);
            alert("Lỗi khi tạo PDF!");
        }
    }

    function insertMathFormula() {
        const mathField = document.getElementById('math-field-input');
        if (!mathField.value.trim()) return;
        
        if (window.addMathNode) {
            window.addMathNode(mathField.value);
        }
        document.getElementById('math-modal').style.display = 'none';
        mathField.value = ''; // Reset
    }

    function insertMermaidDiagram() {
        const mermaidInput = document.getElementById('mermaid-input');
        if (!mermaidInput.value.trim()) return;
        
        if (window.addMermaidNode) {
            window.addMermaidNode(mermaidInput.value);
        }
        document.getElementById('mermaid-modal').style.display = 'none';
    }

    // ========== XỬ LÝ ĐỒNG BỘ CÙNG XEM (CO-WATCH) ==========