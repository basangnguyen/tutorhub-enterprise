import * as THREE from 'three';
import { GLTFLoader } from 'three/addons/loaders/GLTFLoader.js';
const { ipcRenderer } = window.require('electron');

// ---- KHỞI TẠO THREE.JS ----
const canvas = document.getElementById('glcanvas');
const renderer = new THREE.WebGLRenderer({ canvas, alpha: true, antialias: true }); // alpha: true giúp nền trong suốt
renderer.setSize(window.innerWidth, window.innerHeight);
renderer.setPixelRatio(window.devicePixelRatio);

const scene = new THREE.Scene();
const camera = new THREE.PerspectiveCamera(45, window.innerWidth / window.innerHeight, 0.1, 100);
camera.position.set(0, 1.2, 3.5); // Điều chỉnh vị trí camera nhìn vào nhân vật

// Ánh sáng
const light = new THREE.DirectionalLight(0xffffff, 1.5);
light.position.set(1, 1, 1).normalize();
scene.add(light);
scene.add(new THREE.AmbientLight(0xcccccc, 0.8));

// ---- LOAD MODEL (GLTF/GLB) ----
const loader = new GLTFLoader();
let currentModel = null;
let mixer = null; // Animation mixer
let jawBone = null; // Xương hàm để nhép môi
let auraMeshes = []; // Lưu trữ các mesh sóng nước / nền

// Ẩn UI vì giao diện Chat được chuyển sang Java
const uiContainer = document.getElementById('ui-container');
if (uiContainer) uiContainer.style.display = 'none';
const chatContainer = document.getElementById('chat-container');
if (chatContainer) chatContainer.style.display = 'none';

let overrideVolume = 0;
ipcRenderer.on('lipsync', (event, vol) => {
  overrideVolume = vol;
});

let chatState = "CLOSED";
ipcRenderer.on('lavie-state', (event, state) => {
  chatState = state;
});


// Bạn cần tải file .glb từ Sketchfab và đặt vào thư mục models/
// Ở đây mình tạm trỏ vào file 'models/aqua.glb'. Nếu chưa có, nó sẽ báo lỗi ở console, đó là bình thường.
loader.load(
  'models/aqua.glb',
  (gltf) => {
    currentModel = gltf.scene;
    
    // Thu nhỏ nhân vật để hiển thị gọn gàng ở góc màn hình
    currentModel.scale.set(0.07, 0.07, 0.07);
    
    // Tính toán giới hạn màn hình (Frustum) để đặt Lavie sát mép phải
    const dist = camera.position.z;
    const visibleHeight = 2 * Math.tan((Math.PI / 180) * camera.fov / 2) * dist;
    const visibleWidth = visibleHeight * camera.aspect;
    const rightEdgeX = visibleWidth / 2;
    
    // Đặt Lavie sát góc dưới bên phải (cách mép phải một khoảng rất nhỏ để không bị lẹm)
    currentModel.position.set(rightEdgeX - 0.3, -0.1, 0); 
    
    // Đảm bảo model luôn nhìn về phía trước
    currentModel.rotation.set(0, 0, 0);

    // Ẩn mặt nước và mây (Môi trường)
    // Duyệt qua tất cả các mesh, xử lý các Mesh nền (aura/sóng nước)
    currentModel.traverse((child) => {
      if (child.isMesh && !child.isSkinnedMesh) {
        // Tắt nền sóng biển / bãi biển
        child.visible = false;
      }
      // Tìm xương hàm (Jaw) để nhép môi
      if (child.isBone) {
        let name = child.name.toLowerCase();
        if (name.includes('jaw') || name.includes('mouth')) {
          jawBone = child;
        }
      }
    });

    // Kích hoạt Animation (nếu có)
    if (gltf.animations && gltf.animations.length > 0) {
      mixer = new THREE.AnimationMixer(currentModel);
      // Chạy animation đầu tiên (thường là Idle)
      const action = mixer.clipAction(gltf.animations[0]);
      action.play();
    }

    scene.add(currentModel);
  },
  (xhr) => {
    console.log((xhr.loaded / xhr.total * 100) + '% loaded');
  },
  (error) => {
    console.warn('Vui lòng tải model từ Sketchfab về dưới dạng file .glb, bỏ vào thư mục models/aqua.glb để hiển thị nhân vật!', error);
  }
);

// ---- RENDER LOOP ----
const clock = new THREE.Clock();
function animate() {
  requestAnimationFrame(animate);
  const delta = clock.getDelta();
  
  // Tương tác chuột cơ bản: Quay nguyên cụm model nếu không có bone cụ thể
  if (currentModel && mouseX !== 0) {
    // Để làm model xoay đầu, cần tìm node xương cổ trong gltf (rất phụ thuộc vào model cụ thể)
    // currentModel.rotation.y = THREE.MathUtils.lerp(currentModel.rotation.y, mouseX * 0.5, 5 * delta);
  }

  // Cập nhật Animation
  if (mixer) {
    mixer.update(delta);
  }

  // Cập nhật Nhép môi (Lip-sync) dựa trên âm thanh từ UDP Java
  if (jawBone) {
    jawBone.rotation.x = overrideVolume * 0.5; // Hệ số 0.5 có thể điều chỉnh
  }

  // Cập nhật hiệu ứng sóng biển (aura)
  if (currentModel && auraMeshes && auraMeshes.length > 0) {
    // Xác định cường độ sóng biển
    let isHoveringLavie = (intersectsHover && intersectsHover.length > 0) || isDragging;
    let targetOpacity = 0.2; // Trạng thái nghỉ
    let targetScale = 1.0;
    
    if (chatState === "OPEN") {
      targetOpacity = 0.6;
      targetScale = 1.1;
    }
    if (isHoveringLavie) {
      targetOpacity = 0.8;
      targetScale = 1.15;
    }
    if (overrideVolume > 0) {
      targetOpacity = 0.9;
      targetScale = 1.2;
    }

    // Làm mượt hiệu ứng (Lerp)
    currentAuraOpacity = THREE.MathUtils.lerp(currentAuraOpacity || 0.2, targetOpacity, 2 * delta);
    currentAuraScale = THREE.MathUtils.lerp(currentAuraScale || 1.0, targetScale, 2 * delta);
    
    // Áp dụng cho các mesh sóng nước (mây/nền)
    auraMeshes.forEach(mesh => {
      if (mesh.material) mesh.material.opacity = currentAuraOpacity;
      // Tránh scale các xương, chỉ scale mesh nền. Thường các mesh nền có geometry scale riêng
      mesh.scale.set(currentAuraScale, currentAuraScale, currentAuraScale);
    });
  }

  renderer.render(scene, camera);
}
let currentAuraOpacity = 0.2;
let currentAuraScale = 1.0;
let intersectsHover = [];
animate();

// ---- TƯƠNG TÁC CHUỘT (DRAG MODEL & CHAT TOGGLE) ----
let mouseX = 0;
let mouseY = 0;
const raycaster = new THREE.Raycaster();
const mouse = new THREE.Vector2();
let isDragging = false;
let dragPlane = new THREE.Plane(new THREE.Vector3(0, 0, 1), 0);
let offset = new THREE.Vector3();
let mouseDownPos = { x: 0, y: 0 };

window.addEventListener('mousedown', (event) => {
  // Bỏ qua click vào UI chat
  if (event.target.closest('#chat-container')) return;

  mouseDownPos.x = event.clientX;
  mouseDownPos.y = event.clientY;

  mouse.x = (event.clientX / window.innerWidth) * 2 - 1;
  mouse.y = -(event.clientY / window.innerHeight) * 2 + 1;
  raycaster.setFromCamera(mouse, camera);

  if (currentModel) {
    const intersects = raycaster.intersectObject(currentModel, true).filter(hit => hit.object.visible && hit.object.isSkinnedMesh);
    if (intersects.length > 0) {
      isDragging = true;
      dragPlane.setFromNormalAndCoplanarPoint(camera.getWorldDirection(dragPlane.normal), intersects[0].point);
      raycaster.ray.intersectPlane(dragPlane, offset);
      offset.sub(currentModel.position);
    }
  }
});

function updateLaviePositionToJava() {
  if (!currentModel) return;
  // Lấy toạ độ trực tiếp để không bị trễ frame (stale matrixWorld)
  const vector = currentModel.position.clone();
  // Nâng y lên một chút để lấy điểm sát trên đầu Lavie (giảm từ 0.7 xuống 0.3)
  vector.y += 0.3; 
  vector.project(camera);

  const x = Math.round((vector.x * 0.5 + 0.5) * window.innerWidth);
  const y = Math.round(-(vector.y * 0.5 - 0.5) * window.innerHeight);

  ipcRenderer.send('lavie-moved', { x, y });
}

window.addEventListener('mousemove', (event) => {
  mouseX = (event.clientX / window.innerWidth) * 2 - 1;
  mouseY = -(event.clientY / window.innerHeight) * 2 + 1;
  
  mouse.x = mouseX;
  mouse.y = mouseY;
  raycaster.setFromCamera(mouse, camera);

  // Kéo nhân vật
  if (isDragging && currentModel) {
    const point = new THREE.Vector3();
    if (raycaster.ray.intersectPlane(dragPlane, point)) {
      currentModel.position.copy(point.sub(offset));
      updateLaviePositionToJava();
    }
  }

  // Cảm biến IPC: Bật/tắt xuyên thấu chuột
  const isHoveringUI = event.target.closest('#chat-container') !== null;
  const isChatVisible = document.getElementById('chat-container') && document.getElementById('chat-container').style.display !== 'none';
  intersectsHover = currentModel ? raycaster.intersectObject(currentModel, true).filter(hit => hit.object.visible && hit.object.isSkinnedMesh) : [];
  
  if (isDragging || intersectsHover.length > 0 || (isHoveringUI && isChatVisible)) {
    ipcRenderer.send('set-ignore-mouse-events', false);
  } else {
    ipcRenderer.send('set-ignore-mouse-events', true, { forward: true });
  }
});

window.addEventListener('mouseup', (event) => {
  if (isDragging) {
    const dx = Math.abs(event.clientX - mouseDownPos.x);
    const dy = Math.abs(event.clientY - mouseDownPos.y);
    // Nếu di chuyển chuột rất ít (chưa tới 5 pixel), thì coi như là Click
    if (dx < 5 && dy < 5) {
      updateLaviePositionToJava(); // Cập nhật vị trí trước khi hiện Chat
      ipcRenderer.send('lavie-clicked');
    }
  }
  isDragging = false;
});

// Resizing
window.addEventListener('resize', () => {
  camera.aspect = window.innerWidth / window.innerHeight;
  camera.updateProjectionMatrix();
  renderer.setSize(window.innerWidth, window.innerHeight);

  if (currentModel) {
    const dist = camera.position.z;
    const visibleHeight = 2 * Math.tan((Math.PI / 180) * camera.fov / 2) * dist;
    const visibleWidth = visibleHeight * camera.aspect;
    const rightEdgeX = visibleWidth / 2;
    currentModel.position.set(rightEdgeX - 0.3, -0.1, 0);
    updateLaviePositionToJava();
  }
});

// Xóa bỏ giao tiếp cũ vì đã chuyển sang Java
