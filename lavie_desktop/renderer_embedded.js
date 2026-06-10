import * as THREE from './node_modules/three/build/three.module.js';
import { GLTFLoader } from './node_modules/three/examples/jsm/loaders/GLTFLoader.js';

// ---- KHỞI TẠO THREE.JS ----
const canvas = document.getElementById('glcanvas');
const renderer = new THREE.WebGLRenderer({ canvas, alpha: true, antialias: true }); // alpha: true giúp nền trong suốt
renderer.setSize(window.innerWidth, window.innerHeight);
renderer.setPixelRatio(window.devicePixelRatio);
// Đảm bảo clear color là transparent
renderer.setClearColor(0x000000, 0);

const scene = new THREE.Scene();
const camera = new THREE.PerspectiveCamera(45, window.innerWidth / window.innerHeight, 0.1, 100);
// Với WebView khung nhỏ nhắn, căn giữa model
camera.position.set(0, 1.0, 3.0); 

// Ánh sáng
const light = new THREE.DirectionalLight(0xffffff, 1.5);
light.position.set(1, 1, 1).normalize();
scene.add(light);
scene.add(new THREE.AmbientLight(0xcccccc, 0.8));

// ---- LOAD MODEL (GLTF/GLB) ----
const loader = new GLTFLoader();
let currentModel = null;
let mixer = null; 
let jawBone = null; 

loader.load(
  'models/aqua.glb',
  (gltf) => {
    currentModel = gltf.scene;
    
    // Scale lớn hơn một chút vì đã thu gọn khung chat
    currentModel.scale.set(0.12, 0.12, 0.12);
    
    // Đặt ở giữa khung hình
    currentModel.position.set(0, 0, 0); 
    currentModel.rotation.set(0, 0, 0);

    currentModel.traverse((child) => {
      if (child.isMesh && !child.isSkinnedMesh) {
        child.visible = false;
      }
      if (child.isBone) {
        let name = child.name.toLowerCase();
        if (name.includes('jaw') || name.includes('mouth')) {
          jawBone = child;
        }
      }
    });

    if (gltf.animations && gltf.animations.length > 0) {
      mixer = new THREE.AnimationMixer(currentModel);
      const action = mixer.clipAction(gltf.animations[0]);
      action.play();
    }

    scene.add(currentModel);
  },
  undefined,
  (error) => { console.error('Lỗi load model:', error); }
);

// ---- RENDER LOOP ----
const clock = new THREE.Clock();
let currentLipSyncVolume = 0;

function animate() {
  requestAnimationFrame(animate);
  const delta = clock.getDelta();
  
  if (mixer) mixer.update(delta);

  // Nhép môi dựa trên volume được set từ Java
  if (jawBone) {
    // currentLipSyncVolume từ 0 đến 1
    jawBone.rotation.x = currentLipSyncVolume * 0.5;
  }

  renderer.render(scene, camera);
}
animate();

// ---- GIAO TIẾP VỚI JAVA ----
window.setLipSyncVolume = function(volume) {
  currentLipSyncVolume = volume;
};

// Resizing
window.addEventListener('resize', () => {
  camera.aspect = window.innerWidth / window.innerHeight;
  camera.updateProjectionMatrix();
  renderer.setSize(window.innerWidth, window.innerHeight);
});
